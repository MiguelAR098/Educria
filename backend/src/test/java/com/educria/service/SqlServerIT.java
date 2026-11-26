package com.educria.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = IntegracaoSqlServer.VARIAVEL_URL, matches = ".+")
class SqlServerIT extends IntegracaoSqlServer {
    private static final String SESSOES = "/api/aluno/sessoes";
    @Autowired private JdbcTemplate db;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PasswordEncoder encoder;

    @Test
    void flywayValidaEsquemaComAIdentidadeEOCatalogo() {
        assertEquals(5, id("select count(*) from dbo.flyway_schema_history where success=1 and version in ('1','2','3','4','5')"));
        assertEquals(1, id("""
                select count(*) from sys.columns
                where object_id=object_id('dbo.respostas_aluno') and name='pontos_individuais'
                    and is_nullable=1 and default_object_id=0
                """));
        assertEquals(1, id("""
                select count(*) from sys.indexes
                where object_id=object_id('dbo.respostas_aluno') and name='idx_respostas_aluno_pontos_por_sessao'
                    and has_filter=1 and is_disabled=0
                """));
        assertEquals(1000, id("select cast(ident_seed('dbo.recados') as bigint)"));
        assertEquals(10, id("select cast(ident_incr('dbo.recados') as bigint)"));
        assertEquals(3, id("select count(*) from dbo.materias where nome in (N'Matemática',N'Português',N'Ciências')"));
    }

    @Test
    void alunoUsaSelecaoFilaIdempotenciaAgendaEConsultasDeHistorico() throws Exception {
        long materia = id("select id from dbo.materias where nome=N'Matemática'");
        long assunto = id("insert dbo.assuntos(materia_id,nome) output inserted.id values (?,?)", materia, UUID.randomUUID().toString());
        long atividade = id("insert dbo.atividades(titulo) output inserted.id values (N'Atividade de validação')");
        db.update("""
                insert dbo.questoes(atividade_id,assunto_id,dificuldade,enunciado,alternativa_a,alternativa_b,resposta_correta)
                values (?,?,1,N'Questão vazia',?,N'Errada',N'A')
                """, atividade, assunto, "\t\u2003");
        long questao = id("""
                insert dbo.questoes(atividade_id,assunto_id,dificuldade,enunciado,alternativa_a,alternativa_b,resposta_correta)
                output inserted.id values (?,?,1,N'Quanto é três mais um?',N'Quatro',N'Três',N'A')
                """, atividade, assunto);
        var aluno = cadastrar(null);
        var outro = cadastrar(null);
        assertEquals(1, id("select nivel_atual from dbo.desempenho_por_assunto where aluno_id=? and assunto_id=?",
                aluno.get("id").asLong(), assunto));
        String token = aluno.get("token").asText();
        var semAtividade = chamar(get("/api/aluno/evolucao").param("assuntoId", Long.toString(assunto)), token, null, 200);
        assertEquals(0, semAtividade.at("/resumo/tentativas").asLong());
        assertEquals(30, semAtividade.get("evolucaoDiaria").size());
        chamar(post(SESSOES), token, Map.of("assuntoId", -1), 400);
        var estado = chamar(post(SESSOES), token, Map.of("assuntoId", assunto), 201);
        assertEquals(questao, estado.at("/questaoAtual/id").asLong());
        assertFalse(estado.get("questaoAtual").has("respostaCorreta"));
        long sessao = estado.at("/sessao/id").asLong();
        String rota = SESSOES + "/" + sessao;
        String resposta = rota + "/questoes/" + questao + "/resposta";
        chamar(get(rota), outro.get("token").asText(), null, 404);
        chamar(get(rota), "token-invalido", null, 401);
        for (int passo = 1; passo <= 4; passo++) {
            Map<String, Object> pedido = Map.of("passo", passo, "alternativa", passo == 4 ? "A" : "B",
                    "pontosConcedidos", 10_000);
            var feedback = chamar(put(resposta), token, pedido, 200);
            assertEquals(feedback, chamar(put(resposta), token, pedido, 200));
            assertEquals(passo == 4, feedback.get("correta").asBoolean());
            assertEquals(passo == 4 ? 6 : 0, feedback.get("pontosConcedidos").asInt());
            assertEquals(passo == 3, feedback.has("alternativaCorreta"));
            if (passo == 1) {
                assertEquals(feedback, chamar(put(rota + "/interromper"), token, null, 200).get("feedback"));
                assertEquals(feedback, chamar(put(rota + "/retomar"), token, null, 200).get("feedback"));
            }
            if (passo < 4) chamar(put(rota + "/questoes/" + (passo + 1)), token, null, 200);
        }
        chamar(put(rota + "/encerrar"), token, null, 200);
        db.update("update dbo.respostas_aluno set respondido_em=? where sessao_id=?",
                LocalDate.now(ZoneId.of("America/Sao_Paulo")).atTime(12, 0), sessao);
        var resultado = chamar(get(rota + "/resultado"), token, null, 200);
        assertEquals(4, resultado.get("questoes").size());
        assertEquals(1, resultado.get("revisoes").size());
        assertEquals(1, resultado.at("/sessao/erros").asLong());
        assertEquals(3, resultado.at("/sessao/tentativasIncorretas").asLong());
        assertEquals(1, chamar(get(SESSOES + "/historico"), token, null, 200).at("/sessoes/0/erros").asLong());
        var evolucao = chamar(get("/api/aluno/evolucao").param("assuntoId", Long.toString(assunto)), token, null, 200);
        assertEquals(4, evolucao.at("/resumo/tentativas").asLong());
        assertEquals(1, evolucao.at("/resumo/erros").asLong());
        assertEquals(30, evolucao.get("evolucaoDiaria").size());
        chamar(put(rota + "/retomar"), token, null, 409);
    }

