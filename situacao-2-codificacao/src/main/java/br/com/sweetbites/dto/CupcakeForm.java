package br.com.sweetbites.dto;

import br.com.sweetbites.model.Cupcake;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Formulário do admin para criar/editar cupcake (US-14). */
public class CupcakeForm {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 80, message = "O nome pode ter no máximo 80 caracteres.")
    private String nome;

    @NotBlank(message = "Informe a descrição.")
    @Size(max = 1000, message = "A descrição pode ter no máximo 1000 caracteres.")
    private String descricao;

    @NotBlank(message = "Informe os ingredientes.")
    @Size(max = 500, message = "Os ingredientes podem ter no máximo 500 caracteres.")
    private String ingredientes;

    @NotNull(message = "Informe o preço.")
    @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
    @Digits(integer = 6, fraction = 2, message = "Preço inválido.")
    private BigDecimal preco;

    @NotNull(message = "Informe o estoque.")
    @Min(value = 0, message = "O estoque não pode ser negativo.")
    @Max(value = 10000, message = "Estoque máximo: 10000.")
    private Integer estoque;

    @NotNull(message = "Escolha a categoria.")
    private Long categoriaId;

    public static CupcakeForm de(Cupcake c) {
        CupcakeForm f = new CupcakeForm();
        f.nome = c.getNome();
        f.descricao = c.getDescricao();
        f.ingredientes = c.getIngredientes();
        f.preco = c.getPreco();
        f.estoque = c.getEstoque();
        f.categoriaId = c.getCategoria().getId();
        return f;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getIngredientes() { return ingredientes; }
    public void setIngredientes(String ingredientes) { this.ingredientes = ingredientes; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public Integer getEstoque() { return estoque; }
    public void setEstoque(Integer estoque) { this.estoque = estoque; }
    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
}
