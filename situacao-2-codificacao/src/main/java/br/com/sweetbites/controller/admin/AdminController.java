package br.com.sweetbites.controller.admin;

import br.com.sweetbites.dto.CategoriaForm;
import br.com.sweetbites.model.StatusPedido;
import br.com.sweetbites.service.CatalogoService;
import br.com.sweetbites.service.PedidoService;
import br.com.sweetbites.service.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** Painel do administrador: visão geral, pedidos (US-16), estoque (US-15) e categorias. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final PedidoService pedidoService;
    private final CatalogoService catalogoService;

    public AdminController(PedidoService pedidoService, CatalogoService catalogoService) {
        this.pedidoService = pedidoService;
        this.catalogoService = catalogoService;
    }

    @GetMapping
    public String painel(Model model) {
        model.addAttribute("painel", pedidoService.painel());
        return "admin/painel";
    }

    // ---------------- pedidos (US-16)

    @GetMapping("/pedidos")
    public String pedidos(@RequestParam(required = false) StatusPedido status,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                          Model model) {
        model.addAttribute("pedidos", pedidoService.listarAdmin(status, de, ate));
        model.addAttribute("statusList", StatusPedido.values());
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("de", de);
        model.addAttribute("ate", ate);
        return "admin/pedidos";
    }

    @GetMapping("/pedidos/{id}")
    public String pedido(@PathVariable Long id, Model model) {
        model.addAttribute("pedido", pedidoService.buscarAdmin(id));
        return "admin/pedido";
    }

    @PostMapping("/pedidos/{id}/status")
    public String atualizarStatus(@PathVariable Long id, @RequestParam StatusPedido status,
                                  RedirectAttributes redirect) {
        try {
            pedidoService.atualizarStatusAdmin(id, status);
            redirect.addFlashAttribute("sucesso", "Status atualizado para \"" + status.getDescricao() + "\". O cliente foi notificado.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/pedidos/" + id;
    }

    // ---------------- estoque (US-15)

    @GetMapping("/estoque")
    public String estoque(Model model) {
        model.addAttribute("cupcakes", catalogoService.listarAtivos());
        return "admin/estoque";
    }

    @PostMapping("/estoque/{id}")
    public String atualizarEstoque(@PathVariable Long id, @RequestParam(required = false) Integer quantidade,
                                   RedirectAttributes redirect) {
        try {
            if (quantidade == null) {
                throw new RegraNegocioException("Informe a quantidade.");
            }
            catalogoService.atualizarEstoque(id, quantidade);
            redirect.addFlashAttribute("sucesso", "Estoque atualizado.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/estoque";
    }

    // ---------------- categorias

    @GetMapping("/categorias")
    public String categorias(Model model) {
        model.addAttribute("categorias", catalogoService.categorias());
        model.addAttribute("form", new CategoriaForm());
        return "admin/categorias";
    }

    @PostMapping("/categorias")
    public String criarCategoria(@Valid @ModelAttribute("form") CategoriaForm form, BindingResult resultado,
                                 Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                catalogoService.criarCategoria(form);
                redirect.addFlashAttribute("sucesso", "Categoria criada.");
                return "redirect:/admin/categorias";
            } catch (RegraNegocioException e) {
                resultado.rejectValue("nome", "regra", e.getMessage());
            }
        }
        model.addAttribute("categorias", catalogoService.categorias());
        return "admin/categorias";
    }

    @PostMapping("/categorias/{id}/remover")
    public String removerCategoria(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            catalogoService.removerCategoria(id);
            redirect.addFlashAttribute("sucesso", "Categoria removida.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/categorias";
    }
}
