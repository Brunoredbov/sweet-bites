package br.com.sweetbites.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoriaForm {

    @NotBlank(message = "Informe o nome da categoria.")
    @Size(max = 60, message = "O nome pode ter no máximo 60 caracteres.")
    private String nome;

    @Size(max = 255, message = "A descrição pode ter no máximo 255 caracteres.")
    private String descricao;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
