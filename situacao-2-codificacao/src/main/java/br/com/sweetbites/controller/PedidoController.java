package br.com.sweetbites.controller;

import br.com.sweetbites.config.UsuarioLogado;
import br.com.sweetbites.dto.CheckoutForm;
import br.com.sweetbites.model.FormaPagamento;
import br.com.sweetbites.model.Pedido;
import br.com.sweetbites.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** UC-08 Realizar Pedido, US-09 Pagamento, US-10 Acompanhar Status e notificações. */
@Controller
public class PedidoController {

    private final PedidoService pedidoService;
    private final NotificacaoService notificacaoService;
    private final QrCodeService qrCodeService;
    private final boolean modoDemonstracao;

    public PedidoController(PedidoService pedidoService, NotificacaoService notificacaoService,
                            QrCodeService qrCodeService,
                            @Value("${loja.modo-demonstracao:true}") boolean modoDemonstracao) {
        this.pedidoService = pedidoService;
        this.notificacaoService = notificacaoService;
        this.qrCodeService = qrCodeService;
        this.modoDemonstracao = modoDemonstracao;
    }

    @GetMapping("/checkout")
    public String checkout(Model model) {
        CheckoutForm form = new CheckoutForm();
        form.setFormaPagamento(FormaPagamento.PIX);
        prepararCheckout(model, form);
        return "pedido/checkout";
    }

    @PostMapping("/pedidos")
    public String finalizar(@AuthenticationPrincipal UsuarioLogado usuario,
                            @Valid @ModelAttribute("form") CheckoutForm form, BindingResult resultado,
                            Model model) {
        if (resultado.hasErrors()) {
            form.limparDadosCartao();
            prepararCheckout(model, form);
            return "pedido/checkout";
        }
        try {
            Pedido pedido = pedidoService.criarPedido(usuario.getId(), form);
            return "redirect:/pedidos/" + pedido.getId() + "?confirmado";
        } catch (RegraNegocioException e) {
            form.limparDadosCartao();
            model.addAttribute("erro", e.getMessage());
            prepararCheckout(model, form);
            return "pedido/checkout";
        }
    }

    private static void prepararCheckout(Model model, CheckoutForm form) {
        model.addAttribute("form", form);
        model.addAttribute("formasPagamento", FormaPagamento.values());
        model.addAttribute("maxParcelas", PagamentoService.MAX_PARCELAS);
        model.addAttribute("finalCartaoRecusado", PagamentoService.FINAL_CARTAO_RECUSADO);
    }

    @GetMapping("/pedidos")
    public String meusPedidos(@AuthenticationPrincipal UsuarioLogado usuario, Model model) {
        model.addAttribute("pedidos", pedidoService.listarDoCliente(usuario.getId()));
        return "pedido/lista";
    }

    @GetMapping("/pedidos/{id}")
    public String acompanhar(@AuthenticationPrincipal UsuarioLogado usuario, @PathVariable Long id,
                             @RequestParam(required = false) String confirmado, Model model) {
        model.addAttribute("pedido", pedidoService.buscarDoCliente(usuario.getId(), id));
        model.addAttribute("recemConfirmado", confirmado != null);
        return "pedido/acompanhar";
    }

    @GetMapping(value = "/pedidos/{id}/pix.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCodePix(@AuthenticationPrincipal UsuarioLogado usuario, @PathVariable Long id) {
        Pedido pedido = pedidoService.buscarDoCliente(usuario.getId(), id);
        if (pedido.getPagamento().getPixCodigo() == null) {
            throw new RecursoNaoEncontradoException("PIX não encontrado");
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(qrCodeService.gerarPng(pedido.getPagamento().getPixCodigo(), 240));
    }

    @PostMapping("/pedidos/{id}/pix/simular-pagamento")
    public String simularPix(@AuthenticationPrincipal UsuarioLogado usuario, @PathVariable Long id,
                             RedirectAttributes redirect) {
        if (!modoDemonstracao) {
            throw new RecursoNaoEncontradoException("Simulação desativada");
        }
        try {
            pedidoService.confirmarPixSimulado(usuario.getId(), id);
            redirect.addFlashAttribute("sucesso", "Pagamento PIX confirmado!");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    @PostMapping("/pedidos/{id}/cancelar")
    public String cancelar(@AuthenticationPrincipal UsuarioLogado usuario, @PathVariable Long id,
                           RedirectAttributes redirect) {
        try {
            pedidoService.cancelarPeloCliente(usuario.getId(), id);
            redirect.addFlashAttribute("sucesso", "Pedido cancelado.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    @GetMapping("/notificacoes")
    public String notificacoes(@AuthenticationPrincipal UsuarioLogado usuario, Model model) {
        model.addAttribute("notificacoes", notificacaoService.listar(usuario.getId()));
        notificacaoService.marcarTodasComoLidas(usuario.getId());
        model.addAttribute("notificacoesNaoLidas", 0L);
        return "pedido/notificacoes";
    }
}
