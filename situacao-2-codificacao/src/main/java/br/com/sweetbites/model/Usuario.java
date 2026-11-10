package br.com.sweetbites.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    public static final int MAX_TENTATIVAS_LOGIN = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 160, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel = Papel.CLIENTE;

    @Column(name = "tentativas_falhas", nullable = false)
    private int tentativasFalhas;

    @Column(nullable = false)
    private boolean bloqueado;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    protected Usuario() {
    }

    public Usuario(String nome, String email, String senhaHash, String telefone, Papel papel) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.telefone = telefone;
        this.papel = papel;
    }

    /** RN (US-02): bloqueia a conta após 5 tentativas erradas. */
    public void registrarFalhaLogin() {
        tentativasFalhas++;
        if (tentativasFalhas >= MAX_TENTATIVAS_LOGIN) {
            bloqueado = true;
        }
    }

    public void registrarLoginComSucesso() {
        tentativasFalhas = 0;
    }

    /** Redefinir a senha também desbloqueia a conta. */
    public void redefinirSenha(String novoHash) {
        this.senhaHash = novoHash;
        this.tentativasFalhas = 0;
        this.bloqueado = false;
    }

    public boolean isAdmin() {
        return papel == Papel.ADMIN;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public String getTelefone() { return telefone; }
    public Papel getPapel() { return papel; }
    public int getTentativasFalhas() { return tentativasFalhas; }
    public boolean isBloqueado() { return bloqueado; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
