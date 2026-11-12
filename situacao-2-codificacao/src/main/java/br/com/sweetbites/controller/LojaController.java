package br.com.sweetbites.controller;

import br.com.sweetbites.config.UsuarioLogado;
import br.com.sweetbites.dto.AlterarSenhaForm;
import br.com.sweetbites.model.ImagemProduto;
import br.com.sweetbites.service.RegraNegocioException;
import br.com.sweetbites.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import br.com.sweetbites.service.CatalogoService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;

/** Vitrine (US-04), busca (US-05), detalhes (US-06) e carrinho (US-07). */
@Controller
public class LojaController {

    private final CatalogoService catalogoService;
    private final UsuarioService usuarioService;

    public LojaController(CatalogoService catalogoService, UsuarioService usuarioService) {
        this.catalogoService = catalogoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/")
    public String vitrine(@RequestParam(required = false) String busca,
                          @RequestParam(required = false) Long categoria, Model model) {
        model.addAttribute("categorias", catalogoService.categorias());
        model.addAttribute("cupcakes", catalogoService.vitrine(busca, categoria));
        model.addAttribute("busca", busca);
        model.addAttribute("categoriaSelecionada", categoria);
        return "loja/vitrine";
    }

    /** Busca em tempo real: devolve só o fragmento do grid, trocado via JavaScript. */
    @GetMapping("/vitrine/grid")
    public String gridVitrine(@RequestParam(required = false) String busca,
                              @RequestParam(required = false) Long categoria, Model model) {
        model.addAttribute("cupcakes", catalogoService.vitrine(busca, categoria));
        return "loja/vitrine :: grid";
    }

    @GetMapping("/cupcakes/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        model.addAttribute("cupcake", catalogoService.detalhe(id));
        return "loja/detalhe";
    }

    @GetMapping("/imagens/{id}")
    public ResponseEntity<byte[]> imagem(@PathVariable Long id) {
        ImagemProduto imagem = catalogoService.imagem(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.getTipo()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .body(imagem.getDados());
    }

    @GetMapping("/carrinho")
    public String carrinho() {
        return "loja/carrinho";
    }

    @GetMapping("/perfil")
    public String perfil() {
        return "loja/perfil";
    }

    @GetMapping("/perfil/senha")
    public String alterarSenha(Model model) {
        model.addAttribute("form", new AlterarSenhaForm());
        return "loja/alterar-senha";
    }

    @PostMapping("/perfil/senha")
    public String salvarSenha(@AuthenticationPrincipal UsuarioLogado usuario,
                              @Valid @ModelAttribute("form") AlterarSenhaForm form, BindingResult resultado,
                              RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return "loja/alterar-senha";
        }
        try {
            usuarioService.alterarSenha(usuario.getId(), form);
        } catch (RegraNegocioException e) {
            String campo = e.getMessage().contains("atual está") ? "senhaAtual" : "confirmarSenha";
            resultado.rejectValue(campo, "regra", e.getMessage());
            return "loja/alterar-senha";
        }
        redirect.addFlashAttribute("sucesso", "Senha alterada com sucesso!");
        return "redirect:/perfil";
    }
}
