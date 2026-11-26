package com.educria.repository;

/** Compartilha a regra de primeiro erro diário entre o relatório e o histórico de sessões. */
final class ErrosDiarios {
    private ErrosDiarios() {}

    // r é uma resposta do titular. respondido_em é a hora local gravada sem fuso.
    // A busca pelo evento anterior independe dos filtros de sessão, assunto e paginação.
    // Uma questão gera no máximo um erro contabilizado por aluno e dia. O ID desempata horários iguais.
    static final String PRIMEIRO_ERRO = """
            (r.correta = 0 and not exists (
                select 1 from respostas_aluno anterior
                where anterior.aluno_id = r.aluno_id and anterior.questao_id = r.questao_id
                  and anterior.correta = 0
                  and anterior.respondido_em >= cast(r.respondido_em as date)
                  and (anterior.respondido_em < r.respondido_em
                       or (anterior.respondido_em = r.respondido_em and anterior.id < r.id))
                  and (anterior.sessao_id is null or exists (
                      select 1 from sessoes_estudo titular
                      where titular.id = anterior.sessao_id and titular.aluno_id = anterior.aluno_id
                  ))
            ))
            """;
}
