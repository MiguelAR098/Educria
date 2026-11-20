package com.educria.repository;

import com.educria.domain.entity.Questao;
import com.educria.dto.QuestaoParaAluno;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestaoRepository extends Repository<Questao, Long> {

    // Os mesmos espaços que String.isBlank() rejeita no envio da resposta.
    String ESPACOS_JAVA = " \t\n\013\f\r\u001C\u001D\u001E\u001F\u1680"
            + "\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2008\u2009\u200A\u2028\u2029\u205F\u3000";

    String QUESTAO_ELEGIVEL = """
            q.atividade.ativa = true and q.dificuldade is not null
            and function('translate', case upper(q.respostaCorreta)
                when 'A' then q.alternativaA
                when 'B' then q.alternativaB
                when 'C' then q.alternativaC
                when 'D' then q.alternativaD
                else null
                end, '""" + ESPACOS_JAVA + "', '') <> ''";

    @Query("""
            select new com.educria.dto.QuestaoParaAluno(q.id, q.assunto.id, q.dificuldade,
                q.enunciado, q.alternativaA, q.alternativaB, q.alternativaC, q.alternativaD)
            from Questao q
            where q.assunto.id = :assuntoId and
            """ + QUESTAO_ELEGIVEL)
    List<QuestaoParaAluno> listarParaSelecao(@Param("assuntoId") Long assuntoId);
}
