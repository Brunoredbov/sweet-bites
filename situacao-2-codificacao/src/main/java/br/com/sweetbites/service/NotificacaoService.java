package br.com.sweetbites.service;

import br.com.sweetbites.model.Notificacao;
import br.com.sweetbites.model.Pedido;
import br.com.sweetbites.repository.NotificacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Notificações dentro do app — simulam o push das US-08 e US-10. */
@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final Clock clock;

    public NotificacaoService(NotificacaoRepository notificacaoRepository, Clock clock) {
        this.notificacaoRepository = notificacaoRepository;
        this.clock = clock;
    }

    @Transactional
    public void notificarPedido(Pedido pedido, String mensagem) {
        notificacaoRepository.save(new Notificacao(pedido.getUsuario(), pedido,
                "Pedido " + pedido.getNumero() + ": " + mensagem, LocalDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    public List<Notificacao> listar(Long usuarioId) {
        return notificacaoRepository.findByUsuario(usuarioId);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long usuarioId) {
        return notificacaoRepository.countByUsuarioIdAndLidaFalse(usuarioId);
    }

    @Transactional
    public void marcarTodasComoLidas(Long usuarioId) {
        notificacaoRepository.marcarTodasComoLidas(usuarioId);
    }
}
