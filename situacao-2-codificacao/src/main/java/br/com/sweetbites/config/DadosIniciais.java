package br.com.sweetbites.config;

import br.com.sweetbites.model.*;
import br.com.sweetbites.repository.CategoriaRepository;
import br.com.sweetbites.repository.CupcakeRepository;
import br.com.sweetbites.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Popula o cardápio inicial (do mockup do PIT I) quando o banco está vazio e cria o
 * administrador a partir das variáveis ADMIN_EMAIL / ADMIN_SENHA (nunca com senha fixa no código).
 */
@Component
public class DadosIniciais implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    private final CategoriaRepository categoriaRepository;
    private final CupcakeRepository cupcakeRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminSenha;

    public DadosIniciais(CategoriaRepository categoriaRepository, CupcakeRepository cupcakeRepository,
                         UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                         @Value("${loja.admin.email:}") String adminEmail,
                         @Value("${loja.admin.senha:}") String adminSenha) {
        this.categoriaRepository = categoriaRepository;
        this.cupcakeRepository = cupcakeRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminSenha = adminSenha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        criarAdmin();
        if (categoriaRepository.count() == 0) {
            criarCardapio();
        }
    }

    private void criarAdmin() {
        if (adminEmail.isBlank() || adminSenha.isBlank()) {
            log.warn("[dados-iniciais] ADMIN_EMAIL/ADMIN_SENHA não definidos: nenhum administrador criado.");
            return;
        }
        String email = adminEmail.trim().toLowerCase();
        if (!usuarioRepository.existsByEmailIgnoreCase(email)) {
            usuarioRepository.save(new Usuario("Administrador Sweet Bites", email,
                    passwordEncoder.encode(adminSenha), "(11) 90000-0000", Papel.ADMIN));
            log.info("[dados-iniciais] administrador criado");
        }
    }

    private void criarCardapio() {
        Map<String, Categoria> cat = new HashMap<>();
        for (String[] c : new String[][]{
                {"Chocolate", "Para quem ama cacau"},
                {"Baunilha", "Clássicos e delicados"},
                {"Red Velvet", "A massa aveludada mais pedida"},
                {"Frutas", "Leves, com frutas frescas"},
                {"Especiais", "Edições da casa"}}) {
            cat.put(c[0], categoriaRepository.save(new Categoria(c[0], c[1])));
        }
        // nome, categoria, preço, estoque, descrição, ingredientes, cor da massa, cor da cobertura, cor do topo
        Object[][] cupcakes = {
            {"Chocolate Belga", "Chocolate", "16.90", 30,
             "Massa de chocolate 70% cacau com cobertura cremosa de ganache belga.",
             "chocolate belga, cacau, farinha, ovos, manteiga, açúcar, creme de leite", "#6b3e26", "#4a2616", "#d9a066"},
            {"Brigadeiro Gourmet", "Chocolate", "15.90", 25,
             "Recheio de brigadeiro cremoso e granulado belga por cima.",
             "chocolate, leite condensado, granulado belga, farinha, ovos, manteiga", "#5a3220", "#7b4a2e", "#3a1f12"},
            {"Baunilha Sonho", "Baunilha", "14.90", 30,
             "Massa de baunilha de Madagascar com buttercream suave.",
             "baunilha, farinha, ovos, manteiga, açúcar, leite", "#f3d9a4", "#fff3d6", "#ff6b8a"},
            {"Doce de Leite", "Baunilha", "15.50", 20,
             "Massa de baunilha recheada com doce de leite artesanal.",
             "doce de leite, baunilha, farinha, ovos, manteiga, açúcar", "#f0cf8f", "#d9a066", "#fff3d6"},
            {"Red Velvet Divine", "Red Velvet", "14.90", 30,
             "Massa red velvet fofinha com frosting de cream cheese e confeitos.",
             "cacau, cream cheese, farinha, ovos, manteiga, açúcar, corante natural de beterraba", "#b3283c", "#fff7ef", "#e0304d"},
            {"Red Velvet Morango", "Red Velvet", "16.50", 4,
             "Red velvet com cream cheese e morangos frescos.",
             "cacau, cream cheese, morango, farinha, ovos, manteiga, açúcar", "#a8243a", "#ffe0e6", "#e53950"},
            {"Morango Festa", "Frutas", "15.50", 25,
             "Massa de baunilha com recheio e cobertura de morango.",
             "morango, baunilha, farinha, ovos, manteiga, açúcar, chantilly", "#f3d9a4", "#ff9fb2", "#e53950"},
            {"Limão Siciliano", "Frutas", "14.50", 18,
             "Massa cítrica com merengue maçaricado.",
             "limão siciliano, merengue, farinha, ovos, manteiga, açúcar", "#f6e7a1", "#fffbea", "#f2d33b"},
            {"Pistache", "Especiais", "18.90", 12,
             "Massa de pistache com creme de pistache e pistaches picados.",
             "pistache, farinha, ovos, manteiga, açúcar, chocolate branco", "#c8d79a", "#b5cf7a", "#7a9a3a"},
            {"Nutella com Avelã", "Especiais", "18.50", 0,
             "Massa de chocolate recheada com Nutella e avelãs tostadas.",
             "creme de avelã, avelã, cacau, farinha, ovos, manteiga, açúcar", "#5a3220", "#8a5a3c", "#c9925a"},
        };
        for (Object[] d : cupcakes) {
            Cupcake c = new Cupcake((String) d[0], (String) d[4], (String) d[5], new BigDecimal((String) d[2]),
                    (Integer) d[3], cat.get((String) d[1]));
            c.setImagem(new ImagemProduto("image/svg+xml",
                    svgCupcake((String) d[6], (String) d[7], (String) d[8]).getBytes(StandardCharsets.UTF_8)));
            cupcakeRepository.save(c);
        }
        log.info("[dados-iniciais] cardápio inicial criado: {} cupcakes", cupcakes.length);
    }

    /** Ilustração vetorial de cupcake usada no cardápio de exemplo (o admin troca por fotos reais). */
    static String svgCupcake(String massa, String cobertura, String topo) {
        return """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
              <rect width="200" height="200" fill="#fff0f3"/>
              <path d="M52 112 L148 112 L136 176 Q100 184 64 176 Z" fill="%1$s"/>
              <path d="M66 114 L72 176 M84 114 L87 178 M100 114 L100 179 M116 114 L113 178 M134 114 L128 176"
                    stroke="#00000022" stroke-width="3"/>
              <path d="M44 114 Q40 88 64 82 Q62 58 90 58 Q100 40 114 56 Q142 56 138 82 Q162 90 156 114 Z" fill="%2$s"/>
              <path d="M58 100 Q80 92 100 100 Q122 108 144 98" stroke="#ffffff66" stroke-width="5" fill="none" stroke-linecap="round"/>
              <circle cx="104" cy="44" r="12" fill="%3$s"/>
              <circle cx="72" cy="90" r="3" fill="%3$s"/><circle cx="128" cy="86" r="3" fill="%3$s"/>
              <circle cx="96" cy="76" r="3" fill="%3$s"/><circle cx="116" cy="102" r="3" fill="%3$s"/>
            </svg>
            """.formatted(massa, cobertura, topo);
    }
}
