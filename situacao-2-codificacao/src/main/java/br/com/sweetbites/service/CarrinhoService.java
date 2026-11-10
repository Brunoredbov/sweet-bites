package br.com.sweetbites.service;

import br.com.sweetbites.dto.ItemCarrinho;
import br.com.sweetbites.dto.LinhaCarrinho;
import br.com.sweetbites.dto.ResumoCarrinho;
import br.com.sweetbites.model.Cupcake;
import br.com.sweetbites.model.Pedido;
import br.com.sweetbites.repository.CupcakeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * O carrinho fica no navegador (localStorage — RN#2 da US-07: "salvo mesmo se o app for
 * fechado"). O servidor nunca confia nos preços do navegador: este service recalcula
 * tudo a partir do banco.
 */
@Service
public class CarrinhoService {

    private static final int MAX_ITENS_DISTINTOS = 50;

    private final CupcakeRepository cupcakeRepository;

    public CarrinhoService(CupcakeRepository cupcakeRepository) {
        this.cupcakeRepository = cupcakeRepository;
    }

    /** Converte "3:2,5:1" em itens. Ignora entradas malformadas; soma ids repetidos. */
    public static List<ItemCarrinho> interpretar(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        for (String parte : texto.split(",")) {
            String[] idQtd = parte.trim().split(":");
            if (idQtd.length != 2) continue;
            try {
                long id = Long.parseLong(idQtd[0].trim());
                int qtd = Integer.parseInt(idQtd[1].trim());
                if (id > 0 && qtd > 0) {
                    quantidades.merge(id, qtd, Integer::sum);
                }
            } catch (NumberFormatException ignorado) {
                // entrada malformada vinda do navegador: descartada
            }
            if (quantidades.size() > MAX_ITENS_DISTINTOS) break;
        }
        return quantidades.entrySet().stream().map(e -> new ItemCarrinho(e.getKey(), e.getValue())).toList();
    }

    @Transactional(readOnly = true)
    public ResumoCarrinho resumir(List<ItemCarrinho> itens) {
        Map<Long, Cupcake> cupcakes = cupcakeRepository
                .findAtivosByIdIn(itens.stream().map(ItemCarrinho::cupcakeId).toList())
                .stream().collect(Collectors.toMap(Cupcake::getId, Function.identity()));
        List<LinhaCarrinho> linhas = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            Cupcake c = cupcakes.get(item.cupcakeId());
            if (c == null) {
                avisos.add("Um item do seu carrinho não está mais no cardápio e foi removido.");
                continue;
            }
            int qtd = Math.min(item.quantidade(), Pedido.MAX_QUANTIDADE_POR_ITEM);
            if (qtd < item.quantidade()) {
                avisos.add(c.getNome() + ": máximo de " + Pedido.MAX_QUANTIDADE_POR_ITEM + " unidades por item.");
            }
            BigDecimal sub = c.getPreco().multiply(BigDecimal.valueOf(qtd));
            boolean disponivel = c.isDisponivel() && qtd <= c.getEstoque();
            if (!disponivel) {
                avisos.add(c.getEstoque() == 0
                        ? c.getNome() + " está indisponível no momento."
                        : c.getNome() + ": só temos " + c.getEstoque() + " unidade(s) em estoque.");
            }
            linhas.add(new LinhaCarrinho(c.getId(), c.getNome(), c.getPreco(), qtd, sub, c.getEstoque(), disponivel, c.getImagemId()));
            subtotal = subtotal.add(sub);
        }
        return new ResumoCarrinho(linhas, subtotal, Pedido.VALOR_MINIMO,
                subtotal.compareTo(Pedido.VALOR_MINIMO) >= 0, avisos);
    }
}
