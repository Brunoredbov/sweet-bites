package br.com.sweetbites.service;

import br.com.sweetbites.dto.*;
import br.com.sweetbites.model.*;
import br.com.sweetbites.repository.CupcakeRepository;
import br.com.sweetbites.repository.PedidoRepository;
import br.com.sweetbites.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** UC-08 Realizar Pedido (US-08/09/10) e gestão de pedidos pelo admin (US-16). */
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;
    private final CupcakeRepository cupcakeRepository;
    private final UsuarioRepository usuarioRepository;
    private final FreteService freteService;
    private final PagamentoService pagamentoService;
    private final NotificacaoService notificacaoService;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public PedidoService(PedidoRepository pedidoRepository, CupcakeRepository cupcakeRepository,
                         UsuarioRepository usuarioRepository, FreteService freteService,
                         PagamentoService pagamentoService, NotificacaoService notificacaoService,
                         EmailService emailService, TransactionTemplate transactionTemplate, Clock clock) {
        this.pedidoRepository = pedidoRepository;
        this.cupcakeRepository = cupcakeRepository;
        this.usuarioRepository = usuarioRepository;
        this.freteService = freteService;
        this.pagamentoService = pagamentoService;
        this.notificacaoService = notificacaoService;
        this.emailService = emailService;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ cliente

    /**
     * Fluxo principal do UC-08. A consulta de CEP (rede) acontece ANTES de abrir a transação,
     * para não segurar conexão do banco esperando a API externa.
     */
    public Pedido criarPedido(Long usuarioId, CheckoutForm form) {
        List<ItemCarrinho> itens = CarrinhoService.interpretar(form.getItens());
        if (itens.isEmpty()) {
            throw new RegraNegocioException("Seu carrinho está vazio.");
        }
        for (ItemCarrinho item : itens) {
            if (item.quantidade() > Pedido.MAX_QUANTIDADE_POR_ITEM) {
                throw new RegraNegocioException("Máximo de " + Pedido.MAX_QUANTIDADE_POR_ITEM + " unidades por item.");
            }
        }
        DadosCartao cartao = null;
        if (form.getFormaPagamento() == FormaPagamento.CARTAO) {
            cartao = new DadosCartao(form.getNumeroCartao(), form.getNomeCartao(), form.getValidadeCartao(),
                    form.getCvv(), form.getParcelas() == null ? 1 : form.getParcelas());
            pagamentoService.validarCartao(cartao);
        }
        CotacaoFrete frete = freteService.calcular(form.getCep());
        String logradouro = preferir(frete.endereco().logradouro(), form.getLogradouro());
        String bairro = preferir(frete.endereco().bairro(), form.getBairro());
        if (logradouro.isBlank() || bairro.isBlank()) {
            throw new RegraNegocioException("Informe a rua e o bairro do endereço de entrega.");
        }
        DadosCartao cartaoFinal = cartao;
        Pedido pedido = transactionTemplate.execute(status ->
                registrarPedido(usuarioId, itens, form, frete, logradouro, bairro, cartaoFinal));
        log.info("[pedido][criar] pedido={} usuario={} forma={} total={}",
                pedido.getId(), usuarioId, pedido.getFormaPagamento(), pedido.getValorTotal());
        return pedido;
    }

    private Pedido registrarPedido(Long usuarioId, List<ItemCarrinho> itens, CheckoutForm form, CotacaoFrete frete,
                                   String logradouro, String bairro, DadosCartao cartao) {
        LocalDateTime agora = LocalDateTime.now(clock);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        // Lock pessimista nos cupcakes: dois clientes não compram a última unidade ao mesmo tempo.
        Map<Long, Cupcake> cupcakes = cupcakeRepository
                .findAllByIdForUpdate(itens.stream().map(ItemCarrinho::cupcakeId).toList())
                .stream().collect(Collectors.toMap(Cupcake::getId, Function.identity()));

        Pedido pedido = new Pedido(usuario, form.getFormaPagamento(), agora);
        for (ItemCarrinho item : itens) {
            Cupcake c = cupcakes.get(item.cupcakeId());
            if (c == null || !c.isAtivo()) {
                throw new RegraNegocioException("Um item do carrinho não está mais disponível. Revise seu carrinho.");
            }
            if (item.quantidade() > c.getEstoque()) {
                throw new RegraNegocioException(c.getEstoque() == 0
                        ? c.getNome() + " está indisponível no momento."
                        : c.getNome() + ": só temos " + c.getEstoque() + " unidade(s) em estoque.");
            }
            pedido.adicionarItem(new ItemPedido(c, item.quantidade(), c.getPreco()));
        }
        if (pedido.getSubtotal().compareTo(Pedido.VALOR_MINIMO) < 0) {
            throw new RegraNegocioException("O pedido mínimo é de R$ 15,00 em produtos.");
        }
        pedido.definirFrete(frete.valor());
        EnderecoCep e = frete.endereco();
        pedido.definirEntrega(new Entrega(e.cep(), logradouro, form.getNumero().trim(), limpar(form.getComplemento()),
                bairro, e.cidade(), e.uf(), frete.tempoEstimadoMin(), agora.plusMinutes(frete.tempoEstimadoMin())));

        Pagamento pagamento = new Pagamento(form.getFormaPagamento(), pedido.getValorTotal());
        switch (form.getFormaPagamento()) {
            case CARTAO -> {
                ResultadoPagamento resultado = pagamentoService.processarCartao(cartao);
                if (!resultado.aprovado()) {
                    // Curso Alternativo Beta do UC-08: nada é gravado, o cliente escolhe outro método.
                    throw new RegraNegocioException("Pagamento não aprovado. Tente outro método de pagamento.");
                }
                pagamento.setParcelas(cartao.parcelas());
                pagamento.setCartaoFinal(resultado.cartaoFinal());
                pagamento.aprovar(agora);
                pedido.atualizarStatus(StatusPedido.EM_PREPARACAO);
            }
            case PIX -> {
                pagamento.setPixExpiraEm(agora.plus(PagamentoService.VALIDADE_PIX));
                pedido.atualizarStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
            }
            case DINHEIRO -> pedido.atualizarStatus(StatusPedido.EM_PREPARACAO);
        }
        pedido.definirPagamento(pagamento);

        // RN (US-15): estoque decrementado a cada pedido confirmado (reservado; devolvido se cancelar).
        pedido.getItens().forEach(i -> i.getCupcake().baixarEstoque(i.getQuantidade()));
        pedidoRepository.save(pedido);

        if (pagamento.getMetodo() == FormaPagamento.PIX) {
            pagamento.setPixCodigo(pagamentoService.gerarCodigoPix(pedido.getId()));
            notificacaoService.notificarPedido(pedido, "aguardando o pagamento do PIX (válido por 30 minutos).");
        } else {
            notificacaoService.notificarPedido(pedido, "confirmado! Já estamos preparando seus cupcakes.");
        }
        emailService.enviar(usuario.getEmail(), "Sweet Bites - pedido " + pedido.getNumero(),
                "Recebemos seu pedido " + pedido.getNumero() + " no valor de R$ " + pedido.getValorTotal() + ".");
        return pedido;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarDoCliente(Long usuarioId) {
        return pedidoRepository.findByUsuario(usuarioId);
    }

    /** Ownership: pedido de outro cliente responde 404, igual a pedido inexistente. */
    @Transactional(readOnly = true)
    public Pedido buscarDoCliente(Long usuarioId, Long pedidoId) {
        return pedidoRepository.findDetalhadoById(pedidoId)
                .filter(p -> p.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
    }

    /** Pagamento do PIX — SIMULADO pelo botão "Simular pagamento" (modo demonstração). */
    @Transactional
    public void confirmarPixSimulado(Long usuarioId, Long pedidoId) {
        Pedido pedido = travarPedidoDoCliente(usuarioId, pedidoId);
        Pagamento pg = pedido.getPagamento();
        LocalDateTime agora = LocalDateTime.now(clock);
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO || !pg.isPixPendente()) {
            throw new RegraNegocioException("Este pedido não está aguardando pagamento via PIX.");
        }
        if (agora.isAfter(pg.getPixExpiraEm())) {
            throw new RegraNegocioException("O código PIX expirou. Faça um novo pedido.");
        }
        pg.aprovar(agora);
        pedido.atualizarStatus(StatusPedido.EM_PREPARACAO);
        pedido.getEntrega().setDataPrevista(agora.plusMinutes(pedido.getEntrega().getTempoEstimadoMin()));
        notificacaoService.notificarPedido(pedido, "pagamento PIX recebido! Já estamos preparando seus cupcakes.");
    }

    /** Cliente pode cancelar enquanto o pedido não saiu para entrega. */
    @Transactional
    public void cancelarPeloCliente(Long usuarioId, Long pedidoId) {
        Pedido pedido = travarPedidoDoCliente(usuarioId, pedidoId);
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO && pedido.getStatus() != StatusPedido.EM_PREPARACAO) {
            throw new RegraNegocioException("Este pedido não pode mais ser cancelado.");
        }
        cancelar(pedido, StatusPagamento.CANCELADO);
        notificacaoService.notificarPedido(pedido, "cancelado conforme sua solicitação.");
    }

    private Pedido travarPedidoDoCliente(Long usuarioId, Long pedidoId) {
        return pedidoRepository.findByIdForUpdate(pedidoId)
                .filter(p -> p.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
    }

    // ------------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    public List<Pedido> listarAdmin(StatusPedido status, LocalDate de, LocalDate ate) {
        Collection<StatusPedido> filtroStatus = status == null ? EnumSet.allOf(StatusPedido.class) : EnumSet.of(status);
        LocalDateTime inicio = de == null ? LocalDateTime.of(2000, 1, 1, 0, 0) : de.atStartOfDay();
        LocalDateTime fim = ate == null ? LocalDateTime.of(2999, 12, 31, 0, 0) : ate.plusDays(1).atStartOfDay();
        return pedidoRepository.buscarAdmin(filtroStatus, inicio, fim);
    }

    @Transactional(readOnly = true)
    public Pedido buscarAdmin(Long pedidoId) {
        return pedidoRepository.findDetalhadoById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
    }

    /**
     * US-16: o admin avança o pedido no fluxo (preparação → saiu → entregue) ou cancela.
     * RN: pedido entregue não pode ser cancelado. Toda mudança notifica o cliente (CA#2).
     */
    @Transactional
    public void atualizarStatusAdmin(Long pedidoId, StatusPedido novoStatus) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
        StatusPedido atual = pedido.getStatus();
        if (novoStatus == StatusPedido.CANCELADO) {
            if (!atual.permiteCancelamento()) {
                throw new RegraNegocioException("Pedidos com status \"" + atual.getDescricao() + "\" não podem ser cancelados.");
            }
            Pagamento pg = pedido.getPagamento();
            cancelar(pedido, pg.getStatus() == StatusPagamento.APROVADO ? StatusPagamento.ESTORNADO : StatusPagamento.CANCELADO);
            notificacaoService.notificarPedido(pedido, "cancelado pela loja. Em caso de dúvidas, fale conosco.");
            return;
        }
        if (novoStatus != atual.proximo()) {
            throw new RegraNegocioException("Não é possível mudar de \"" + atual.getDescricao()
                    + "\" para \"" + novoStatus.getDescricao() + "\".");
        }
        pedido.atualizarStatus(novoStatus);
        if (novoStatus == StatusPedido.ENTREGUE && pedido.getPagamento().getMetodo() == FormaPagamento.DINHEIRO) {
            pedido.getPagamento().aprovar(LocalDateTime.now(clock));
        }
        notificacaoService.notificarPedido(pedido, switch (novoStatus) {
            case SAIU_PARA_ENTREGA -> "saiu para entrega! Fique de olho na campainha.";
            case ENTREGUE -> "entregue. Bom apetite!";
            default -> "status atualizado para " + novoStatus.getDescricao() + ".";
        });
    }

    @Transactional(readOnly = true)
    public PainelAdmin painel() {
        Map<StatusPedido, Long> porStatus = new EnumMap<>(StatusPedido.class);
        for (StatusPedido s : StatusPedido.values()) {
            porStatus.put(s, pedidoRepository.countByStatus(s));
        }
        return new PainelAdmin(porStatus, cupcakeRepository.findEstoqueBaixo(Cupcake.LIMITE_ESTOQUE_BAIXO));
    }

    // ------------------------------------------------------------------ job

    /** RN (US-09): PIX expira em 30 minutos — pedidos não pagos são cancelados e o estoque volta. */
    public int expirarPixVencidos() {
        LocalDateTime agora = LocalDateTime.now(clock);
        List<Long> ids = pedidoRepository.findIdsPixExpirados(StatusPedido.AGUARDANDO_PAGAMENTO,
                FormaPagamento.PIX, StatusPagamento.PENDENTE, agora);
        int expirados = 0;
        for (Long id : ids) {
            Boolean ok = transactionTemplate.execute(tx -> pedidoRepository.findByIdForUpdate(id)
                    .filter(p -> p.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO && p.getPagamento().isPixPendente())
                    .map(p -> {
                        cancelar(p, StatusPagamento.EXPIRADO);
                        notificacaoService.notificarPedido(p, "o código PIX expirou e o pedido foi cancelado.");
                        return true;
                    })
                    .orElse(false));
            if (Boolean.TRUE.equals(ok)) expirados++;
        }
        if (expirados > 0) {
            log.info("[pedido][expirar-pix] {} pedido(s) cancelado(s) por PIX expirado", expirados);
        }
        return expirados;
    }

    // ------------------------------------------------------------------ comum

    private void cancelar(Pedido pedido, StatusPagamento statusPagamento) {
        List<Long> ids = pedido.getItens().stream().map(i -> i.getCupcake().getId()).toList();
        cupcakeRepository.findAllByIdForUpdate(ids); // trava as linhas antes de devolver o estoque
        pedido.getItens().forEach(i -> i.getCupcake().devolverEstoque(i.getQuantidade()));
        pedido.getPagamento().setStatus(statusPagamento);
        pedido.atualizarStatus(StatusPedido.CANCELADO);
    }

    private static String preferir(String doCep, String doFormulario) {
        if (doCep != null && !doCep.isBlank()) return doCep;
        return doFormulario == null ? "" : doFormulario.trim();
    }

    private static String limpar(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
