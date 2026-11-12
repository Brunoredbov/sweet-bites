package br.com.sweetbites.controller;

import br.com.sweetbites.config.UsuarioLogado;
import br.com.sweetbites.dto.CadastroForm;
import br.com.sweetbites.dto.RedefinirSenhaForm;
import br.com.sweetbites.service.RecuperacaoSenhaService;
import br.com.sweetbites.service.RegraNegocioException;
import br.com.sweetbites.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Optional;

/** UC-01 Cadastrar Conta, US-02 Login e US-18 Recuperar Senha. */
@Controller
public class AuthController {

    private final UsuarioService usuarioService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public AuthController(UsuarioService usuarioService, RecuperacaoSenhaService recuperacaoSenhaService) {
        this.usuarioService = usuarioService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal UsuarioLogado usuario) {
        if (usuario != null) {
            return usuario.isAdmin() ? "redirect:/admin" : "redirect:/";
        }
        return "auth/login";
    }

    @GetMapping("/cadastro")
    public String cadastro(@AuthenticationPrincipal UsuarioLogado usuario, Model model) {
        if (usuario != null) {
            return "redirect:/";
        }
        model.addAttribute("form", new CadastroForm());
        return "auth/cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("form") CadastroForm form, BindingResult resultado,
                            RedirectAttributes redirect) {
        if (!resultado.hasFieldErrors("confirmarSenha") && form.getSenha() != null
                && !form.getSenha().equals(form.getConfirmarSenha())) {
            resultado.rejectValue("confirmarSenha", "senhas", "As senhas não conferem.");
        }
        if (resultado.hasErrors()) {
            return "auth/cadastro";
        }
        try {
            usuarioService.cadastrar(form);
        } catch (RegraNegocioException e) {
            resultado.rejectValue("email", "regra", e.getMessage());
            return "auth/cadastro";
        }
        redirect.addFlashAttribute("sucesso", "Conta criada com sucesso! Enviamos um e-mail de confirmação. Faça login para continuar.");
        return "redirect:/login";
    }

    @GetMapping("/recuperar-senha")
    public String recuperarSenha() {
        return "auth/recuperar-senha";
    }

    @PostMapping("/recuperar-senha")
    public String solicitarRecuperacao(@RequestParam(defaultValue = "") String email, HttpServletRequest request,
                                       Model model) {
        if (email.isBlank()) {
            model.addAttribute("erro", "Informe seu e-mail.");
            return "auth/recuperar-senha";
        }
        String urlBase = ServletUriComponentsBuilder.fromContextPath(request).build().toUriString();
        Optional<String> token = recuperacaoSenhaService.solicitar(email, urlBase);
        // Mesma mensagem existindo ou não a conta: não revela quem é cliente.
        model.addAttribute("enviado", true);
        model.addAttribute("tokenDemonstracao", token.orElse(null));
        return "auth/recuperar-senha";
    }

    @GetMapping("/redefinir-senha")
    public String redefinirSenha(@RequestParam(required = false) String token, Model model) {
        if (!recuperacaoSenhaService.tokenValido(token)) {
            model.addAttribute("tokenInvalido", true);
            return "auth/redefinir-senha";
        }
        RedefinirSenhaForm form = new RedefinirSenhaForm();
        form.setToken(token);
        model.addAttribute("form", form);
        return "auth/redefinir-senha";
    }

    @PostMapping("/redefinir-senha")
    public String redefinir(@Valid @ModelAttribute("form") RedefinirSenhaForm form, BindingResult resultado,
                            Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return "auth/redefinir-senha";
        }
        try {
            recuperacaoSenhaService.redefinir(form);
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "auth/redefinir-senha";
        }
        redirect.addFlashAttribute("sucesso", "Senha redefinida! Por segurança, encerramos todas as sessões abertas. Faça login com a nova senha.");
        return "redirect:/login";
    }
}
