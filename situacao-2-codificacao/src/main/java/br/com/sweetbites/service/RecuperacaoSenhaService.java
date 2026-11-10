package br.com.sweetbites.service;

import br.com.sweetbites.dto.RedefinirSenhaForm;
import br.com.sweetbites.model.TokenRecuperacaoSenha;
import br.com.sweetbites.model.Usuario;
import br.com.sweetbites.repository.TokenRecuperacaoSenhaRepository;
import br.com.sweetbites.repository.UsuarioRepository;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/** US-18: recuperação de senha por link enviado por e-mail (simulado). */
@Service
public class RecuperacaoSenhaService {

    /** CA#2: o link expira em 1 hora. */
    public static final Duration VALIDADE_TOKEN = Duration.ofHours(1);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacaoSenhaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SessionRegistry sessionRegistry;
    private final Clock clock;

    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository, TokenRecuperacaoSenhaRepository tokenRepository,
                                   PasswordEncoder passwordEncoder, EmailService emailService,
                                   SessionRegistry sessionRegistry, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.sessionRegistry = sessionRegistry;
        this.clock = clock;
    }

    /**
     * Gera o token e "envia" o e-mail. Devolve o token em claro só para o modo demonstração
     * exibir o link na tela (já que o e-mail é simulado). Se o e-mail não existir, devolve
     * vazio — a tela mostra a mesma mensagem nos dois casos, sem revelar quem tem conta.
     */
    @Transactional
    public Optional<String> solicitar(String email, String urlBase) {
        Optional<Usuario> usuario = usuarioRepository.findByEmailIgnoreCase(UsuarioService.normalizarEmail(email));
        if (usuario.isEmpty()) {
            return Optional.empty();
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime agora = LocalDateTime.now(clock);
        tokenRepository.save(new TokenRecuperacaoSenha(usuario.get(), hash(token), agora.plus(VALIDADE_TOKEN)));
        emailService.enviar(usuario.get().getEmail(), "Sweet Bites - redefinição de senha",
                "Use o link para redefinir sua senha (válido por 1 hora): " + urlBase + "/redefinir-senha?token=" + token);
        return Optional.of(token);
    }

    @Transactional(readOnly = true)
    public boolean tokenValido(String token) {
        return token != null && tokenRepository.findByTokenHash(hash(token))
                .map(t -> t.isValido(LocalDateTime.now(clock)))
                .orElse(false);
    }

    /** RN: após redefinir, todas as sessões ativas do usuário são encerradas. */
    @Transactional
    public void redefinir(RedefinirSenhaForm form) {
        if (!form.getSenha().equals(form.getConfirmarSenha())) {
            throw new RegraNegocioException("As senhas não conferem.");
        }
        TokenRecuperacaoSenha token = tokenRepository.findByTokenHash(hash(form.getToken()))
                .filter(t -> t.isValido(LocalDateTime.now(clock)))
                .orElseThrow(() -> new RegraNegocioException("Link inválido ou expirado. Solicite um novo."));
        token.marcarComoUsado();
        Usuario usuario = token.getUsuario();
        usuario.redefinirSenha(passwordEncoder.encode(form.getSenha()));
        encerrarSessoes(usuario.getEmail());
    }

    private void encerrarSessoes(String email) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(p -> p instanceof org.springframework.security.core.userdetails.UserDetails ud
                        && ud.getUsername().equalsIgnoreCase(email))
                .flatMap(p -> sessionRegistry.getAllSessions(p, false).stream())
                .forEach(SessionInformation::expireNow);
    }

    static String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
