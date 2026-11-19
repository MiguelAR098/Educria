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

    // Os extremos são os da primeira e da última resposta, inclusive quando o nível legado é nulo.
    private static final String INDICADORES = """
            select s.id, a.nome as assunto, s.situacao, s.iniciada_em, count(sq.questao_id) as apresentadas,
                   count(*) filter (where r.correta) as acertos, count(*) filter (where not r.correta) as erros,
                   (array_agg(r.nivel_anterior order by sq.indice) filter (where r.id is not null))[1] as antes,
                   (array_agg(r.nivel_atual order by sq.indice desc) filter (where r.id is not null))[1] as depois
            from sessoes_estudo s
            join usuarios u on u.id = s.aluno_id
            join assuntos a on a.id = s.assunto_id
            left join sessao_questoes sq on sq.sessao_id = s.id
            left join respostas_aluno r on r.sessao_id = s.id and r.questao_id = sq.questao_id and r.aluno_id = s.aluno_id
            where u.email = :email
            """;
    private static final String AGRUPAR = " group by s.id, a.nome order by s.id desc ";
    private static final RowMapper<Indicadores> LER = (rs, i) -> new Indicadores(
            rs.getLong("id"), rs.getString("assunto"), SituacaoSessao.valueOf(rs.getString("situacao")),
            rs.getTimestamp("iniciada_em").toInstant(), rs.getInt("apresentadas"), rs.getInt("acertos"),
            rs.getInt("erros"), rs.getObject("antes", Integer.class), rs.getObject("depois", Integer.class));

    public List<Indicadores> listar(String email, Long antesDe, int limite) {
        String filtro = antesDe == null ? "" : " and s.id < :antesDe";
        var consulta = jdbc.sql(INDICADORES + filtro + AGRUPAR + "limit :limite")
                .param("email", email).param("limite", limite);
        if (antesDe != null) consulta = consulta.param("antesDe", antesDe);
        return consulta.query(LER).list();
    }

    public Optional<Indicadores> obter(String email, long id) {
        return jdbc.sql(INDICADORES + " and s.id = :id" + AGRUPAR)
                .param("email", email).param("id", id).query(LER).optional();
    }

    public List<Item> questoes(String email, long id) {
        return jdbc.sql("""
                select sq.indice + 1 as numero, q.enunciado, r.resposta_dada as alternativa, r.correta
                from sessoes_estudo s
                join usuarios u on u.id = s.aluno_id
                join sessao_questoes sq on sq.sessao_id = s.id
                join questoes q on q.id = sq.questao_id
                left join respostas_aluno r on r.sessao_id = s.id and r.questao_id = q.id and r.aluno_id = s.aluno_id
                where u.email = :email and s.id = :id order by sq.indice
                """).param("email", email).param("id", id).query(Item.class).list();
    }

    public List<Revisao> revisoes(String email, long id) {
        return jdbc.sql("""
                select q.id as questao_id, q.enunciado, rep.proxima_revisao
                from sessoes_estudo s
                join usuarios u on u.id = s.aluno_id
                join respostas_aluno r on r.sessao_id = s.id and r.aluno_id = s.aluno_id
                join questoes q on q.id = r.questao_id
                join atividades a on a.id = q.atividade_id
                join repeticao_espacada rep on rep.questao_id = q.id and rep.aluno_id = s.aluno_id
                where u.email = :email and s.id = :id and a.ativa and q.assunto_id is not null and q.dificuldade is not null
                order by rep.proxima_revisao, q.id
                """).param("email", email).param("id", id).query(Revisao.class).list();
    }
}