    @Test
    void professorEnviaRecadoEAcessoRespeitaVinculos() throws Exception {
        long turma = id("insert dbo.turmas(nome) output inserted.id values (N'Turma de validação')");
        long materia = id("select id from dbo.materias where nome=N'Matemática'");
        String email = UUID.randomUUID() + "@example.test";
        String senha = segredoTemporario();
        long professor = id("""
                insert dbo.usuarios(email,nome,senha,perfil) output inserted.id values (?,N'Docente',?,N'PROFESSOR')
                """, email, encoder.encode(senha));
        db.update("insert dbo.professores(usuario_id,materia_id) values (?,?)", professor, materia);
        db.update("insert dbo.professor_turmas(professor_id,turma_id) values (?,?)", professor, turma);
        var professorAuth = chamar(post("/api/auth/login"), null, Map.of("email", email, "senha", senha), 200);
        var aluno = cadastrar(turma);
        var outro = cadastrar(null);
        String rota = "/api/professor/turmas/" + turma + "/alunos/";
        String token = professorAuth.get("token").asText();
        String alunoToken = aluno.get("token").asText();
        chamar(post(rota + aluno.get("id").asLong() + "/recados"), token, Map.of("texto", "Revisão de frações"), 201);
        chamar(post(rota + aluno.get("id").asLong() + "/recados"), alunoToken, Map.of("texto", "Teste de papel"), 403);
        String recados = "/api/aluno/recados";
        assertEquals("Revisão de frações", chamar(get(recados), alunoToken, null, 200).at("/recados/0/texto").asText());
        assertEquals(0, chamar(get(recados), outro.get("token").asText(), null, 200).get("recados").size());
        chamar(post(recados + "/lidos"), alunoToken, null, 204);
        assertEquals(0, chamar(get(recados), alunoToken, null, 200).get("naoLidos").asLong());
    }

    @Test
    void adminDesvinculaAlunoPreservandoContaSessaoEPontuacao() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        String senha = segredoTemporario();
        long admin = id("""
                insert dbo.usuarios(email,nome,senha,perfil) output inserted.id values (?,N'Administrador',?,N'ADMIN')
                """, email, encoder.encode(senha));
        db.update("insert dbo.admins(usuario_id) values (?)", admin);
        String token = chamar(post("/api/auth/login"), null, Map.of("email", email, "senha", senha), 200)
                .get("token").asText();
        String turmas = "/api/admin/turmas";
        var turmaCriada = chamar(post(turmas), token, Map.of("nome", "Turma de vínculo", "codigo", "VINC"), 201);
        assertEquals("VINC", turmaCriada.get("codigo").asText());
        long turma = turmaCriada.get("id").asLong();
        long outraTurma = chamar(post(turmas), token, Map.of("nome", "Outra turma"), 201).get("id").asLong();
        chamar(post(turmas + "/" + turma + "/pontos"), token, Map.of("pontos", 20), 200);
        var aluno = cadastrar(turma);
        long alunoId = aluno.get("id").asLong();
        long assunto = id("select min(id) from dbo.assuntos");
        long sessao = id("""
                insert dbo.sessoes_estudo(aluno_id,assunto_id,situacao,iniciada_em,atualizada_em)
                output inserted.id values (?,?,'ENCERRADA',sysdatetimeoffset(),sysdatetimeoffset())
                """, alunoId, assunto);
        String alunoToken = aluno.get("token").asText();
        String rota = turmas + "/" + turma + "/alunos/" + alunoId;

        chamar(delete(rota), alunoToken, null, 403);
        chamar(delete(turmas + "/" + outraTurma + "/alunos/" + alunoId), token, null, 404);
        assertEquals(turma, id("select turma_id from dbo.alunos where usuario_id=?", alunoId));
        chamar(delete(rota), token, null, 204);
        chamar(delete(rota), token, null, 404);

        assertEquals(1, id("select count(*) from dbo.alunos where usuario_id=? and turma_id is null", alunoId));
        assertEquals(20, id("select pontos_totais from dbo.pontuacao_coletiva where turma_id=?", turma));
        var resultado = chamar(get(SESSOES + "/" + sessao + "/resultado"), alunoToken, null, 200);
        assertEquals(sessao, resultado.at("/sessao/id").asLong());
    }

    private JsonNode cadastrar(Long turma) throws Exception {
        var pedido = new java.util.HashMap<String, Object>(Map.of("nome", "Aluno", "email", UUID.randomUUID() + "@example.test",
                "senha", segredoTemporario()));
        if (turma != null) pedido.put("turmaId", turma);
        return chamar(post("/api/auth/register"), null, pedido, 201);
    }

    private JsonNode chamar(MockHttpServletRequestBuilder req, String token, Map<String, Object> pedido, int codigo) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (pedido != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(pedido));
        return json.readTree(mvc.perform(req).andExpect(status().is(codigo)).andReturn().getResponse().getContentAsByteArray());
    }

    private long id(String sql, Object... args) {
        return db.queryForObject(sql, Long.class, args);
    }
}
