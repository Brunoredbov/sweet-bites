package br.com.sweetbites.service;

import br.com.sweetbites.dto.CotacaoFrete;
import br.com.sweetbites.dto.EnderecoCep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Cálculo do frete e do tempo estimado de entrega (US-08 CA#2 / US-10 RN#1).
 * A "distância" é aproximada por zona: mesma cidade da loja ou demais cidades do mesmo
 * estado. Fora do estado da loja não há entrega. Os valores ficam no application.properties.
 */
@Service
public class FreteService {

    private final CepService cepService;
    private final String cidadeLoja;
    private final String ufLoja;
    private final BigDecimal freteCidade;
    private final int tempoCidadeMin;
    private final BigDecimal freteEstado;
    private final int tempoEstadoMin;

    public FreteService(CepService cepService,
                        @Value("${loja.cidade}") String cidadeLoja,
                        @Value("${loja.uf}") String ufLoja,
                        @Value("${loja.frete.cidade.valor}") BigDecimal freteCidade,
                        @Value("${loja.frete.cidade.tempo-min}") int tempoCidadeMin,
                        @Value("${loja.frete.estado.valor}") BigDecimal freteEstado,
                        @Value("${loja.frete.estado.tempo-min}") int tempoEstadoMin) {
        this.cepService = cepService;
        this.cidadeLoja = cidadeLoja;
        this.ufLoja = ufLoja;
        this.freteCidade = freteCidade;
        this.tempoCidadeMin = tempoCidadeMin;
        this.freteEstado = freteEstado;
        this.tempoEstadoMin = tempoEstadoMin;
    }

    public static String somenteDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    public CotacaoFrete calcular(String cepInformado) {
        String cep = somenteDigitos(cepInformado);
        if (cep.length() != 8) {
            throw new RegraNegocioException("CEP não encontrado. Verifique o endereço.");
        }
        EnderecoCep endereco = cepService.consultar(cep)
                .orElseThrow(() -> new RegraNegocioException("CEP não encontrado. Verifique o endereço."));
        if (!endereco.uf().equalsIgnoreCase(ufLoja)) {
            throw new RegraNegocioException("Ainda não entregamos em " + endereco.cidade() + "/" + endereco.uf()
                    + ". Entregamos apenas no estado de " + ufLoja + ".");
        }
        if (normalizar(endereco.cidade()).equals(normalizar(cidadeLoja))) {
            return new CotacaoFrete(endereco, freteCidade, tempoCidadeMin);
        }
        return new CotacaoFrete(endereco, freteEstado, tempoEstadoMin);
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}
