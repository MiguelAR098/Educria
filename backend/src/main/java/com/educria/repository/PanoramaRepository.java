package com.educria.repository;

import com.educria.domain.entity.RespostaAluno;
import com.educria.dto.RespostaNivel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Consultas de histórico de uma turma, restritas aos assuntos de uma matéria. */
public interface PanoramaRepository extends JpaRepository<RespostaAluno, Long> {

    interface PontosDoAluno {
        Long getAlunoId();
        Long getPontos();
    }

    interface AtividadeDoAluno {
        Long getAlunoId();
        String getDataIso();

        default LocalDateTime data() {
            return LocalDateTime.parse(getDataIso());
        }
    }

    /** Soma pontos individuais já registrados nas respostas de sessão, agrupados por aluno da turma. */
    @Query("""
            select r.aluno.id as alunoId, sum(cast(r.pontosIndividuais as Long)) as pontos
            from RespostaAluno r join r.sessao s
            where r.aluno.turma.id = :turmaId and s.assunto.materia.id = :materiaId
              and r.pontosIndividuais is not null
            group by r.aluno.id
            """)
    List<PontosDoAluno> pontosDaTurma(@Param("turmaId") Long turmaId, @Param("materiaId") Long materiaId);

    /** A sessão preserva o assunto estudado. Para respostas legadas sem sessão, usa-se o assunto atual da questão. */
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

    // A projeção ISO conserva a hora local do datetime2 sem conversão para o fuso da JVM.
    @Query(value = """
            select r.aluno_id as alunoId, convert(varchar(27), max(r.respondido_em), 126) as dataIso
            from dbo.respostas_aluno r
            join dbo.alunos al on al.usuario_id = r.aluno_id
            join dbo.questoes q on q.id = r.questao_id
            left join dbo.sessoes_estudo s on s.id = r.sessao_id
            join dbo.assuntos a on a.id = coalesce(s.assunto_id, q.assunto_id)
            where al.turma_id = :turmaId and a.materia_id = :materiaId
              and (:alunoId is null or al.usuario_id = :alunoId)
            group by r.aluno_id
            """, nativeQuery = true)
    List<AtividadeDoAluno> ultimasAtividadesNaMateria(@Param("turmaId") Long turmaId, @Param("materiaId") Long materiaId,
                                                     @Param("alunoId") Long alunoId);

    /** Cada linha: {id do aluno, quantidade de revisões de questões da matéria já vencidas}. */
    @Query("""
            select rep.aluno.id, count(rep)
            from RepeticaoEspacada rep join rep.questao q
            where q.ativa = true and q.atividade.ativa = true
              and rep.proximaRevisao <= :hoje
              and rep.aluno.turma.id = :turmaId
              and q.assunto.materia.id = :materiaId
            group by rep.aluno.id
            """)
    List<Object[]> revisoesVencidas(@Param("turmaId") Long turmaId, @Param("materiaId") Long materiaId,
                                    @Param("hoje") LocalDate hoje);
}
