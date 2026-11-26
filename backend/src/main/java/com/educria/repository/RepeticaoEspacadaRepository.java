package com.educria.repository;

import com.educria.domain.entity.RepeticaoEspacada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.educria.dto.RevisaoDoAluno;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Consulta a agenda por questão e mantém consultas por atividade para registros legados. */
public interface RepeticaoEspacadaRepository extends JpaRepository<RepeticaoEspacada, Long> {

    interface DataQuestao {
        Long getQuestaoId();
        LocalDate getData();
    }

    Optional<RepeticaoEspacada> findByAlunoIdAndQuestaoId(Long alunoId, Long questaoId);

    /** Carrega as datas de um assunto em lote para que a seleção avalie a disponibilidade de cada questão. */
    @Query("""
            select r.questao.id as questaoId, r.proximaRevisao as data from RepeticaoEspacada r
            where r.aluno.id = :alunoId and r.questao.assunto.id = :assuntoId
            """)
    List<DataQuestao> datasPorAssunto(Long alunoId, Long assuntoId);

    @Query("""
            select new com.educria.dto.RevisaoDoAluno(q.id, q.enunciado, q.assunto.nome, r.proximaRevisao, r.intervaloAtual)
            from RepeticaoEspacada r join r.questao q
            where r.aluno.id = :alunoId and q.atividade.ativa = true and q.ativa = true and q.dificuldade is not null
            order by r.proximaRevisao, q.id
            """)
    List<RevisaoDoAluno> agenda(Long alunoId);

    Optional<RepeticaoEspacada> findByAlunoIdAndAtividadeId(Long alunoId, Long atividadeId);

    @Query("SELECT r FROM RepeticaoEspacada r WHERE r.aluno.id = :alunoId AND r.atividade is not null AND r.proximaRevisao <= :hoje")
    List<RepeticaoEspacada> findPendentesByAluno(Long alunoId, LocalDate hoje);

    @Query("select r from RepeticaoEspacada r where r.aluno.id = :alunoId and r.atividade is not null")
    List<RepeticaoEspacada> findByAlunoId(Long alunoId);
}
