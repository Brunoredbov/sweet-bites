package br.com.sweetbites.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaPagamento metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private int parcelas = 1;

    /** Apenas os 4 últimos dígitos — o número do cartão nunca é guardado. */
    @Column(name = "cartao_final", length = 4)
    private String cartaoFinal;

    @Column(name = "pix_codigo")
    private String pixCodigo;

    @Column(name = "pix_expira_em")
    private LocalDateTime pixExpiraEm;

    @Column(name = "data_aprovacao")
    private LocalDateTime dataAprovacao;

    protected Pagamento() {
    }

    public Pagamento(FormaPagamento metodo, BigDecimal valor) {
        this.metodo = metodo;
        this.valor = valor;
    }

    public void aprovar(LocalDateTime agora) {
        this.status = StatusPagamento.APROVADO;
        this.dataAprovacao = agora;
    }

    public boolean isPixPendente() {
        return metodo == FormaPagamento.PIX && status == StatusPagamento.PENDENTE;
    }

    void setPedido(Pedido pedido) { this.pedido = pedido; }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public FormaPagamento getMetodo() { return metodo; }
    public StatusPagamento getStatus() { return status; }
    public BigDecimal getValor() { return valor; }
    public int getParcelas() { return parcelas; }
    public String getCartaoFinal() { return cartaoFinal; }
    public String getPixCodigo() { return pixCodigo; }
    public LocalDateTime getPixExpiraEm() { return pixExpiraEm; }
    public LocalDateTime getDataAprovacao() { return dataAprovacao; }

    public void setStatus(StatusPagamento status) { this.status = status; }
    public void setParcelas(int parcelas) { this.parcelas = parcelas; }
    public void setCartaoFinal(String cartaoFinal) { this.cartaoFinal = cartaoFinal; }
    public void setPixCodigo(String pixCodigo) { this.pixCodigo = pixCodigo; }
    public void setPixExpiraEm(LocalDateTime pixExpiraEm) { this.pixExpiraEm = pixExpiraEm; }
}
