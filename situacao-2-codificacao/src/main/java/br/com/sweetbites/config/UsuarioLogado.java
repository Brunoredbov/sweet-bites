package br.com.sweetbites.config;

import br.com.sweetbites.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Principal do Spring Security. Igualdade pelo e-mail, para o SessionRegistry achar todas
 * as sessões do mesmo usuário (necessário para encerrá-las após redefinir a senha).
 */
public class UsuarioLogado implements UserDetails {

    private final Long id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final boolean bloqueado;
    private final List<GrantedAuthority> authorities;

    public UsuarioLogado(Usuario u) {
        this.id = u.getId();
        this.nome = u.getNome();
        this.email = u.getEmail();
        this.senhaHash = u.getSenhaHash();
        this.bloqueado = u.isBloqueado();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + u.getPapel().name()));
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }

    public String getPrimeiroNome() {
        int espaco = nome.indexOf(' ');
        return espaco > 0 ? nome.substring(0, espaco) : nome;
    }

    public boolean isAdmin() {
        return authorities.stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return senhaHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonLocked() { return !bloqueado; }

    @Override
    public boolean equals(Object o) {
        return o instanceof UsuarioLogado outro && email.equals(outro.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
