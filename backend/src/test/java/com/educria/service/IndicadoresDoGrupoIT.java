package com.educria.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = IntegracaoSqlServer.VARIAVEL_URL, matches = ".+")
class IndicadoresDoGrupoIT extends IntegracaoSqlServer {
    @Autowired private JdbcTemplate db;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PasswordEncoder encoder;
    @Autowired private Clock relogio;
    @Autowired private EntityManager entidades;

    @Test
    void usaAsTurmasEAJanelaDoPanoramaSemDuplicarNemExporEstudantes() throws Exception {
        long turmaA = turma("Turma A");
        long turmaB = turma("Turma B");
        long externa = turma("Turma externa");
        long matematica = materia("Matemática");
        long portugues = materia("Português");
        long fracoes = id("select id from dbo.assuntos where nome=N'frações' and materia_id=?", matematica);
        long leitura = id("insert dbo.assuntos(materia_id,nome) output inserted.id values (?,N'Leitura')", portugues);
        long questaoMat = questao(fracoes);
        long questaoEditada = questao(fracoes);
        long questaoPort = questao(leitura);
        var dono = professor(matematica, turmaA, turmaB);
        var colega = professor(matematica, turmaA);
        long alvo = grupo(dono.id(), false, turmaA, turmaB);
        long outro = grupo(dono.id(), false, turmaA);
        long deColega = grupo(colega.id(), false, turmaA);
        long encerrado = grupo(dono.id(), true, turmaA);

        long ana = aluno("Ana fictícia", turmaA);
        long bia = aluno("Bia fictícia", turmaA);
        long transferida = aluno("Estudante transferida", turmaB);
        long emOutrosGrupos = aluno("Participa em vários", turmaA);
        long comparado = aluno("Comparado fictício", turmaB);
        long foraDaJanela = aluno("Sem evolução recente", turmaB);
        long participanteDoColega = aluno("Em grupo de outro professor", turmaA);
        long inativo = aluno("Conta inativa", turmaA);

        vincular(alvo, ana, turmaA);
        vincular(alvo, bia, turmaA);
        vincular(alvo, transferida, turmaB);
        vincular(alvo, inativo, turmaA);
        vincular(outro, ana, turmaA);
        vincular(outro, emOutrosGrupos, turmaA);
        vincular(grupo(dono.id(), false, turmaA), emOutrosGrupos, turmaA);
        vincular(deColega, participanteDoColega, turmaA);
        vincular(encerrado, participanteDoColega, turmaA);
        db.update("update dbo.alunos set turma_id=? where usuario_id=?", turmaB, ana);
        db.update("update dbo.alunos set turma_id=? where usuario_id=?", externa, transferida);
        db.update("update dbo.usuarios set ativo=0 where id=?", inativo);

        var hoje = LocalDate.now(relogio);
        var inicio = hoje.minusDays(30).atStartOfDay();
        var fim = hoje.plusDays(1).atStartOfDay();
        resposta(ana, fracoes, questaoMat, inicio.minusDays(3), 1, 2, 10);
        resposta(ana, fracoes, questaoEditada, inicio, 4, 5, 10);
        db.update("update dbo.questoes set assunto_id=? where id=?", leitura, questaoEditada);
        resposta(ana, leitura, questaoPort, inicio.plusDays(1), 8, 10, 10);
        resposta(comparado, fracoes, questaoMat, inicio, 3, 3, 0);
        resposta(foraDaJanela, fracoes, questaoMat, inicio.minusSeconds(1), 1, 3, 10);
        resposta(foraDaJanela, fracoes, questaoMat, fim, 3, 4, 10);
        resposta(transferida, fracoes, questaoMat, inicio.plusDays(1), 1, 9, 10);
        resposta(emOutrosGrupos, fracoes, questaoMat, inicio.plusDays(1), 1, 9, 10);
        resposta(inativo, fracoes, questaoMat, inicio.plusDays(1), 1, 9, 10);

        JsonNode relatorio = indicadores(alvo, dono.token());
        assertEquals(alvo, relatorio.path("grupoId").asLong());
        assertEquals(matematica, relatorio.at("/materia/id").asLong());
        assertEquals("PROJETO", relatorio.path("tipo").asText());
        assertEquals("GRUPOS_ATIVOS_DO_PROFESSOR_NAS_TURMAS_DO_GRUPO", relatorio.path("comparacaoEscopo").asText());
        assertEquals(30, relatorio.at("/periodo/dias").asInt());
        assertEquals(inicio, LocalDateTime.parse(relatorio.at("/periodo/inicio").asText()));
        assertEquals(fim, LocalDateTime.parse(relatorio.at("/periodo/fimExclusivo").asText()));
        assertEquals("America/Sao_Paulo", relatorio.at("/periodo/fuso").asText());
        assertEquals(2, relatorio.at("/participantes/total").asInt());
        assertEquals(1, relatorio.path("turmasDeOrigem").size());
        assertEquals(turmaA, relatorio.at("/turmasDeOrigem/0/id").asLong());
        assertEquals(2, relatorio.at("/turmasDeOrigem/0/participantes").asLong());
        medida(relatorio.at("/participantes/nivel"), 5, 1, 1);
        medida(relatorio.at("/participantes/evolucao30d"), 0.38, 1, 1);
        assertEquals(3, relatorio.at("/naoParticipantes/total").asInt());
        medida(relatorio.at("/naoParticipantes/nivel"), 3.5, 2, 1);
        medida(relatorio.at("/naoParticipantes/evolucao30d"), 0, 1, 2);
        assertFalse(relatorio.toString().contains("fictícia"));
        assertFalse(relatorio.toString().contains("Turma externa"));

        var panorama = mvc.perform(get("/api/professor/turmas/{id}/panorama", turmaB)
                        .header("Authorization", "Bearer " + dono.token()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        var indicadorAna = StreamSupport.stream(json.readTree(panorama).path("alunos").spliterator(), false)
                .filter(linha -> linha.path("alunoId").asLong() == ana).findFirst().orElseThrow();
        assertEquals(indicadorAna.path("evolucao30d"), relatorio.at("/participantes/evolucao30d/media"));
    }

    @Test
    void relatorioVazioEArquivadoRespeitamTitularidadeETurmasAtuais() throws Exception {
        long turmaA = turma("Turma A");
        long turmaB = turma("Turma B");
        long matematica = materia("Matemática");
        var dono = professor(matematica, turmaA, turmaB);
        var colega = professor(matematica, turmaA, turmaB);
        long arquivado = grupo(dono.id(), true, turmaA, turmaB);
        long vazio = grupo(dono.id(), false, turmaA);
        long aluno = aluno("Estudante arquivado", turmaA);
        vincular(arquivado, aluno, turmaA);

        JsonNode recorteVazio = indicadores(vazio, dono.token());
        assertEquals(0, recorteVazio.at("/participantes/total").asInt());
        assertTrue(recorteVazio.at("/participantes/nivel/media").isNull());
        assertEquals(1, recorteVazio.at("/naoParticipantes/total").asInt());
        JsonNode anterior = indicadores(arquivado, dono.token());
        assertEquals(1, anterior.at("/participantes/total").asInt());
        assertEquals(0, anterior.at("/naoParticipantes/total").asInt());
        assertTrue(anterior.at("/participantes/evolucao30d/media").isNull());
        mvc.perform(get("/api/professor/grupos/{id}/indicadores", arquivado)
                        .header("Authorization", "Bearer " + colega.token()))
                .andExpect(status().isNotFound());
        long turmaSubstituta = turma("Turma substituta");
        db.update("update dbo.professor_turmas set turma_id=? where professor_id=? and turma_id=?",
                turmaSubstituta, dono.id(), turmaB);
        entidades.clear();
        mvc.perform(get("/api/professor/grupos/{id}/indicadores", arquivado)
                        .header("Authorization", "Bearer " + dono.token()))
                .andExpect(status().isNotFound());
    }

    private JsonNode indicadores(long grupo, String token) throws Exception {
        var resposta = mvc.perform(get("/api/professor/grupos/{id}/indicadores", grupo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        return json.readTree(resposta.andReturn().getResponse().getContentAsByteArray());
    }

    private static void medida(JsonNode objeto, double media, int comDados, int semDados) {
        assertEquals(media, objeto.path("media").asDouble(Double.NaN));
        assertEquals(comDados, objeto.path("comDados").asInt(-1));
        assertEquals(semDados, objeto.path("semDados").asInt(-1));
    }

    private long grupo(long professor, boolean arquivado, long... turmas) {
        long grupo = id("""
                insert dbo.grupos_pedagogicos(professor_id,nome,tipo,criado_em,arquivado_em)
                output inserted.id values (?,N'Projeto de teste',N'PROJETO',sysdatetimeoffset(),
                    case when ?=1 then dateadd(minute,1,sysdatetimeoffset()) end)
                """, professor, arquivado ? 1 : 0);
        for (long turma : turmas) db.update("insert dbo.grupo_turmas(grupo_id,turma_id) values (?,?)", grupo, turma);
        return grupo;
    }

    private void vincular(long grupo, long aluno, long turma) {
        db.update("insert dbo.grupo_participacoes(grupo_id,aluno_id,turma_origem_id,entrada_em) values (?,?,?,sysdatetimeoffset())",
                grupo, aluno, turma);
    }

    private void resposta(long aluno, long assunto, long questao, LocalDateTime data, int antes, int depois, int pontos) {
        long sessao = id("""
                insert dbo.sessoes_estudo(aluno_id,assunto_id,situacao,iniciada_em,atualizada_em)
                output inserted.id values (?,?,'ENCERRADA',sysdatetimeoffset(),sysdatetimeoffset())
                """, aluno, assunto);
        db.update("insert dbo.sessao_questoes(sessao_id,indice,questao_id) values (?,0,?)", sessao, questao);
        db.update("""
                insert dbo.respostas_aluno(aluno_id,questao_id,sessao_id,passo,resposta_dada,correta,
                                            respondido_em,nivel_anterior,nivel_atual,pontos_individuais)
                values (?,?,?,1,N'A',1,?,?,?,?)
                """, aluno, questao, sessao, data, antes, depois, pontos);
        db.update("update dbo.desempenho_por_assunto set nivel_atual=?, total_acertos=total_acertos+1 where aluno_id=? and assunto_id=?",
                depois, aluno, assunto);
    }

    private long turma(String nome) {
        return id("insert dbo.turmas(nome) output inserted.id values (?)", nome);
    }

    private long materia(String nome) {
        return id("select id from dbo.materias where nome=?", nome);
    }

    private long questao(long assunto) {
        long atividade = id("insert dbo.atividades(titulo) output inserted.id values(N'Questão de teste')");
        return id("""
                insert dbo.questoes(atividade_id,assunto_id,dificuldade,enunciado,resposta_correta)
                output inserted.id values (?,?,1,N'Questão de teste',N'A')
                """, atividade, assunto);
    }

    private long aluno(String nome, long turma) {
        long aluno = id("insert dbo.usuarios(email,nome,senha,perfil) output inserted.id values (?,?,N'fixture',N'ALUNO')",
                UUID.randomUUID() + "@example.test", nome);
        db.update("insert dbo.alunos(usuario_id,turma_id) values (?,?)", aluno, turma);
        return aluno;
    }

    private ProfessorTeste professor(long materia, long... turmas) throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        String senha = segredoTemporario();
        long professor = id("insert dbo.usuarios(email,nome,senha,perfil) output inserted.id values (?,N'Docente',?,N'PROFESSOR')",
                email, encoder.encode(senha));
        db.update("insert dbo.professores(usuario_id,materia_id) values (?,?)", professor, materia);
        for (long turma : turmas) db.update("insert dbo.professor_turmas(professor_id,turma_id) values (?,?)", professor, turma);
        var resposta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", email, "senha", senha))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        return new ProfessorTeste(professor, json.readTree(resposta).path("token").asText());
    }

    private long id(String sql, Object... parametros) {
        return db.queryForObject(sql, Long.class, parametros);
    }

    private record ProfessorTeste(long id, String token) {}
}
