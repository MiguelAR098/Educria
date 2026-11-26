package com.educria.repository;

import com.educria.domain.entity.Questao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Consultas do professor sobre as questões da sua matéria (inclui as desativadas). */
public interface BancoDeQuestoesRepository extends JpaRepository<Questao, Long> {

    interface ContagemPorDificuldade {
        Long getAssuntoId();
        Integer getDificuldade();
        long getTotal();
    }

    @Query("""
            select a.id as assuntoId, q.dificuldade as dificuldade, count(q) as total
            from Questao q join q.assunto a
            where a.materia.id = :materiaId and
            """ + QuestaoRepository.QUESTAO_ELEGIVEL + """
            group by a.id, q.dificuldade
            """)
    List<ContagemPorDificuldade> contarElegiveis(@Param("materiaId") Long materiaId);

    @Query("""
            select q from Questao q
            join fetch q.assunto a
            left join fetch q.autor
            where a.materia.id = :materiaId
            order by a.nome, q.dificuldade, q.id
            """)
    List<Questao> daMateria(@Param("materiaId") Long materiaId);

    @Query("""
            select q from Questao q
            join fetch q.assunto a
            left join fetch q.autor
            where a.id = :assuntoId
            order by q.dificuldade, q.id
            """)
    List<Questao> doAssunto(@Param("assuntoId") Long assuntoId);

    @Query("""
            select q from Questao q
            join fetch q.assunto a
            join fetch a.materia
            left join fetch q.autor
            where q.id = :id
            """)
    Optional<Questao> buscar(@Param("id") Long id);

    /** Para cada questão: {id, total de respostas, total de acertos}. */
    @Query("""
            select q.id, count(r), sum(cast(case when r.correta = true then 1 else 0 end as Long))
            from RespostaAluno r
            join r.questao q join q.assunto a
            where a.materia.id = :materiaId
              and (:assuntoId is null or a.id = :assuntoId)
              and (:questaoId is null or q.id = :questaoId)
            group by q.id
            """)
    List<Object[]> estatisticas(@Param("materiaId") Long materiaId, @Param("assuntoId") Long assuntoId,
                                @Param("questaoId") Long questaoId);
}
