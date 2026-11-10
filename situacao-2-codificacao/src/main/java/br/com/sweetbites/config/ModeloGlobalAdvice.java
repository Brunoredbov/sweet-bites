package br.com.sweetbites.config;

import br.com.sweetbites.service.NotificacaoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Atributos disponíveis em todas as telas (cabeçalho, contador de notificações, modo demo). */
@ControllerAdvice
public class ModeloGlobalAdvice {

    private final NotificacaoService notificacaoService;
    private final boolean modoDemonstracao;

    public ModeloGlobalAdvice(NotificacaoService notificacaoService,
                              @Value("${loja.modo-demonstracao:true}") boolean modoDemonstracao) {
        this.notificacaoService = notificacaoService;
        this.modoDemonstracao = modoDemonstracao;
    }

    @ModelAttribute("usuario")
    public UsuarioLogado usuario(@AuthenticationPrincipal UsuarioLogado usuario) {
        return usuario;
    }

    @ModelAttribute("notificacoesNaoLidas")
    public long notificacoesNaoLidas(@AuthenticationPrincipal UsuarioLogado usuario) {
        return usuario == null ? 0 : notificacaoService.contarNaoLidas(usuario.getId());
    }

    @ModelAttribute("modoDemonstracao")
    public boolean modoDemonstracao() {
        return modoDemonstracao;
    }
}
