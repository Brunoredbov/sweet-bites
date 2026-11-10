package br.com.sweetbites.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RedefinirSenhaForm {

    @NotBlank
    private String token;

    @NotBlank(message = "Informe a nova senha.")
    @Pattern(regexp = CadastroForm.REGRA_SENHA, message = "A senha deve ter no mínimo 8 caracteres, com letras e números.")
    private String senha;

    @NotBlank(message = "Confirme a nova senha.")
    private String confirmarSenha;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getConfirmarSenha() { return confirmarSenha; }
    public void setConfirmarSenha(String confirmarSenha) { this.confirmarSenha = confirmarSenha; }
}
