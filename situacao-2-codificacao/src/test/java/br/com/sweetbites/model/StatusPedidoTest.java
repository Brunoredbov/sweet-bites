package br.com.sweetbites.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatusPedidoTest {

    @Test
    void fluxoNormalDePreparacaoEEntrega() {
        assertThat(StatusPedido.EM_PREPARACAO.proximo()).isEqualTo(StatusPedido.SAIU_PARA_ENTREGA);
        assertThat(StatusPedido.SAIU_PARA_ENTREGA.proximo()).isEqualTo(StatusPedido.ENTREGUE);
        assertThat(StatusPedido.ENTREGUE.proximo()).as("entregue é estado final").isNull();
        assertThat(StatusPedido.AGUARDANDO_PAGAMENTO.proximo())
                .as("só o pagamento do PIX tira o pedido de 'aguardando pagamento'").isNull();
    }

    @Test
    void pedidoEntregueOuCanceladoNaoPodeSerCancelado() {
        for (StatusPedido s : StatusPedido.values()) {
            boolean esperado = s != StatusPedido.ENTREGUE && s != StatusPedido.CANCELADO;
            assertThat(s.permiteCancelamento()).as("permiteCancelamento de %s", s).isEqualTo(esperado);
        }
    }

    @Test
    void ordemDasConstantesEhALinhaDoTempoExibidaAoCliente() {
        assertThat(StatusPedido.values()).startsWith(StatusPedido.AGUARDANDO_PAGAMENTO, StatusPedido.EM_PREPARACAO,
                StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.ENTREGUE);
    }
}
