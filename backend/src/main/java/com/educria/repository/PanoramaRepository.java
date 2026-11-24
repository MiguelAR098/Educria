package com.educria.repository;

import com.educria.domain.entity.RespostaAluno;
import com.educria.dto.RespostaNivel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/** Consultas de histórico de uma turma, restritas aos assuntos de uma matéria. */
public interface PanoramaRepository extends JpaRepository<RespostaAluno, Long> {

    @Query("""
            select new com.educria.dto.RespostaNivel(r.aluno.id, a.id, a.nome, r.nivelAnterior, r.nivelAtual,
                                                     r.correta, r.respondidoEm)
            from RespostaAluno r
            join r.questao q
            join q.assunto a
            where r.aluno.turma.id = :turmaId and a.materia.id = :materiaId
            order by r.respondidoEm, r.id
            """)
    List<RespostaNivel> respostasDaTurma(@Param("turmaId") Long turmaId, @Param("materiaId") Long materiaId);

    /** Cada linha: {id do aluno, quantidade de revisões de questões da matéria já vencidas}. */
    @Query("""
            select rep.aluno.id, count(rep)
            from RepeticaoEspacada rep
            where rep.questao is not null
              and rep.proximaRevisao <= :hoje
              and rep.aluno.turma.id = :turmaId
              and rep.questao.assunto.materia.id = :materiaId
            group by rep.aluno.id
            """)
    List<Object[]> revisoesVencidas(@Param("turmaId") Long turmaId, @Param("materiaId") Long materiaId,
                                    @Param("hoje") LocalDate hoje);
}
