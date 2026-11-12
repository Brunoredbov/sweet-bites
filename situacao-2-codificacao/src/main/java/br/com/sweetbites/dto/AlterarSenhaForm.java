package br.com.sweetbites.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Troca de senha pelo próprio usuário logado (Perfil → Alterar senha). */
public class AlterarSenhaForm {

    @NotBlank(message = "Informe sua senha atual.")
    private String senhaAtual;

    @NotBlank(message = "Informe a nova senha.")
    @Pattern(regexp = CadastroForm.REGRA_SENHA, message = "A senha deve ter no mínimo 8 caracteres, com letras e números.")
    private String novaSenha;

    @NotBlank(message = "Confirme a nova senha.")
    private String confirmarSenha;

    public String getSenhaAtual() { return senhaAtual; }
    public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }
    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
    public String getConfirmarSenha() { return confirmarSenha; }
    public void setConfirmarSenha(String confirmarSenha) { this.confirmarSenha = confirmarSenha; }
}
