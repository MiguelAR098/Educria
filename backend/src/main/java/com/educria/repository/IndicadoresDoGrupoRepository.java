package com.educria.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/** Usa SQL Server via JdbcClient para projetar somente dados necessários aos indicadores agregados. */
@Repository
@RequiredArgsConstructor
public class IndicadoresDoGrupoRepository {
    private final JdbcClient jdbc;

    public record AlunoDoRecorte(Long id, Long origemId, String origemNome) {}
    public record NivelDoAssunto(Long alunoId, int nivelAtual, Integer nivelInicial) {}

    /*
     * Inclui participantes vigentes e alunos ativos das turmas do grupo. No grupo de comparação,
     * exclui alunos com vínculo vigente em outro grupo ativo do mesmo professor que abranja a turma atual.
     */
    private static final String RECORTE = """
            with recorte as (
                select al.usuario_id as aluno_id, p.turma_origem_id
                from dbo.grupo_turmas contexto
                join dbo.alunos al on al.turma_id = contexto.turma_id
                join dbo.usuarios u on u.id = al.usuario_id and u.ativo = 1
                left join dbo.grupo_participacoes p on p.grupo_id = contexto.grupo_id
                    and p.aluno_id = al.usuario_id and p.saida_em is null
                where contexto.grupo_id = :grupoId and (
                    p.id is not null or not exists (
                        select 1
                        from dbo.grupo_participacoes outro
                        join dbo.grupos_pedagogicos g on g.id = outro.grupo_id
                        join dbo.grupo_turmas alcance on alcance.grupo_id = g.id and alcance.turma_id = al.turma_id
                        where outro.aluno_id = al.usuario_id and outro.saida_em is null
                            and g.professor_id = :professorId and g.arquivado_em is null
                    )
                )
            )
            """;

    public List<AlunoDoRecorte> alunos(Long grupoId, Long professorId) {
        return jdbc.sql(RECORTE + """
                select r.aluno_id, t.id as origem_id, t.nome as origem_nome
                from recorte r left join dbo.turmas t on t.id = r.turma_origem_id
                """)
                .param("grupoId", grupoId).param("professorId", professorId)
                .query((rs, i) -> new AlunoDoRecorte(rs.getLong("aluno_id"),
                        rs.getObject("origem_id", Long.class), rs.getString("origem_nome")))
                .list();
    }

    /**
     * Consulta níveis dos assuntos praticados na matéria. OUTER APPLY obtém o último nível antes
     * do início ou o nível anterior da primeira resposta histórica que registrar esse valor.
     * Níveis históricos preenchidos sempre pertencem a uma sessão, pelo CHECK de respostas_aluno.
     */
    public List<NivelDoAssunto> niveis(Long grupoId, Long professorId, Long materiaId, LocalDateTime inicio) {
        return jdbc.sql(RECORTE + """
                select d.aluno_id, d.nivel_atual, coalesce(anterior.nivel_atual, primeira.nivel_anterior) as nivel_inicial
                from recorte r
                join dbo.desempenho_por_assunto d on d.aluno_id = r.aluno_id
                join dbo.assuntos a on a.id = d.assunto_id and a.materia_id = :materiaId
                outer apply (
                    select top (1) h.nivel_atual
                    from dbo.respostas_aluno h join dbo.sessoes_estudo s on s.id = h.sessao_id
                    where h.aluno_id = d.aluno_id and s.assunto_id = d.assunto_id
                        and h.nivel_atual is not null and h.respondido_em < :inicio
                    order by h.respondido_em desc, h.id desc
                ) anterior
                outer apply (
                    select top (1) h.nivel_anterior
                    from dbo.respostas_aluno h join dbo.sessoes_estudo s on s.id = h.sessao_id
                    where anterior.nivel_atual is null and h.aluno_id = d.aluno_id and s.assunto_id = d.assunto_id
                        and h.nivel_anterior is not null
                    order by h.respondido_em, h.id
                ) primeira
                where d.total_acertos > 0 or d.total_erros > 0
                """)
                .param("grupoId", grupoId).param("professorId", professorId).param("materiaId", materiaId)
                .param("inicio", inicio)
                .query((rs, i) -> new NivelDoAssunto(rs.getLong("aluno_id"), rs.getInt("nivel_atual"),
                        rs.getObject("nivel_inicial", Integer.class)))
                .list();
    }

    /** Identifica atividade no período sem carregar cada resposta. A sessão determina o assunto quando disponível. */
    public List<Long> alunosComRespostaNaJanela(Long grupoId, Long professorId, Long materiaId,
                                               LocalDateTime inicio, LocalDateTime fimExclusivo) {
        return jdbc.sql(RECORTE + """
                select r.aluno_id from recorte r
                where exists (
                    select 1
                    from dbo.respostas_aluno h
                    join dbo.questoes q on q.id = h.questao_id
                    left join dbo.sessoes_estudo s on s.id = h.sessao_id
                    join dbo.assuntos a on a.id = coalesce(s.assunto_id, q.assunto_id)
                    where h.aluno_id = r.aluno_id and a.materia_id = :materiaId
                        and h.respondido_em >= :inicio and h.respondido_em < :fimExclusivo
                )
                """)
                .param("grupoId", grupoId).param("professorId", professorId).param("materiaId", materiaId)
                .param("inicio", inicio).param("fimExclusivo", fimExclusivo)
                .query(Long.class).list();
    }
}
