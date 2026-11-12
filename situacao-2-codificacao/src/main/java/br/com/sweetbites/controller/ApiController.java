package br.com.sweetbites.controller;

import br.com.sweetbites.config.UsuarioLogado;
import br.com.sweetbites.dto.CotacaoFrete;
import br.com.sweetbites.dto.ResumoCarrinho;
import br.com.sweetbites.model.Pedido;
import br.com.sweetbites.service.CarrinhoService;
import br.com.sweetbites.service.FreteService;
import br.com.sweetbites.service.PedidoService;
import br.com.sweetbites.service.RecursoNaoEncontradoException;
import br.com.sweetbites.service.RegraNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Endpoints JSON usados pelo JavaScript das telas (carrinho, checkout e acompanhamento). */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final CarrinhoService carrinhoService;
    private final FreteService freteService;
    private final PedidoService pedidoService;

    public ApiController(CarrinhoService carrinhoService, FreteService freteService, PedidoService pedidoService) {
        this.carrinhoService = carrinhoService;
        this.freteService = freteService;
        this.pedidoService = pedidoService;
    }

    /** Recalcula o carrinho do navegador com preços e estoque reais. */
    @GetMapping("/carrinho")
    public ResumoCarrinho carrinho(@RequestParam(defaultValue = "") String itens) {
        return carrinhoService.resumir(CarrinhoService.interpretar(itens));
    }

    @GetMapping("/frete")
    public CotacaoFrete frete(@RequestParam String cep) {
        return freteService.calcular(cep);
    }

    @GetMapping("/pedidos/{id}/status")
    public Map<String, String> statusPedido(@AuthenticationPrincipal UsuarioLogado usuario, @PathVariable Long id) {
        Pedido pedido = pedidoService.buscarDoCliente(usuario.getId(), id);
        return Map.of("status", pedido.getStatus().name(), "descricao", pedido.getStatus().getDescricao());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, String>> regraNegocio(RegraNegocioException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler({RecursoNaoEncontradoException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> naoEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", "Não encontrado"));
    }
}
