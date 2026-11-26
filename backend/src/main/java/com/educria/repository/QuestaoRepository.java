package com.educria.repository;

import com.educria.domain.entity.Questao;
import com.educria.dto.QuestaoParaAluno;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestaoRepository extends Repository<Questao, Long> {

    String QUESTAO_ELEGIVEL = """
            q.atividade.ativa = true and q.ativa = true and q.dificuldade is not null
            and function('educria_tamanho_texto', case upper(q.respostaCorreta)
                when 'A' then q.alternativaA
                when 'B' then q.alternativaB
                when 'C' then q.alternativaC
                when 'D' then q.alternativaD
                else null
                end) > 0
            """;

    @Query("""
            select new com.educria.dto.QuestaoParaAluno(q.id, q.assunto.id, q.dificuldade,
                q.enunciado, q.alternativaA, q.alternativaB, q.alternativaC, q.alternativaD)
            from Questao q
            where q.assunto.id = :assuntoId and
            """ + QUESTAO_ELEGIVEL)
    List<QuestaoParaAluno> listarParaSelecao(@Param("assuntoId") Long assuntoId);
}
