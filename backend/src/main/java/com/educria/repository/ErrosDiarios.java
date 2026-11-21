package com.educria.repository;

final class ErrosDiarios {
    private ErrosDiarios() {}

    // r é uma resposta do titular. respondido_em é a hora local gravada sem fuso.
    // A busca pelo evento anterior independe dos filtros de sessão, assunto e paginação.
    static final String PRIMEIRO_ERRO = """
            (not r.correta and not exists (
                select 1 from respostas_aluno anterior
                where anterior.aluno_id = r.aluno_id and anterior.questao_id = r.questao_id
                  and not anterior.correta
                  and anterior.respondido_em >= date_trunc('day', r.respondido_em)
                  and (anterior.respondido_em, anterior.id) < (r.respondido_em, r.id)
                  and (anterior.sessao_id is null or exists (
                      select 1 from sessoes_estudo titular
                      where titular.id = anterior.sessao_id and titular.aluno_id = anterior.aluno_id
                  ))
            ))
            """;
}
