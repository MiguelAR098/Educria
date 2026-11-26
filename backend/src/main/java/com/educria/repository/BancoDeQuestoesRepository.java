package com.educria.repository;

import com.educria.domain.entity.Questao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Consultas do professor sobre as questões da sua matéria (inclui as desativadas). */
public interface BancoDeQuestoesRepository extends JpaRepository<Questao, Long> {

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
            select r.questao.id, count(r), sum(case when r.correta = true then 1 else 0 end)
            from RespostaAluno r
            where r.questao.id in :ids
            group by r.questao.id
            """)
    List<Object[]> estatisticas(@Param("ids") Collection<Long> ids);
}
