package br.com.sweetbites.repository;

import br.com.sweetbites.model.FormaPagamento;
import br.com.sweetbites.model.Pedido;
import br.com.sweetbites.model.StatusPagamento;
import br.com.sweetbites.model.StatusPedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Carrega o pedido com itens, cupcakes, pagamento, entrega e cliente numa query só. */
    @EntityGraph(attributePaths = {"itens", "itens.cupcake", "pagamento", "entrega", "usuario"})
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> findDetalhadoById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"pagamento", "entrega"})
    @Query("SELECT p FROM Pedido p WHERE p.usuario.id = :usuarioId ORDER BY p.dataCriacao DESC")
    List<Pedido> findByUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * Listagem do admin (US-16) por status e período. Parâmetros nunca nulos: o service
     * passa todos os status / um período amplo quando o filtro não é usado (o PostgreSQL
     * não infere o tipo de parâmetro nulo em "? IS NULL").
     */
    @EntityGraph(attributePaths = {"usuario", "pagamento"})
    @Query("""
            SELECT p FROM Pedido p
            WHERE p.status IN :status
              AND p.dataCriacao >= :inicio
              AND p.dataCriacao < :fim
            ORDER BY p.dataCriacao DESC
            """)
    List<Pedido> buscarAdmin(@Param("status") Collection<StatusPedido> status,
                             @Param("inicio") LocalDateTime inicio,
                             @Param("fim") LocalDateTime fim);

    @Query("""
            SELECT p.id FROM Pedido p JOIN p.pagamento pg
            WHERE p.status = :status AND pg.metodo = :metodo AND pg.status = :statusPagamento
              AND pg.pixExpiraEm < :agora
            """)
    List<Long> findIdsPixExpirados(@Param("status") StatusPedido status,
                                   @Param("metodo") FormaPagamento metodo,
                                   @Param("statusPagamento") StatusPagamento statusPagamento,
                                   @Param("agora") LocalDateTime agora);

    long countByStatus(StatusPedido status);

    /** RN (US-14): cupcake em pedido ativo não pode ser removido. */
    @Query("""
            SELECT COUNT(i) > 0 FROM ItemPedido i
            WHERE i.cupcake.id = :cupcakeId AND i.pedido.status IN :statusAtivos
            """)
    boolean existeCupcakeEmPedidoAtivo(@Param("cupcakeId") Long cupcakeId,
                                       @Param("statusAtivos") Collection<StatusPedido> statusAtivos);
}
