package br.com.sweetbites.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cupcake")
public class Cupcake {

    public static final int LIMITE_ESTOQUE_BAIXO = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(nullable = false, length = 500)
    private String ingredientes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private int estoque;

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "imagem_id")
    private ImagemProduto imagem;

    protected Cupcake() {
    }

    public Cupcake(String nome, String descricao, String ingredientes, BigDecimal preco, int estoque, Categoria categoria) {
        this.nome = nome;
        this.descricao = descricao;
        this.ingredientes = ingredientes;
        this.preco = preco;
        this.estoque = estoque;
        this.categoria = categoria;
    }

    /** RN (US-04 / US-15): estoque zero = indisponível. */
    public boolean isDisponivel() {
        return ativo && estoque > 0;
    }

    public boolean isEstoqueBaixo() {
        return estoque < LIMITE_ESTOQUE_BAIXO;
    }

    /** RN (US-15): estoque decrementado a cada pedido confirmado. */
    public void baixarEstoque(int quantidade) {
        if (quantidade > estoque) {
            throw new IllegalStateException("Estoque insuficiente para " + nome);
        }
        estoque -= quantidade;
    }

    public void devolverEstoque(int quantidade) {
        estoque += quantidade;
    }

    public Long getImagemId() {
        return imagem == null ? null : imagem.getId();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getIngredientes() { return ingredientes; }
    public BigDecimal getPreco() { return preco; }
    public int getEstoque() { return estoque; }
    public boolean isAtivo() { return ativo; }
    public Categoria getCategoria() { return categoria; }
    public ImagemProduto getImagem() { return imagem; }

    public void setNome(String nome) { this.nome = nome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setIngredientes(String ingredientes) { this.ingredientes = ingredientes; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public void setEstoque(int estoque) { this.estoque = estoque; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public void setImagem(ImagemProduto imagem) { this.imagem = imagem; }
}
