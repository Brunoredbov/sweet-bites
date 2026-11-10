package br.com.sweetbites.repository;

import br.com.sweetbites.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    @Query("SELECT n FROM Notificacao n LEFT JOIN FETCH n.pedido WHERE n.usuario.id = :usuarioId ORDER BY n.dataCriacao DESC, n.id DESC")
    List<Notificacao> findByUsuario(@Param("usuarioId") Long usuarioId);

    long countByUsuarioIdAndLidaFalse(Long usuarioId);

    @Modifying
    @Query("UPDATE Notificacao n SET n.lida = true WHERE n.usuario.id = :usuarioId AND n.lida = false")
    int marcarTodasComoLidas(@Param("usuarioId") Long usuarioId);
}
