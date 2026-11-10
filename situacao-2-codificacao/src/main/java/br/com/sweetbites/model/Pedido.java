package br.com.sweetbites.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class Pedido {

    /** RN (US-08): pedido mínimo de R$ 15,00 (sobre o subtotal dos itens). */
    public static final BigDecimal VALOR_MINIMO = new BigDecimal("15.00");
    /** RN (US-07): no máximo 20 unidades por item. */
    public static final int MAX_QUANTIDADE_POR_ITEM = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPedido status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal frete;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemPedido> itens = new ArrayList<>();

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Pagamento pagamento;

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Entrega entrega;

    protected Pedido() {
    }

    public Pedido(Usuario usuario, FormaPagamento formaPagamento, LocalDateTime dataCriacao) {
        this.usuario = usuario;
        this.formaPagamento = formaPagamento;
        this.dataCriacao = dataCriacao;
        this.subtotal = BigDecimal.ZERO;
        this.frete = BigDecimal.ZERO;
        this.valorTotal = BigDecimal.ZERO;
    }

    public void adicionarItem(ItemPedido item) {
        item.setPedido(this);
        itens.add(item);
        subtotal = subtotal.add(item.calcularSubtotal());
        valorTotal = subtotal.add(frete);
    }

    public void definirFrete(BigDecimal valorFrete) {
        this.frete = valorFrete;
        this.valorTotal = subtotal.add(valorFrete);
    }

    public void definirPagamento(Pagamento pagamento) {
        pagamento.setPedido(this);
        this.pagamento = pagamento;
    }

    public void definirEntrega(Entrega entrega) {
        entrega.setPedido(this);
        this.entrega = entrega;
    }

    public void atualizarStatus(StatusPedido novoStatus) {
        this.status = novoStatus;
    }

    public int getQuantidadeTotalItens() {
        return itens.stream().mapToInt(ItemPedido::getQuantidade).sum();
    }

    /** Número exibido ao cliente, ex.: #SB00042. */
    public String getNumero() {
        return id == null ? "" : String.format("#SB%05d", id);
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public StatusPedido getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getFrete() { return frete; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public List<ItemPedido> getItens() { return itens; }
    public Pagamento getPagamento() { return pagamento; }
    public Entrega getEntrega() { return entrega; }
}
