package br.com.sweetbites.service;

import br.com.sweetbites.dto.CheckoutForm;
import br.com.sweetbites.dto.CotacaoFrete;
import br.com.sweetbites.dto.EnderecoCep;
import br.com.sweetbites.model.*;
import br.com.sweetbites.repository.CupcakeRepository;
import br.com.sweetbites.repository.PedidoRepository;
import br.com.sweetbites.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static br.com.sweetbites.service.CarrinhoServiceTest.cupcake;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PedidoServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final LocalDateTime agora = LocalDateTime.now(clock);

    private PedidoRepository pedidoRepository;
    private CupcakeRepository cupcakeRepository;
    private NotificacaoService notificacaoService;
    private PedidoService service;
    private Usuario cliente;
    private Cupcake baunilha;

    @BeforeEach
    void setUp() {
        pedidoRepository = mock(PedidoRepository.class);
        cupcakeRepository = mock(CupcakeRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        FreteService freteService = mock(FreteService.class);
        notificacaoService = mock(NotificacaoService.class);
        TransactionTemplate tx = new TransactionTemplate(mock(PlatformTransactionManager.class));

        service = new PedidoService(pedidoRepository, cupcakeRepository, usuarioRepository, freteService,
                new PagamentoService(clock), notificacaoService, mock(EmailService.class), tx, clock);

        cliente = new Usuario("Ana", "ana@teste.local", "hash", "(11) 91234-5678", Papel.CLIENTE);
        ReflectionTestUtils.setField(cliente, "id", 1L);
        baunilha = cupcake(10, "14.90", 5);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(cupcakeRepository.findAllByIdForUpdate(anyCollection())).thenReturn(List.of(baunilha));
        when(freteService.calcular(anyString())).thenReturn(new CotacaoFrete(
                new EnderecoCep("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP"), new BigDecimal("7.00"), 45));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 100L);
            return p;
        });
    }

    private CheckoutForm checkout(String itens, FormaPagamento forma) {
        CheckoutForm f = new CheckoutForm();
        f.setItens(itens);
        f.setCep("01310-100");
        f.setNumero("1000");
        f.setFormaPagamento(forma);
        if (forma == FormaPagamento.CARTAO) {
            f.setNumeroCartao("4111 1111 1111 1111");
            f.setNomeCartao("ANA");
            f.setValidadeCartao("12/30");
            f.setCvv("123");
            f.setParcelas(2);
        }
        return f;
    }

    @Test
    void pedidoComCartaoAprovadoVaiParaPreparacaoEBaixaEstoque() {
        Pedido p = service.criarPedido(1L, checkout("10:2", FormaPagamento.CARTAO));

        assertThat(p.getStatus()).isEqualTo(StatusPedido.EM_PREPARACAO);
        assertThat(p.getSubtotal()).isEqualByComparingTo("29.80");
        assertThat(p.getValorTotal()).as("subtotal + frete").isEqualByComparingTo("36.80");
        assertThat(p.getPagamento().getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(p.getPagamento().getCartaoFinal()).isEqualTo("1111");
        assertThat(baunilha.getEstoque()).as("RN US-15: estoque decrementado").isEqualTo(3);
        verify(notificacaoService).notificarPedido(eq(p), anyString());
    }

    @Test
    void cartaoRecusadoNaoGravaPedidoNemMexeNoEstoque() {
        CheckoutForm form = checkout("10:2", FormaPagamento.CARTAO);
        form.setNumeroCartao("4000 0000 0000 0002");

        assertThatThrownBy(() -> service.criarPedido(1L, form))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Pagamento não aprovado. Tente outro método de pagamento.");
        verify(pedidoRepository, never()).save(any());
        assertThat(baunilha.getEstoque()).isEqualTo(5);
    }

    @Test
    void pixFicaAguardandoPagamentoComValidadeDe30Minutos() {
        Pedido p = service.criarPedido(1L, checkout("10:2", FormaPagamento.PIX));

        assertThat(p.getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        assertThat(p.getPagamento().getPixExpiraEm()).isEqualTo(agora.plusMinutes(30));
        assertThat(p.getPagamento().getPixCodigo()).contains("PEDIDO-00000100");
    }

    @Test
    void pedidoMinimoDe15Reais() {
        assertThatThrownBy(() -> service.criarPedido(1L, checkout("10:1", FormaPagamento.DINHEIRO)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("pedido mínimo");
    }

    @Test
    void naoVendeMaisDoQueOEstoque() {
        assertThatThrownBy(() -> service.criarPedido(1L, checkout("10:6", FormaPagamento.DINHEIRO)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("só temos 5");
    }

    @Test
    void maximoDe20UnidadesPorItem() {
        assertThatThrownBy(() -> service.criarPedido(1L, checkout("10:21", FormaPagamento.DINHEIRO)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Máximo de 20");
    }

    @Test
    void carrinhoVazio() {
        assertThatThrownBy(() -> service.criarPedido(1L, checkout("", FormaPagamento.PIX)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("vazio");
    }

    // ---------------------------------------------------------------- admin

    private Pedido pedidoNoStatus(StatusPedido status, FormaPagamento forma) {
        Pedido p = new Pedido(cliente, forma, agora);
        p.adicionarItem(new ItemPedido(baunilha, 2, baunilha.getPreco()));
        p.definirPagamento(new Pagamento(forma, p.getValorTotal()));
        p.atualizarStatus(status);
        ReflectionTestUtils.setField(p, "id", 7L);
        when(pedidoRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void adminAvancaStatusSomenteNoFluxoENotificaCliente() {
        Pedido p = pedidoNoStatus(StatusPedido.EM_PREPARACAO, FormaPagamento.CARTAO);

        assertThatThrownBy(() -> service.atualizarStatusAdmin(7L, StatusPedido.ENTREGUE))
                .as("não pode pular etapa").isInstanceOf(RegraNegocioException.class);

        service.atualizarStatusAdmin(7L, StatusPedido.SAIU_PARA_ENTREGA);
        assertThat(p.getStatus()).isEqualTo(StatusPedido.SAIU_PARA_ENTREGA);
        verify(notificacaoService).notificarPedido(eq(p), contains("saiu para entrega"));
    }

    @Test
    void entregaEmDinheiroAprovaOPagamento() {
        Pedido p = pedidoNoStatus(StatusPedido.SAIU_PARA_ENTREGA, FormaPagamento.DINHEIRO);
        service.atualizarStatusAdmin(7L, StatusPedido.ENTREGUE);
        assertThat(p.getPagamento().getStatus()).isEqualTo(StatusPagamento.APROVADO);
    }

    @Test
    void pedidoEntregueNaoPodeSerCancelado() {
        pedidoNoStatus(StatusPedido.ENTREGUE, FormaPagamento.CARTAO);
        assertThatThrownBy(() -> service.atualizarStatusAdmin(7L, StatusPedido.CANCELADO))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("não podem ser cancelados");
    }

    @Test
    void cancelarDevolveEstoqueEEstornaPagamentoAprovado() {
        for (StatusPedido status : List.of(StatusPedido.EM_PREPARACAO, StatusPedido.SAIU_PARA_ENTREGA)) {
            baunilha.setEstoque(3);
            Pedido p = pedidoNoStatus(status, FormaPagamento.CARTAO);
            p.getPagamento().aprovar(agora);

            service.atualizarStatusAdmin(7L, StatusPedido.CANCELADO);

            assertThat(p.getStatus()).as("cancelar a partir de %s", status).isEqualTo(StatusPedido.CANCELADO);
            assertThat(baunilha.getEstoque()).as("estoque devolvido (%s)", status).isEqualTo(5);
            assertThat(p.getPagamento().getStatus()).isEqualTo(StatusPagamento.ESTORNADO);
        }
    }

    @Test
    void pixVencidoCancelaPedidoEDevolveEstoque() {
        baunilha.setEstoque(3);
        Pedido p = pedidoNoStatus(StatusPedido.AGUARDANDO_PAGAMENTO, FormaPagamento.PIX);
        p.getPagamento().setPixExpiraEm(agora.minusMinutes(1));
        when(pedidoRepository.findIdsPixExpirados(any(), any(), any(), any())).thenReturn(List.of(7L));

        assertThat(service.expirarPixVencidos()).isEqualTo(1);
        assertThat(p.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(p.getPagamento().getStatus()).isEqualTo(StatusPagamento.EXPIRADO);
        assertThat(baunilha.getEstoque()).isEqualTo(5);
    }

    @Test
    void clienteNaoAcessaPedidoDeOutroCliente() {
        Pedido p = pedidoNoStatus(StatusPedido.EM_PREPARACAO, FormaPagamento.PIX);
        when(pedidoRepository.findDetalhadoById(7L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.buscarDoCliente(2L, 7L))
                .as("pedido de outro cliente responde 404, como se não existisse")
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(service.buscarDoCliente(1L, 7L)).isSameAs(p);
    }
}
