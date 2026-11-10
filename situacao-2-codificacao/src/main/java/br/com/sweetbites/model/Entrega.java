package br.com.sweetbites.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "entrega")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(nullable = false, length = 8)
    private String cep;

    @Column(nullable = false, length = 160)
    private String logradouro;

    @Column(nullable = false, length = 10)
    private String numero;

    @Column(length = 80)
    private String complemento;

    @Column(nullable = false, length = 80)
    private String bairro;

    @Column(nullable = false, length = 80)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(name = "tempo_estimado_min", nullable = false)
    private int tempoEstimadoMin;

    @Column(name = "data_prevista", nullable = false)
    private LocalDateTime dataPrevista;

    protected Entrega() {
    }

    public Entrega(String cep, String logradouro, String numero, String complemento, String bairro,
                   String cidade, String uf, int tempoEstimadoMin, LocalDateTime dataPrevista) {
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
        this.tempoEstimadoMin = tempoEstimadoMin;
        this.dataPrevista = dataPrevista;
    }

    public String getEnderecoFormatado() {
        String compl = (complemento == null || complemento.isBlank()) ? "" : " - " + complemento;
        return logradouro + ", " + numero + compl + " - " + bairro + ", " + cidade + "/" + uf
                + " - CEP " + cep.substring(0, 5) + "-" + cep.substring(5);
    }

    void setPedido(Pedido pedido) { this.pedido = pedido; }

    public void setDataPrevista(LocalDateTime dataPrevista) { this.dataPrevista = dataPrevista; }

    public Long getId() { return id; }
    public String getCep() { return cep; }
    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getComplemento() { return complemento; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getUf() { return uf; }
    public int getTempoEstimadoMin() { return tempoEstimadoMin; }
    public LocalDateTime getDataPrevista() { return dataPrevista; }
}
