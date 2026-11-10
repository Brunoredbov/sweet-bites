package br.com.sweetbites.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Formulário da TELA-CADASTRO (UC-01 / US-01). */
public class CadastroForm {

    public static final String REGRA_SENHA = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";

    @NotBlank(message = "Informe seu nome.")
    @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe seu e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 160, message = "O e-mail pode ter no máximo 160 caracteres.")
    private String email;

    @NotBlank(message = "Informe uma senha.")
    @Pattern(regexp = REGRA_SENHA, message = "A senha deve ter no mínimo 8 caracteres, com letras e números.")
    private String senha;

    @NotBlank(message = "Confirme a senha.")
    private String confirmarSenha;

    @NotBlank(message = "Informe seu telefone.")
    @Pattern(regexp = "^\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}$", message = "Informe um telefone válido com DDD, ex.: (11) 91234-5678.")
    private String telefone;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getConfirmarSenha() { return confirmarSenha; }
    public void setConfirmarSenha(String confirmarSenha) { this.confirmarSenha = confirmarSenha; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
