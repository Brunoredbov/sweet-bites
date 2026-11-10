package br.com.sweetbites.service;

import br.com.sweetbites.dto.DadosCartao;
import br.com.sweetbites.dto.ResultadoPagamento;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagamentoServiceTest {

    // "Hoje" fixo: 10/11/2026
    private final PagamentoService service = new PagamentoService(
            Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), ZoneId.of("America/Sao_Paulo")));

    private static DadosCartao cartao(String numero, String validade, int parcelas) {
        return new DadosCartao(numero, "ANA TESTE", validade, "123", parcelas);
    }

    @Test
    void aprovaCartaoValidoEGuardaSoOsUltimos4Digitos() {
        ResultadoPagamento r = service.processarCartao(cartao("4111 1111 1111 1111", "12/30", 3));
        assertThat(r.aprovado()).isTrue();
        assertThat(r.cartaoFinal()).isEqualTo("1111");
    }

    @Test
    void recusaCartaoTerminadoEm0002() {
        ResultadoPagamento r = service.processarCartao(cartao("4000000000000002", "12/30", 1));
        assertThat(r.aprovado()).as("cartão de teste de recusa (UC-08 curso Beta)").isFalse();
    }

    @Test
    void rejeitaNumeroQueNaoPassaNoLuhn() {
        assertThatThrownBy(() -> service.processarCartao(cartao("4111111111111112", "12/30", 1)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Número do cartão inválido");
    }

    @Test
    void rejeitaCartaoVencido() {
        assertThatThrownBy(() -> service.processarCartao(cartao("4111111111111111", "10/26", 1)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Validade");
    }

    @Test
    void aceitaCartaoQueVenceNoMesCorrente() {
        assertThat(service.processarCartao(cartao("4111111111111111", "11/26", 1)).aprovado()).isTrue();
    }

    @Test
    void parcelamentoMaximoDe3xSemJuros() {
        assertThatThrownBy(() -> service.processarCartao(cartao("4111111111111111", "12/30", 4)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("1 a 3x");
    }

    @Test
    void codigoPixIdentificaOPedido() {
        assertThat(service.gerarCodigoPix(42L)).contains("PEDIDO-00000042");
    }
}
