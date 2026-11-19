package com.educria.repository;

import com.educria.domain.entity.Questao;
import com.educria.dto.QuestaoParaAluno;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestaoRepository extends Repository<Questao, Long> {

    @Query("""
            select new com.educria.dto.QuestaoParaAluno(q.id, q.assunto.id, q.dificuldade,
                q.enunciado, q.alternativaA, q.alternativaB, q.alternativaC, q.alternativaD)
            from Questao q
            where q.assunto.id = :assuntoId and q.atividade.ativa = true
              and q.dificuldade is not null
            """)
    List<QuestaoParaAluno> listarParaSelecao(@Param("assuntoId") Long assuntoId);
}
