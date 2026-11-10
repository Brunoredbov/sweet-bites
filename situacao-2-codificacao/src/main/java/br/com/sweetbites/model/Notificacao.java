package br.com.sweetbites.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Notificação dentro do app (simula a notificação push das US-08/US-10). */
@Entity
@Table(name = "notificacao")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Column(nullable = false)
    private String mensagem;

    @Column(nullable = false)
    private boolean lida;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    protected Notificacao() {
    }

    public Notificacao(Usuario usuario, Pedido pedido, String mensagem, LocalDateTime dataCriacao) {
        this.usuario = usuario;
        this.pedido = pedido;
        this.mensagem = mensagem;
        this.dataCriacao = dataCriacao;
    }

    public Long getPedidoId() {
        return pedido == null ? null : pedido.getId();
    }

    public void marcarComoLida() { this.lida = true; }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Pedido getPedido() { return pedido; }
    public String getMensagem() { return mensagem; }
    public boolean isLida() { return lida; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
