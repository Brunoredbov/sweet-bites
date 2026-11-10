package br.com.sweetbites.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    private Usuario novoUsuario() {
        return new Usuario("Ana", "ana@teste.local", "hash", "(11) 91234-5678", Papel.CLIENTE);
    }

    @Test
    void bloqueiaContaNaQuintaTentativaErrada() {
        Usuario u = novoUsuario();
        for (int i = 1; i <= 4; i++) {
            u.registrarFalhaLogin();
            assertThat(u.isBloqueado()).as("não deve bloquear na tentativa %d", i).isFalse();
        }
        u.registrarFalhaLogin();
        assertThat(u.isBloqueado()).as("RN US-02: bloqueia após 5 tentativas").isTrue();
    }

    @Test
    void loginComSucessoZeraTentativas() {
        Usuario u = novoUsuario();
        u.registrarFalhaLogin();
        u.registrarFalhaLogin();
        u.registrarLoginComSucesso();
        assertThat(u.getTentativasFalhas()).isZero();
    }

    @Test
    void redefinirSenhaDesbloqueiaConta() {
        Usuario u = novoUsuario();
        for (int i = 0; i < 5; i++) u.registrarFalhaLogin();
        u.redefinirSenha("novoHash");
        assertThat(u.isBloqueado()).isFalse();
        assertThat(u.getTentativasFalhas()).isZero();
        assertThat(u.getSenhaHash()).isEqualTo("novoHash");
    }
}
