package br.com.sweetbites.service;

import br.com.sweetbites.dto.CotacaoFrete;
import br.com.sweetbites.dto.EnderecoCep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FreteServiceTest {

    private final CepService cepFalso = cep -> switch (cep) {
        case "01310100" -> Optional.of(new EnderecoCep(cep, "Avenida Paulista", "Bela Vista", "São Paulo", "SP"));
        case "13010000" -> Optional.of(new EnderecoCep(cep, "Rua X", "Centro", "Campinas", "SP"));
        case "20040020" -> Optional.of(new EnderecoCep(cep, "Rua Y", "Centro", "Rio de Janeiro", "RJ"));
        default -> Optional.empty();
    };

    private final FreteService service = new FreteService(cepFalso, "Sao Paulo", "SP",
            new BigDecimal("7.00"), 45, new BigDecimal("15.00"), 120);

    @Test
    void mesmaCidadeDaLojaIgnorandoAcentos() {
        CotacaoFrete c = service.calcular("01310-100");
        assertThat(c.valor()).isEqualByComparingTo("7.00");
        assertThat(c.tempoEstimadoMin()).isEqualTo(45);
    }

    @Test
    void outraCidadeDoMesmoEstado() {
        CotacaoFrete c = service.calcular("13010000");
        assertThat(c.valor()).isEqualByComparingTo("15.00");
        assertThat(c.tempoEstimadoMin()).isEqualTo(120);
    }

    @Test
    void outroEstadoNaoTemEntrega() {
        assertThatThrownBy(() -> service.calcular("20040020"))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Ainda não entregamos");
    }

    @Test
    void cepInexistenteOuMalformado() {
        for (String cep : new String[]{"99999999", "123", "", null}) {
            assertThatThrownBy(() -> service.calcular(cep))
                    .as("CEP '%s' deve cair no curso alternativo Alfa", cep)
                    .isInstanceOf(RegraNegocioException.class).hasMessage("CEP não encontrado. Verifique o endereço.");
        }
    }
}
