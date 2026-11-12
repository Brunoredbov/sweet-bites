package br.com.sweetbites.service;

import br.com.sweetbites.dto.AlterarSenhaForm;
import br.com.sweetbites.dto.CadastroForm;
import br.com.sweetbites.model.Papel;
import br.com.sweetbites.model.Usuario;
import br.com.sweetbites.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** UC-01 / US-01: cadastra um cliente. RN01: e-mail único. RN02: senha validada no formulário. */
    @Transactional
    public Usuario cadastrar(CadastroForm form) {
        if (!form.getSenha().equals(form.getConfirmarSenha())) {
            throw new RegraNegocioException("As senhas não conferem.");
        }
        String email = normalizarEmail(form.getEmail());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Este e-mail já está cadastrado.");
        }
        Usuario usuario = new Usuario(form.getNome().trim(), email,
                passwordEncoder.encode(form.getSenha()), form.getTelefone().trim(), Papel.CLIENTE);
        usuarioRepository.save(usuario);
        emailService.enviar(email, "Bem-vindo(a) à Sweet Bites!",
                "Olá, " + usuario.getNome() + "! Sua conta foi criada com sucesso.");
        return usuario;
    }

    /** Troca de senha pelo usuário logado: exige a senha atual e uma nova senha diferente dela. */
    @Transactional
    public void alterarSenha(Long usuarioId, AlterarSenhaForm form) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        if (!passwordEncoder.matches(form.getSenhaAtual(), usuario.getSenhaHash())) {
            throw new RegraNegocioException("A senha atual está incorreta.");
        }
        if (!form.getNovaSenha().equals(form.getConfirmarSenha())) {
            throw new RegraNegocioException("As senhas não conferem.");
        }
        if (form.getNovaSenha().equals(form.getSenhaAtual())) {
            throw new RegraNegocioException("A nova senha deve ser diferente da atual.");
        }
        usuario.redefinirSenha(passwordEncoder.encode(form.getNovaSenha()));
        emailService.enviar(usuario.getEmail(), "Sweet Bites - sua senha foi alterada",
                "A senha da sua conta foi alterada. Se não foi você, use 'Esqueci minha senha'.");
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(normalizarEmail(email));
    }

    /** RN (US-02): conta bloqueada após 5 tentativas erradas. Retorna true se bloqueou agora. */
    @Transactional
    public boolean registrarFalhaLogin(String email) {
        return usuarioRepository.findByEmailIgnoreCase(normalizarEmail(email))
                .filter(u -> !u.isBloqueado())
                .map(u -> {
                    u.registrarFalhaLogin();
                    return u.isBloqueado();
                })
                .orElse(false);
    }

    @Transactional
    public void registrarLoginComSucesso(String email) {
        usuarioRepository.findByEmailIgnoreCase(normalizarEmail(email))
                .ifPresent(Usuario::registrarLoginComSucesso);
    }
}
