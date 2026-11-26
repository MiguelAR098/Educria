package com.educria.repository;

import com.educria.domain.entity.GrupoPedagogico;
import com.educria.domain.enums.TipoGrupoPedagogico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Recupera grupos do autor e os contextos de turma usados nas verificações do serviço. */
public interface GrupoPedagogicoRepository extends JpaRepository<GrupoPedagogico, Long> {
    @Query("""
            select g from GrupoPedagogico g join fetch g.turmas
            where g.professor.id = :professorId
              and (:tipo is null or g.tipo = :tipo)
              and (:arquivado is null or (:arquivado = true and g.arquivadoEm is not null)
                   or (:arquivado = false and g.arquivadoEm is null))
            order by g.id
            """)
    List<GrupoPedagogico> listarDoProfessor(@Param("professorId") Long professorId,
                                          @Param("tipo") TipoGrupoPedagogico tipo,
                                          @Param("arquivado") Boolean arquivado);

    Optional<GrupoPedagogico> findByIdAndProfessorId(Long id, Long professorId);

    /** Usa bloqueio de atualização na linha do grupo, até o fim da transação que altera o contexto ou seus vínculos. */
    @Query(value = """
            select g.* from dbo.grupos_pedagogicos g with (UPDLOCK, ROWLOCK)
            where g.id = :id and g.professor_id = :professorId
            """, nativeQuery = true)
    Optional<GrupoPedagogico> bloquearDoProfessor(@Param("id") Long id, @Param("professorId") Long professorId);
}
