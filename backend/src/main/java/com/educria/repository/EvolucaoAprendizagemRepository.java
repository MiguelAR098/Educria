package com.educria.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Agrega eventos de estudo no SQL Server com {@code JdbcClient}, retornando linhas de relatório.
 * {@code GROUPING SETS} obtém totais gerais, por assunto e por dia a partir do mesmo conjunto de eventos.
 */
@Repository
@RequiredArgsConstructor
public class EvolucaoAprendizagemRepository {
    private final JdbcClient jdbc;

    // Primeira tentativa é avaliada pelo passo na sessão, inclusive antes do período.
    // Respostas legadas sem sessão entram nas contagens, mas ficam sem classificação de tentativa.
    // A sessão define o assunto histórico. Apenas a resposta sem sessão usa a classificação atual da questão.
    private static final String AGREGAR = """
            with respostas_periodo as (
                select coalesce(s.assunto_id, q.assunto_id) as assunto_id, cast(r.respondido_em as date) as dia,
                       r.questao_id, cast(r.correta as integer) as correta,
                       case when
            """ + ErrosDiarios.PRIMEIRO_ERRO + """
                       then 1 else 0 end as erro_contabilizado,
                       case when s.id is not null and not exists (
                           select 1 from respostas_aluno anterior
                           where anterior.sessao_id = r.sessao_id and anterior.questao_id = r.questao_id
                             and anterior.aluno_id = r.aluno_id and anterior.passo < r.passo
                       ) then 1 when s.id is not null then 0 end as primeira,
                       r.nivel_anterior, r.nivel_atual,
                       row_number() over (partition by coalesce(s.assunto_id, q.assunto_id)
                           order by r.respondido_em, r.id) as inicio_nivel,
                       row_number() over (partition by coalesce(s.assunto_id, q.assunto_id)
                           order by r.respondido_em desc, r.id desc) as fim_nivel
                from respostas_aluno r
                left join sessoes_estudo s on s.id = r.sessao_id and s.aluno_id = r.aluno_id
                left join questoes q on r.sessao_id is null and q.id = r.questao_id
                where r.aluno_id = :alunoId and r.respondido_em >= :inicioResposta and r.respondido_em < :fimResposta
                  and (r.sessao_id is null or s.id is not null)
                  and (:assuntoId is null or coalesce(s.assunto_id, q.assunto_id) = :assuntoId)
                  and (:materiaId is null or exists (
                      select 1 from assuntos assunto
                      where assunto.id = coalesce(s.assunto_id, q.assunto_id) and assunto.materia_id = :materiaId
                  ))
            ), eventos as (
                select s.assunto_id, cast(null as date) as dia, cast(null as bigint) as questao_id,
                       cast(null as integer) as correta, 0 as erro_contabilizado, cast(null as integer) as primeira,
                       cast(null as integer) as nivel_anterior, cast(null as integer) as nivel_atual,
                       cast(null as bigint) as inicio_nivel, cast(null as bigint) as fim_nivel
                from sessoes_estudo s
                where s.aluno_id = :alunoId and s.iniciada_em >= :inicioSessao and s.iniciada_em < :fimSessao
                  and (:assuntoId is null or s.assunto_id = :assuntoId)
                  and (:materiaId is null or exists (
                      select 1 from assuntos assunto where assunto.id = s.assunto_id and assunto.materia_id = :materiaId
                  ))
                union all
                select assunto_id, dia, questao_id, correta, erro_contabilizado, primeira,
                       nivel_anterior, nivel_atual, inicio_nivel, fim_nivel
                from respostas_periodo
            )
            select case when grouping(a.id) = 1 and grouping(e.dia) = 1 then 1 else 0 end as geral,
                   a.id as assunto_id, a.nome, e.dia,
                   count(case when e.questao_id is null then 1 end) as sessoes_iniciadas,
                   count(e.questao_id) as tentativas, count(distinct e.questao_id) as questoes_unicas,
                   count(case when e.correta = 1 then 1 end) as acertos,
                   count(case when e.correta = 0 then 1 end) as tentativas_incorretas,
                   count(case when e.erro_contabilizado = 1 then 1 end) as erros,
                   count(case when e.primeira = 0 then 1 end) as tentativas_reaprendizagem,
                   count(case when e.primeira = 1 then 1 end) as primeiras_tentativas,
                   count(case when e.primeira = 1 and e.correta = 1 then 1 end) as primeiras_corretas,
                   max(case when e.inicio_nivel = 1 then e.nivel_anterior end) as nivel_inicial_observado,
                   max(case when e.fim_nivel = 1 then e.nivel_atual end) as nivel_final_observado
            from eventos e
            left join assuntos a on a.id = e.assunto_id
            group by grouping sets ((), (a.id, a.nome), (e.dia))
            having grouping(e.dia) = 1 or e.dia is not null
            order by a.id, e.dia
            """;

    /**
     * Agrega a janela para um aluno. Os filtros opcionais limitam os eventos ao assunto ou à matéria.
     * A zona converte o início e o fim do período para instantes nas colunas de sessão.
     */
    public List<Linha> agregar(long alunoId, Long assuntoId, Long materiaId, LocalDate inicio, LocalDate fimExclusivo, ZoneId zona) {
        return jdbc.sql(AGREGAR)
                .param("alunoId", alunoId)
                .param("assuntoId", assuntoId, Types.BIGINT)
                .param("materiaId", materiaId, Types.BIGINT)
                // A sessão guarda um instante, enquanto a resposta guarda a hora local do módulo do aluno.
                .param("inicioSessao", inicio.atStartOfDay(zona).toOffsetDateTime())
                .param("fimSessao", fimExclusivo.atStartOfDay(zona).toOffsetDateTime())
                .param("inicioResposta", inicio.atStartOfDay())
                .param("fimResposta", fimExclusivo.atStartOfDay())
                .query(Linha.class).list();
    }

    public record Linha(boolean geral, Long assuntoId, String nome, LocalDate dia,
                        long sessoesIniciadas, long tentativas, long questoesUnicas, long acertos,
                        long tentativasIncorretas, long erros,
                        long tentativasReaprendizagem, long primeirasTentativas, long primeirasCorretas,
                        Integer nivelInicialObservado, Integer nivelFinalObservado) {}
}
