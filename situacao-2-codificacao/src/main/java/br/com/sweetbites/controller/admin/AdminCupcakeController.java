package br.com.sweetbites.controller.admin;

import br.com.sweetbites.dto.CupcakeForm;
import br.com.sweetbites.service.CatalogoService;
import br.com.sweetbites.service.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** US-14 Gerenciar Produtos. */
@Controller
@RequestMapping("/admin/cupcakes")
public class AdminCupcakeController {

    private final CatalogoService catalogoService;

    public AdminCupcakeController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("cupcakes", catalogoService.listarAtivos());
        return "admin/cupcakes";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        return formulario(model, new CupcakeForm(), null);
    }

    @PostMapping("/novo")
    public String criar(@Valid @ModelAttribute("form") CupcakeForm form, BindingResult resultado,
                        @RequestParam(required = false) MultipartFile imagem, Model model,
                        RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return formulario(model, form, null);
        }
        try {
            catalogoService.criar(form, imagem);
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return formulario(model, form, null);
        }
        redirect.addFlashAttribute("sucesso", "Cupcake \"" + form.getNome() + "\" cadastrado.");
        return "redirect:/admin/cupcakes";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        return formulario(model, CupcakeForm.de(catalogoService.detalhe(id)), id);
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") CupcakeForm form,
                            BindingResult resultado, @RequestParam(required = false) MultipartFile imagem,
                            Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return formulario(model, form, id);
        }
        try {
            catalogoService.atualizar(id, form, imagem);
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return formulario(model, form, id);
        }
        redirect.addFlashAttribute("sucesso", "Cupcake \"" + form.getNome() + "\" atualizado.");
        return "redirect:/admin/cupcakes";
    }

    @PostMapping("/{id}/remover")
    public String remover(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            catalogoService.remover(id);
            redirect.addFlashAttribute("sucesso", "Cupcake removido do cardápio.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/cupcakes";
    }

    private String formulario(Model model, CupcakeForm form, Long id) {
        model.addAttribute("form", form);
        model.addAttribute("cupcakeId", id);
        model.addAttribute("imagemAtualId", id == null ? null : catalogoService.detalhe(id).getImagemId());
        model.addAttribute("categorias", catalogoService.categorias());
        return "admin/cupcake-form";
    }
}
