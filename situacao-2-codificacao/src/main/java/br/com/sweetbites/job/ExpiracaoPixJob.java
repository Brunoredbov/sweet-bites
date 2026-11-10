package br.com.sweetbites.job;

import br.com.sweetbites.service.PedidoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Verifica a cada minuto os pedidos com PIX vencido (RN da US-09: expira em 30 minutos). */
@Component
public class ExpiracaoPixJob {

    private final PedidoService pedidoService;

    public ExpiracaoPixJob(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @Scheduled(fixedDelayString = "${loja.pix.verificacao-ms:60000}", initialDelay = 30000)
    public void executar() {
        pedidoService.expirarPixVencidos();
    }
}
