package br.com.sweetbites.controller;

import br.com.sweetbites.model.ImagemProduto;
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

    public LojaController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
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
}
