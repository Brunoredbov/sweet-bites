package br.com.sweetbites.model;

import jakarta.persistence.*;

/**
 * Imagem do cupcake guardada no próprio banco (bytea), para não depender de disco
 * local — a hospedagem gratuita apaga arquivos a cada deploy.
 */
@Entity
@Table(name = "imagem_produto")
public class ImagemProduto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false)
    private byte[] dados;

    protected ImagemProduto() {
    }

    public ImagemProduto(String tipo, byte[] dados) {
        this.tipo = tipo;
        this.dados = dados;
    }

    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public byte[] getDados() { return dados; }
}
