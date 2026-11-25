package com.educria.repository;

import com.educria.domain.enums.SituacaoSessao;
import com.educria.dto.ResultadoSessao.Indicadores;
import com.educria.dto.ResultadoSessao.Item;
import com.educria.dto.ResultadoSessao.Revisao;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HistoricoSessaoRepository {
    private final JdbcClient jdbc;

    public record PontuacaoAcumulada(long noAssunto, long naMateria) {}

    private static final String ALVO = """
            with alvo as (
                select s.id from sessoes_estudo s join usuarios u on u.id = s.aluno_id
                where u.email = :email
            """;
    // As janelas escolhem a resposta inteira, preservando níveis legados nulos nos extremos.
    private static final String INDICADORES = """
            ), tentativas_alvo as (
                select r.id, r.sessao_id, r.questao_id, cast(r.correta as integer) as correta,
                       r.nivel_anterior, r.nivel_atual, r.pontos_individuais,
                       row_number() over (partition by r.sessao_id order by r.passo) as primeira,
                       row_number() over (partition by r.sessao_id order by r.passo desc) as ultima,
                       case when
            """ + ErrosDiarios.PRIMEIRO_ERRO + """
                       then 1 else 0 end as erro
                from respostas_aluno r
                join alvo p on p.id = r.sessao_id
                join sessoes_estudo titular on titular.id = r.sessao_id and titular.aluno_id = r.aluno_id
                join sessao_questoes sq on sq.sessao_id = r.sessao_id and sq.questao_id = r.questao_id
            )
            select s.id, a.nome as assunto, s.situacao, s.iniciada_em, count(distinct sq.questao_id) as apresentadas,
                   count(distinct r.questao_id) as respondidas, count(r.id) as tentativas,
                   count(case when r.correta = 1 then 1 end) as acertos,
                   count(case when r.correta = 0 then 1 end) as tentativas_incorretas,
                   count(case when r.erro = 1 then 1 end) as erros,
                   max(case when r.primeira = 1 then r.nivel_anterior end) as antes,
                   max(case when r.ultima = 1 then r.nivel_atual end) as depois,
                   sum(cast(r.pontos_individuais as bigint)) as pontos_individuais,
                   case when count(r.pontos_individuais) > 0 and count(r.pontos_individuais) = count(r.id)
                        then 1 else 0 end as pontuacao_completa
            from sessoes_estudo s
            join alvo p on p.id = s.id
            join assuntos a on a.id = s.assunto_id
            left join sessao_questoes sq on sq.sessao_id = s.id
            left join tentativas_alvo r on r.sessao_id = s.id and r.questao_id = sq.questao_id
            group by s.id, a.nome, s.situacao, s.iniciada_em order by s.id desc
            """;
    private static final RowMapper<Indicadores> LER = (rs, i) -> new Indicadores(
            rs.getLong("id"), rs.getString("assunto"), SituacaoSessao.valueOf(rs.getString("situacao")),
            rs.getTimestamp("iniciada_em").toInstant(), rs.getInt("apresentadas"), rs.getInt("respondidas"),
            rs.getLong("tentativas"), rs.getLong("acertos"), rs.getLong("tentativas_incorretas"), rs.getLong("erros"),
            rs.getObject("antes", Integer.class), rs.getObject("depois", Integer.class),
            rs.getObject("pontos_individuais", Long.class), rs.getBoolean("pontuacao_completa"));

    public List<Indicadores> listar(String email, Long antesDe, int limite) {
        String filtro = antesDe == null ? "" : " and s.id < :antesDe";
        var consulta = jdbc.sql(ALVO + filtro + " order by s.id desc offset 0 rows fetch next :limite rows only\n"
                + INDICADORES)
                .param("email", email).param("limite", limite);
        if (antesDe != null) consulta = consulta.param("antesDe", antesDe);
        return consulta.query(LER).list();
    }

    public Optional<Indicadores> obter(String email, long id) {
        return jdbc.sql(ALVO + " and s.id = :id\n" + INDICADORES)
                .param("email", email).param("id", id).query(LER).optional();
    }

    public Optional<Indicadores> anteriorComparavel(String email, long id) {
        return jdbc.sql(ALVO + """
                and s.id = (
                    select candidata.id
                    from sessoes_estudo atual
                    join sessoes_estudo candidata on candidata.aluno_id = atual.aluno_id
                        and candidata.assunto_id = atual.assunto_id and candidata.id < atual.id
                    where atual.id = :id and atual.situacao = 'ENCERRADA' and candidata.situacao = 'ENCERRADA'
                    order by candidata.id desc offset 0 rows fetch next 1 rows only
                )
                """ + INDICADORES).param("email", email).param("id", id).query(LER).optional();
    }

    public PontuacaoAcumulada pontuacaoAcumulada(String email, long id) {
        return jdbc.sql("""
                select coalesce(sum(case when historica.assunto_id = atual.assunto_id
                           then cast(r.pontos_individuais as bigint) end), 0) as no_assunto,
                       coalesce(sum(cast(r.pontos_individuais as bigint)), 0) as na_materia
                from sessoes_estudo atual
                join usuarios u on u.id = atual.aluno_id
                join assuntos alvo on alvo.id = atual.assunto_id
                join respostas_aluno r on r.aluno_id = atual.aluno_id and r.pontos_individuais is not null
                join sessoes_estudo historica on historica.id = r.sessao_id and historica.aluno_id = r.aluno_id
                join assuntos a on a.id = historica.assunto_id and a.materia_id = alvo.materia_id
                where atual.id = :id and u.email = :email
                """).param("id", id).param("email", email)
                .query((rs, i) -> new PontuacaoAcumulada(rs.getLong("no_assunto"), rs.getLong("na_materia")))
                .single();
    }

    public List<Item> questoes(String email, long id) {
        return jdbc.sql("""
                select sq.indice + 1 as numero, q.enunciado, r.resposta_dada as alternativa, r.correta, r.passo
                from sessoes_estudo s
                join usuarios u on u.id = s.aluno_id
                join sessao_questoes sq on sq.sessao_id = s.id
                join questoes q on q.id = sq.questao_id
                left join respostas_aluno r on r.sessao_id = s.id and r.questao_id = q.id and r.aluno_id = s.aluno_id
                where u.email = :email and s.id = :id order by sq.indice, r.passo
                """).param("email", email).param("id", id).query(Item.class).list();
    }

    public List<Revisao> revisoes(String email, long id) {
        return jdbc.sql("""
                select distinct q.id as questao_id, q.enunciado, rep.proxima_revisao
                from sessoes_estudo s
                join usuarios u on u.id = s.aluno_id
                join respostas_aluno r on r.sessao_id = s.id and r.aluno_id = s.aluno_id
                join questoes q on q.id = r.questao_id
                join atividades a on a.id = q.atividade_id
                join repeticao_espacada rep on rep.questao_id = q.id and rep.aluno_id = s.aluno_id
                where u.email = :email and s.id = :id and cast(a.ativa as integer) = 1
                  and q.assunto_id is not null and q.dificuldade is not null
                order by rep.proxima_revisao, q.id
                """).param("email", email).param("id", id).query(Revisao.class).list();
    }
}
