package com.educria.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EvolucaoAprendizagemRepository {
    private final JdbcClient jdbc;

    // Primeira tentativa é avaliada pelo passo na sessão, inclusive antes do período.
    // Respostas legadas sem sessão entram nas contagens, mas ficam sem classificação de tentativa.
    private static final String AGREGAR = """
            with eventos as (
                select s.assunto_id, null::date as dia, null::bigint as questao_id, null::boolean as correta,
                       null::boolean as primeira, null::integer as nivel_anterior, null::integer as nivel_atual,
                       null::timestamp as respondido_em, null::bigint as resposta_id
                from sessoes_estudo s
                where s.aluno_id = :alunoId and s.iniciada_em >= :inicioSessao and s.iniciada_em < :fimSessao
                  and (:assuntoId is null or s.assunto_id = :assuntoId)
                union all
                select coalesce(s.assunto_id, q.assunto_id), r.respondido_em::date, r.questao_id, r.correta,
                       case when s.id is not null then not exists (
                           select 1 from respostas_aluno anterior
                           where anterior.sessao_id = r.sessao_id and anterior.questao_id = r.questao_id
                             and anterior.aluno_id = r.aluno_id and anterior.passo < r.passo
                       ) end,
                       r.nivel_anterior, r.nivel_atual, r.respondido_em, r.id
                from respostas_aluno r
                left join sessoes_estudo s on s.id = r.sessao_id and s.aluno_id = r.aluno_id
                left join questoes q on r.sessao_id is null and q.id = r.questao_id
                where r.aluno_id = :alunoId and r.respondido_em >= :inicioResposta and r.respondido_em < :fimResposta
                  and (r.sessao_id is null or s.id is not null)
                  and (:assuntoId is null or coalesce(s.assunto_id, q.assunto_id) = :assuntoId)
            )
            select grouping(a.id, e.dia) = 3 as geral, a.id as assunto_id, a.nome, e.dia,
                   count(*) filter (where e.questao_id is null) as sessoes_iniciadas,
                   count(e.questao_id) as tentativas, count(distinct e.questao_id) as questoes_unicas,
                   count(*) filter (where e.correta) as acertos, count(*) filter (where not e.correta) as erros,
                   count(*) filter (where not e.primeira) as tentativas_reaprendizagem,
                   count(*) filter (where e.primeira) as primeiras_tentativas,
                   count(*) filter (where e.primeira and e.correta) as primeiras_corretas,
                   (array_agg(e.nivel_anterior order by e.respondido_em, e.resposta_id)
                       filter (where e.questao_id is not null))[1] as nivel_inicial_observado,
                   (array_agg(e.nivel_atual order by e.respondido_em desc, e.resposta_id desc)
                       filter (where e.questao_id is not null))[1] as nivel_final_observado
            from eventos e
            left join assuntos a on a.id = e.assunto_id
            group by grouping sets ((), (a.id, a.nome), (e.dia))
            having grouping(e.dia) = 1 or e.dia is not null
            order by a.id, e.dia
            """;

    public List<Linha> agregar(long alunoId, Long assuntoId, LocalDate inicio, LocalDate fimExclusivo, ZoneId zona) {
        return jdbc.sql(AGREGAR)
                .param("alunoId", alunoId)
                .param("assuntoId", assuntoId, Types.BIGINT)
                // A sessão guarda um instante, enquanto a resposta guarda a hora local do módulo do aluno.
                .param("inicioSessao", inicio.atStartOfDay(zona).toOffsetDateTime())
                .param("fimSessao", fimExclusivo.atStartOfDay(zona).toOffsetDateTime())
                .param("inicioResposta", inicio.atStartOfDay())
                .param("fimResposta", fimExclusivo.atStartOfDay())
                .query(Linha.class).list();
    }

    public record Linha(boolean geral, Long assuntoId, String nome, LocalDate dia,
                        long sessoesIniciadas, long tentativas, long questoesUnicas, long acertos, long erros,
                        long tentativasReaprendizagem, long primeirasTentativas, long primeirasCorretas,
                        Integer nivelInicialObservado, Integer nivelFinalObservado) {}
}
