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

    interface PontosDoAluno {
        Long getAlunoId();
        Long getPontos();
    }

    /** Soma somente pontos individuais registrados no assunto histórico da sessão. */
    @Query(value = """
            select r.aluno_id as alunoId, sum(cast(r.pontos_individuais as bigint)) as pontos
            from dbo.respostas_aluno r
            join dbo.alunos aluno on aluno.usuario_id = r.aluno_id
            join dbo.sessoes_estudo sessao on sessao.id = r.sessao_id
            join dbo.assuntos assunto on assunto.id = sessao.assunto_id
            where aluno.turma_id = :turmaId and assunto.materia_id = :materiaId
              and r.pontos_individuais is not null
            group by r.aluno_id
            """, nativeQuery = true)
    List<PontosDoAluno> pontosDaTurma(@Param("turmaId") Long turmaId, @Param("materiaId") Long materiaId);

    /** Usa o assunto da sessão ou o da questão para respostas legadas sem sessão. */
    @Query("""
            select new com.educria.dto.RespostaNivel(r.aluno.id, a.id, a.nome, r.nivelAnterior, r.nivelAtual,
                                                     r.correta, r.respondidoEm)
            from RespostaAluno r
            join r.questao q
            left join r.sessao s
            join Assunto a on a.id = coalesce(s.assunto.id, q.assunto.id)
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
