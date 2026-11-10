package br.com.sweetbites.repository;

import br.com.sweetbites.model.TokenRecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenha, Long> {

    @Query("SELECT t FROM TokenRecuperacaoSenha t JOIN FETCH t.usuario WHERE t.tokenHash = :hash")
    Optional<TokenRecuperacaoSenha> findByTokenHash(@Param("hash") String tokenHash);
}
