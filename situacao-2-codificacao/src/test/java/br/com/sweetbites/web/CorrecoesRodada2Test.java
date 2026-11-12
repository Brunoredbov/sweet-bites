package br.com.sweetbites.web;

import br.com.sweetbites.dto.EnderecoCep;
import br.com.sweetbites.service.CepService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Regressão das correções da 2ª rodada de qualidade (BUG-06 a BUG-13 do laudo). */
@SpringBootTest
@AutoConfigureMockMvc
class CorrecoesRodada2Test {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CepService cepService;

    @BeforeEach
    void cep() {
        when(cepService.consultar(anyString())).thenReturn(Optional.of(
                new EnderecoCep("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP")));
    }

    private MockHttpSession cliente(String email, String senha) throws Exception {
        mvc.perform(post("/cadastro").with(csrf()).param("nome", "Cliente Rodada").param("email", email)
                .param("telefone", "(11) 91234-5678").param("senha", senha).param("confirmarSenha", senha));
        return entrar(email, senha);
    }

    private MockHttpSession entrar(String email, String senha) throws Exception {
        MvcResult r = mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha").user(email).password(senha))
                .andReturn();
        assertThat(r.getResponse().getRedirectedUrl()).doesNotContain("erro");
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    @Test
    void bug06SessaoExpiradaAoEnviarFormularioLevaAoLoginComAviso() throws Exception {
        // Formulário enviado sem sessão válida (token CSRF antigo): antes mostrava "Acesso negado".
        mvc.perform(post("/pedidos").param("_csrf", "token-de-uma-sessao-que-expirou")
                        .param("itens", "1:2").param("cep", "01310100").param("numero", "1").param("formaPagamento", "PIX"))
                .andExpect(redirectedUrl("/login?expirada"));
        mvc.perform(get("/login").param("expirada", ""))
                .andExpect(content().string(containsString("Sua sessão foi encerrada")));
    }

    @Test
    void bug07ApiSemSessaoResponde401ParaOJavaScript() throws Exception {
        mvc.perform(get("/api/pedidos/1/status")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/carrinho").param("itens", "1:1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/pedidos")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void bug08ClienteAlteraSenhaPeloPerfil() throws Exception {
        MockHttpSession s = cliente("troca.senha@teste.local", "Senha1234");
        mvc.perform(get("/perfil").session(s)).andExpect(content().string(containsString("/perfil/senha")));

        mvc.perform(post("/perfil/senha").session(s).with(csrf())
                        .param("senhaAtual", "errada123").param("novaSenha", "NovaSenha99").param("confirmarSenha", "NovaSenha99"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A senha atual está incorreta.")));
        mvc.perform(post("/perfil/senha").session(s).with(csrf())
                        .param("senhaAtual", "Senha1234").param("novaSenha", "Senha1234").param("confirmarSenha", "Senha1234"))
                .andExpect(content().string(containsString("diferente da atual")));
        mvc.perform(post("/perfil/senha").session(s).with(csrf())
                        .param("senhaAtual", "Senha1234").param("novaSenha", "NovaSenha99").param("confirmarSenha", "NovaSenha99"))
                .andExpect(redirectedUrl("/perfil"));
        entrar("troca.senha@teste.local", "NovaSenha99");
    }

    @Test
    void bug09LayoutInformaOUsuarioParaSepararOCarrinho() throws Exception {
        MockHttpSession s = cliente("carrinho.separado@teste.local", "Senha1234");
        mvc.perform(get("/carrinho").session(s))
                .andExpect(content().string(containsString("name=\"usuario-id\"")));
    }

    @Test
    void bug10StatusDoPagamentoNoAdminEmPortugues() throws Exception {
        MockHttpSession comprador = cliente("pagamento.admin@teste.local", "Senha1234");
        MvcResult r = mvc.perform(post("/pedidos").session(comprador).with(csrf())
                        .param("itens", "1:2").param("cep", "01310-100").param("numero", "10").param("formaPagamento", "PIX"))
                .andReturn();
        String idPedido = r.getResponse().getRedirectedUrl().replaceAll("\\D", "");
        MockHttpSession admin = entrar("admin@teste.local", "AdminTeste123");
        mvc.perform(get("/admin/pedidos/" + idPedido).session(admin))
                .andExpect(content().string(containsString("Pendente")))
                .andExpect(content().string(not(containsString(">PENDENTE<"))));
    }

    @Test
    void bug11FiltroComPeriodoInvertidoAvisaOAdmin() throws Exception {
        MockHttpSession admin = entrar("admin@teste.local", "AdminTeste123");
        mvc.perform(get("/admin/pedidos").session(admin).param("de", "2026-12-31").param("ate", "2026-01-01"))
                .andExpect(content().string(containsString("posterior à data final")));
    }

    @Test
    void bug12IdInvalidoNaUrlMostraPaginaNaoEncontrada() throws Exception {
        MockHttpSession s = cliente("id.invalido@teste.local", "Senha1234");
        mvc.perform(get("/cupcakes/abc").session(s)).andExpect(status().isNotFound());
        mvc.perform(get("/pedidos/abc").session(s)).andExpect(status().isNotFound());
    }

    @Test
    void bug13UsuarioLogadoNaoVeTelaDeLoginDeNovo() throws Exception {
        MockHttpSession s = cliente("ja.logado@teste.local", "Senha1234");
        mvc.perform(get("/login").session(s)).andExpect(redirectedUrl("/"));
        mvc.perform(get("/cadastro").session(s)).andExpect(redirectedUrl("/"));
        MockHttpSession admin = entrar("admin@teste.local", "AdminTeste123");
        mvc.perform(get("/login").session(admin)).andExpect(redirectedUrl("/admin"));
    }

    @Test
    void bug14ImagemEntre5e10MBRecebeMensagemAmigavel() throws Exception {
        byte[] grande = new byte[6 * 1024 * 1024];
        grande[0] = (byte) 0x89; grande[1] = 'P'; grande[2] = 'N'; grande[3] = 'G';
        MockHttpSession admin = entrar("admin@teste.local", "AdminTeste123");
        mvc.perform(multipart("/admin/cupcakes/novo").file(new MockMultipartFile("imagem", "grande.png", "image/png", grande))
                        .session(admin).with(csrf())
                        .param("nome", "Grande").param("descricao", "d").param("ingredientes", "i")
                        .param("preco", "10").param("estoque", "1").param("categoriaId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A imagem deve ter no máximo 5MB.")));
    }
}
