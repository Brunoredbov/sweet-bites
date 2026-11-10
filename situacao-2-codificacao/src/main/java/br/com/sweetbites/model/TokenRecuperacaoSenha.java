package br.com.sweetbites.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Token de redefinição de senha (US-18). Guarda só o hash SHA-256 do token. */
@Entity
@Table(name = "token_recuperacao_senha")
public class TokenRecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(nullable = false)
    private boolean usado;

    protected TokenRecuperacaoSenha() {
    }

    public TokenRecuperacaoSenha(Usuario usuario, String tokenHash, LocalDateTime expiraEm) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.expiraEm = expiraEm;
    }

    public boolean isValido(LocalDateTime agora) {
        return !usado && agora.isBefore(expiraEm);
    }

    public void marcarComoUsado() { this.usado = true; }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public boolean isUsado() { return usado; }
}
