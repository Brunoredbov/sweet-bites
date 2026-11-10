package br.com.sweetbites.service;

import br.com.sweetbites.dto.DadosCartao;
import br.com.sweetbites.dto.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.YearMonth;
import java.util.Locale;

/**
 * Gateway de pagamento SIMULADO (US-09). Regras da simulação, exibidas também na tela:
 *  - cartão com número válido (algoritmo de Luhn), validade futura e CVV de 3-4 dígitos é aprovado;
 *  - cartão terminado em 0002 é sempre RECUSADO (para demonstrar o Curso Alternativo Beta do UC-08);
 *  - PIX gera um código "copia e cola" fictício que expira em 30 minutos.
 */
@Service
public class PagamentoService {

    /** RN (US-09): até 3x sem juros. */
    public static final int MAX_PARCELAS = 3;
    /** RN (US-09): PIX expira em 30 minutos. */
    public static final Duration VALIDADE_PIX = Duration.ofMinutes(30);
    public static final String FINAL_CARTAO_RECUSADO = "0002";

    private final Clock clock;

    public PagamentoService(Clock clock) {
        this.clock = clock;
    }

    /** Valida o formulário do cartão; lança RegraNegocioException com a mensagem do campo inválido. */
    public void validarCartao(DadosCartao cartao) {
        String numero = FreteService.somenteDigitos(cartao.numero());
        if (numero.length() < 13 || numero.length() > 19 || !luhnValido(numero)) {
            throw new RegraNegocioException("Número do cartão inválido.");
        }
        if (cartao.nomeTitular() == null || cartao.nomeTitular().isBlank()) {
            throw new RegraNegocioException("Informe o nome impresso no cartão.");
        }
        if (!validadeFutura(cartao.validade())) {
            throw new RegraNegocioException("Validade do cartão inválida ou vencida (use MM/AA).");
        }
        if (cartao.cvv() == null || !cartao.cvv().matches("\\d{3,4}")) {
            throw new RegraNegocioException("CVV inválido.");
        }
        if (cartao.parcelas() < 1 || cartao.parcelas() > MAX_PARCELAS) {
            throw new RegraNegocioException("Parcelamento disponível: de 1 a 3x sem juros.");
        }
    }

    public ResultadoPagamento processarCartao(DadosCartao cartao) {
        validarCartao(cartao);
        String numero = FreteService.somenteDigitos(cartao.numero());
        String finalCartao = numero.substring(numero.length() - 4);
        if (FINAL_CARTAO_RECUSADO.equals(finalCartao)) {
            return new ResultadoPagamento(false, finalCartao, "Transação recusada pela operadora (simulação).");
        }
        return new ResultadoPagamento(true, finalCartao, null);
    }

    /** Código PIX "copia e cola" fictício, identificado pelo número do pedido. */
    public String gerarCodigoPix(Long pedidoId) {
        return String.format(Locale.ROOT,
                "00020126580014BR.GOV.BCB.PIX0136SWEETBITES-DEMO-PEDIDO-%08d5204000053039865802BR5911SWEET BITES6009SAO PAULO6304DEMO",
                pedidoId);
    }

    static boolean luhnValido(String numero) {
        int soma = 0;
        boolean dobrar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int d = numero.charAt(i) - '0';
            if (dobrar) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            soma += d;
            dobrar = !dobrar;
        }
        return soma % 10 == 0;
    }

    private boolean validadeFutura(String validade) {
        if (validade == null || !validade.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            return false;
        }
        int mes = Integer.parseInt(validade.substring(0, 2));
        int ano = 2000 + Integer.parseInt(validade.substring(3));
        return !YearMonth.of(ano, mes).isBefore(YearMonth.now(clock));
    }
}
