package br.com.sweetbites.config;

import br.com.sweetbites.service.UsuarioService;
import org.springframework.context.annotation.Bean;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
public class SecurityConfig {

    private static final RequestMatcher API = PathPatternRequestMatcher.withDefaults().matcher("/api/**");
    private static final LoginUrlAuthenticationEntryPoint TELA_DE_LOGIN = new LoginUrlAuthenticationEntryPoint("/login");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioService usuarioService) {
        return email -> usuarioService.buscarPorEmail(email)
                .map(UsuarioLogado::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioService usuarioService,
                                                   SessionRegistry sessionRegistry) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/cadastro", "/recuperar-senha", "/redefinir-senha",
                        "/css/**", "/js/**", "/img/**", "/favicon.svg", "/actuator/health", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("senha")
                .successHandler((request, response, authentication) -> {
                    usuarioService.registrarLoginComSucesso(authentication.getName());
                    boolean admin = authentication.getAuthorities().stream()
                            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                    response.sendRedirect(request.getContextPath() + (admin ? "/admin" : "/"));
                })
                .failureHandler((request, response, exception) -> {
                    String erro = "credenciais";
                    if (exception instanceof LockedException) {
                        erro = "bloqueada";
                    } else if (exception instanceof BadCredentialsException
                            && usuarioService.registrarFalhaLogin(request.getParameter("email"))) {
                        erro = "bloqueada";
                    }
                    response.sendRedirect(request.getContextPath() + "/login?erro=" + erro);
                }))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?saiu")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            .sessionManagement(session -> session
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry)
                .expiredUrl("/login?expirada"))
            // Sessão expirada (token CSRF inválido) leva ao login com aviso, em vez de "Acesso negado".
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, negado) -> {
                    if (negado instanceof CsrfException) {
                        response.sendRedirect(request.getContextPath() + "/login?expirada");
                    } else {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    }
                })
                // A API usada pelo JavaScript responde 401; as páginas continuam indo para o login.
                .authenticationEntryPoint((request, response, falha) -> {
                    if (API.matches(request)) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    } else {
                        TELA_DE_LOGIN.commence(request, response, falha);
                    }
                }))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' data:; style-src 'self' https://fonts.googleapis.com; "
                        + "font-src 'self' https://fonts.gstatic.com; script-src 'self'; frame-ancestors 'none'")));
        return http.build();
    }
}
