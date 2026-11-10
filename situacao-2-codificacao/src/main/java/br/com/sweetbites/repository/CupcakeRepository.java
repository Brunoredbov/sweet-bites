package br.com.sweetbites.repository;

import br.com.sweetbites.model.Cupcake;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CupcakeRepository extends JpaRepository<Cupcake, Long> {

    /**
     * Vitrine (US-04/US-05): cupcakes ativos, filtrados opcionalmente por categoria e por
     * termo de busca no nome ou nos ingredientes (sem diferenciar maiúsculas/minúsculas).
     * Parâmetros nunca nulos: sem filtro, o service passa todasCategorias=true e termo "%".
     */
    @Query("""
            SELECT c FROM Cupcake c JOIN FETCH c.categoria cat
            WHERE c.ativo = true
              AND (:todasCategorias = true OR cat.id = :categoriaId)
              AND (LOWER(c.nome) LIKE :termo OR LOWER(c.ingredientes) LIKE :termo)
            ORDER BY c.nome
            """)
    List<Cupcake> buscarVitrine(@Param("todasCategorias") boolean todasCategorias,
                                @Param("categoriaId") long categoriaId,
                                @Param("termo") String termoLike);

    @Query("SELECT c FROM Cupcake c JOIN FETCH c.categoria WHERE c.id = :id AND c.ativo = true")
    Optional<Cupcake> findAtivoById(@Param("id") Long id);

    @Query("SELECT c FROM Cupcake c JOIN FETCH c.categoria WHERE c.id IN :ids AND c.ativo = true")
    List<Cupcake> findAtivosByIdIn(@Param("ids") Collection<Long> ids);

    /** Lock pessimista para baixar estoque sem vender o mesmo cupcake duas vezes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cupcake c WHERE c.id IN :ids ORDER BY c.id")
    List<Cupcake> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    @Query("SELECT c FROM Cupcake c JOIN FETCH c.categoria WHERE c.ativo = true ORDER BY c.nome")
    List<Cupcake> findAtivosComCategoria();

    @Query("SELECT c FROM Cupcake c JOIN FETCH c.categoria WHERE c.ativo = true AND c.estoque < :limite ORDER BY c.estoque, c.nome")
    List<Cupcake> findEstoqueBaixo(@Param("limite") int limite);

    long countByCategoriaId(Long categoriaId);
}
