package br.com.sweetbites.web;

import br.com.sweetbites.dto.EnderecoCep;
import br.com.sweetbites.repository.CupcakeRepository;
import br.com.sweetbites.service.CepService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração: aplicação inteira (controllers MVC + Spring Security + JPA + Flyway)
 * sobre H2. Só a API externa de CEP é substituída por um dublê.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FluxosWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CupcakeRepository cupcakeRepository;

    @MockitoBean
    private CepService cepService;

    @BeforeEach
    void cepDeSaoPaulo() {
        when(cepService.consultar(anyString())).thenReturn(Optional.of(
                new EnderecoCep("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP")));
    }

    private void cadastrar(String email, String senha) throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Cliente Teste").param("email", email).param("telefone", "(11) 91234-5678")
                        .param("senha", senha).param("confirmarSenha", senha))
                .andExpect(redirectedUrl("/login"));
    }

    private MockHttpSession entrar(String email, String senha) throws Exception {
        MvcResult r = mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha").user(email).password(senha))
                .andReturn();
        assertThat(r.getResponse().getRedirectedUrl()).as("login de %s", email).doesNotContain("erro");
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    @Test
    void visitanteEhRedirecionadoParaOLogin() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(containsString("Sweet Bites")));
    }

    @Test
    void cadastroValidaSenhaEEmailUnico() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Ana").param("email", "ana-fraca@teste.local").param("telefone", "(11) 91234-5678")
                        .param("senha", "curta").param("confirmarSenha", "curta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no mínimo 8 caracteres")));

        cadastrar("ana@teste.local", "Senha1234");
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Ana 2").param("email", "ANA@teste.local").param("telefone", "(11) 91234-5678")
                        .param("senha", "Senha1234").param("confirmarSenha", "Senha1234"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("já está cadastrado")));
    }

    @Test
    void contaBloqueadaAposCincoTentativasErradas() throws Exception {
        cadastrar("bloqueio@teste.local", "Senha1234");
        for (int i = 1; i <= 4; i++) {
            mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha").user("bloqueio@teste.local").password("errada"))
                    .andExpect(redirectedUrl("/login?erro=credenciais"));
        }
        mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha").user("bloqueio@teste.local").password("errada"))
                .andExpect(redirectedUrl("/login?erro=bloqueada"));
        mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha").user("bloqueio@teste.local").password("Senha1234"))
                .andExpect(redirectedUrl("/login?erro=bloqueada"));
    }

    @Test
    void recuperarSenhaDesbloqueiaEPermiteNovoLogin() throws Exception {
        cadastrar("esqueci@teste.local", "Senha1234");
        MvcResult r = mvc.perform(post("/recuperar-senha").with(csrf()).param("email", "esqueci@teste.local"))
                .andExpect(status().isOk()).andReturn();
        String token = (String) r.getModelAndView().getModel().get("tokenDemonstracao");
        assertThat(token).as("modo demonstração exibe o link").isNotBlank();

        mvc.perform(post("/redefinir-senha").with(csrf())
                        .param("token", token).param("senha", "NovaSenha99").param("confirmarSenha", "NovaSenha99"))
                .andExpect(redirectedUrl("/login"));
        entrar("esqueci@teste.local", "NovaSenha99");

        mvc.perform(post("/redefinir-senha").with(csrf())
                        .param("token", token).param("senha", "OutraSenha1").param("confirmarSenha", "OutraSenha1"))
                .andExpect(content().string(containsString("inválido ou expirado")));
    }

    @Test
    void emailInexistenteRecebeAMesmaRespostaNaRecuperacao() throws Exception {
        mvc.perform(post("/recuperar-senha").with(csrf()).param("email", "ninguem@teste.local"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Se houver uma conta com esse e-mail")))
                .andExpect(content().string(not(containsString("token="))));
    }

    @Test
    void clienteNaoAcessaPainelAdmin() throws Exception {
        cadastrar("curioso@teste.local", "Senha1234");
        MockHttpSession cliente = entrar("curioso@teste.local", "Senha1234");
        mvc.perform(get("/admin").session(cliente)).andExpect(status().isForbidden());

        MockHttpSession admin = entrar("admin@teste.local", "AdminTeste123");
        mvc.perform(get("/admin").session(admin)).andExpect(status().isOk());
        mvc.perform(get("/admin/pedidos").session(admin).param("status", "EM_PREPARACAO").param("de", "2026-01-01"))
                .andExpect(status().isOk());
    }

    @Test
    void buscaNaVitrineIgnoraMaiusculasEProcuraIngredientes() throws Exception {
        cadastrar("busca@teste.local", "Senha1234");
        MockHttpSession s = entrar("busca@teste.local", "Senha1234");
        mvc.perform(get("/vitrine/grid").session(s).param("busca", "MORANGO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Morango Festa")))
                .andExpect(content().string(containsString("Red Velvet Morango")))
                .andExpect(content().string(not(containsString("Pistache"))));
        mvc.perform(get("/vitrine/grid").session(s).param("busca", "xyz-inexistente"))
                .andExpect(content().string(containsString("Produto não encontrado")));
        mvc.perform(get("/").session(s))
                .andExpect(content().string(containsString("Indisponível")));
    }

    @Test
    void pedidoCompletoComPixEOwnership() throws Exception {
        cadastrar("compra@teste.local", "Senha1234");
        MockHttpSession comprador = entrar("compra@teste.local", "Senha1234");
        Long chocolateId = cupcakeRepository.findAtivosComCategoria().stream()
                .filter(c -> c.getNome().equals("Chocolate Belga")).findFirst().orElseThrow().getId();
        int estoqueAntes = cupcakeRepository.findById(chocolateId).orElseThrow().getEstoque();

        MvcResult r = mvc.perform(post("/pedidos").session(comprador).with(csrf())
                        .param("itens", chocolateId + ":2").param("cep", "01310-100").param("numero", "1000")
                        .param("formaPagamento", "PIX"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/pedidos/*?confirmado"))
                .andReturn();
        String urlPedido = r.getResponse().getRedirectedUrl().replace("?confirmado", "");
        assertThat(cupcakeRepository.findById(chocolateId).orElseThrow().getEstoque()).isEqualTo(estoqueAntes - 2);

        mvc.perform(get(urlPedido).session(comprador))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pague com PIX")));
        mvc.perform(get(urlPedido + "/pix.png").session(comprador))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"));

        cadastrar("intruso@teste.local", "Senha1234");
        MockHttpSession intruso = entrar("intruso@teste.local", "Senha1234");
        mvc.perform(get(urlPedido).session(intruso)).andExpect(status().isNotFound());
        mvc.perform(post(urlPedido + "/cancelar").session(intruso).with(csrf())).andExpect(status().isNotFound());

        mvc.perform(post(urlPedido + "/pix/simular-pagamento").session(comprador).with(csrf()))
                .andExpect(redirectedUrl(urlPedido));
        mvc.perform(get(urlPedido).session(comprador))
                .andExpect(content().string(containsString("Em preparação")));
    }

    @Test
    void checkoutRejeitaPedidoAbaixoDoMinimoMostrandoAMensagem() throws Exception {
        cadastrar("minimo@teste.local", "Senha1234");
        MockHttpSession s = entrar("minimo@teste.local", "Senha1234");
        Long limaoId = cupcakeRepository.findAtivosComCategoria().stream()
                .filter(c -> c.getNome().equals("Limão Siciliano")).findFirst().orElseThrow().getId();
        mvc.perform(post("/pedidos").session(s).with(csrf())
                        .param("itens", limaoId + ":1").param("cep", "01310-100").param("numero", "10")
                        .param("formaPagamento", "DINHEIRO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("pedido mínimo é de R$ 15,00")));
    }

    @Test
    void apiDeFreteDevolveErroAmigavelParaCepInvalido() throws Exception {
        cadastrar("frete@teste.local", "Senha1234");
        MockHttpSession s = entrar("frete@teste.local", "Senha1234");
        when(cepService.consultar("99999999")).thenReturn(Optional.empty());
        mvc.perform(get("/api/frete").session(s).param("cep", "99999-999"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erro").value("CEP não encontrado. Verifique o endereço."));
        mvc.perform(get("/api/frete").session(s).param("cep", "01310-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(7.0))
                .andExpect(jsonPath("$.tempoEstimadoMin").value(45));
    }
}
