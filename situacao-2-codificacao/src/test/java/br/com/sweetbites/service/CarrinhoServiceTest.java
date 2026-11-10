package br.com.sweetbites.service;

import br.com.sweetbites.dto.ItemCarrinho;
import br.com.sweetbites.dto.ResumoCarrinho;
import br.com.sweetbites.model.Categoria;
import br.com.sweetbites.model.Cupcake;
import br.com.sweetbites.repository.CupcakeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CarrinhoServiceTest {

    static Cupcake cupcake(long id, String preco, int estoque) {
        Cupcake c = new Cupcake("Cupcake " + id, "d", "i", new BigDecimal(preco), estoque, new Categoria("Cat", null));
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    void interpretaTextoDoNavegadorIgnorandoEntradasInvalidas() {
        List<ItemCarrinho> itens = CarrinhoService.interpretar("1:2, 3:1,abc,4:-1,5:x,1:3,:2");
        assertThat(itens).containsExactly(new ItemCarrinho(1L, 5), new ItemCarrinho(3L, 1));
        assertThat(CarrinhoService.interpretar(null)).isEmpty();
        assertThat(CarrinhoService.interpretar("   ")).isEmpty();
    }

    @Test
    void recalculaPrecosNoServidorELimitaA20PorItem() {
        CupcakeRepository repo = mock(CupcakeRepository.class);
        when(repo.findAtivosByIdIn(anyCollection())).thenReturn(List.of(cupcake(1, "10.00", 50)));
        ResumoCarrinho r = new CarrinhoService(repo).resumir(List.of(new ItemCarrinho(1L, 30)));
        assertThat(r.itens().get(0).quantidade()).as("RN US-07: no máximo 20 por item").isEqualTo(20);
        assertThat(r.subtotal()).isEqualByComparingTo("200.00");
        assertThat(r.avisos()).anyMatch(a -> a.contains("máximo de 20"));
    }

    @Test
    void sinalizaPedidoAbaixoDoMinimoEItemSemEstoque() {
        CupcakeRepository repo = mock(CupcakeRepository.class);
        when(repo.findAtivosByIdIn(anyCollection())).thenReturn(List.of(cupcake(1, "7.00", 1), cupcake(2, "5.00", 0)));
        ResumoCarrinho r = new CarrinhoService(repo).resumir(List.of(new ItemCarrinho(1L, 1), new ItemCarrinho(2L, 1)));
        assertThat(r.atingiuValorMinimo()).as("R$ 12,00 < mínimo de R$ 15,00").isFalse();
        assertThat(r.itens().get(1).disponivel()).isFalse();
    }

    @Test
    void removeItemQueSaiuDoCardapio() {
        CupcakeRepository repo = mock(CupcakeRepository.class);
        when(repo.findAtivosByIdIn(anyCollection())).thenReturn(List.of());
        ResumoCarrinho r = new CarrinhoService(repo).resumir(List.of(new ItemCarrinho(99L, 1)));
        assertThat(r.itens()).isEmpty();
        assertThat(r.avisos()).hasSize(1);
    }
}
