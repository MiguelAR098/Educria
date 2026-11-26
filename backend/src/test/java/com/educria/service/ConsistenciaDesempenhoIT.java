package com.educria.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = IntegracaoSqlServer.VARIAVEL_URL, matches = ".+")
class ConsistenciaDesempenhoIT extends IntegracaoSqlServer {
    private static final String ALUNO = "/api/aluno";
    private static final String SESSOES = ALUNO + "/sessoes";
    private static final String PROFESSOR = "/api/professor";
    @Autowired private JdbcTemplate db;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PasswordEncoder encoder;
    @Autowired private EntityManager entidades;
    private long matematica, portugues, turma, assuntoMat, assuntoPort;

    @BeforeEach
    void preparar() {
        turma = id("insert dbo.turmas(nome) output inserted.id values (N'Turma do desempenho')");
        matematica = id("select id from dbo.materias where nome=N'Matemática'");
        portugues = id("select id from dbo.materias where nome=N'Português'");
        assuntoMat = assuntoComQuestao(matematica);
        assuntoPort = assuntoComQuestao(portugues);
    }

    /** Regressão que compara a projeção do painel com as medidas atuais de cada assunto. */
    @Test
    void desempenhoDoTitularAcompanhaAssuntosSemRecontarTentativasOuAlterarOLegado() throws Exception {
        var aluno = aluno();
        assertDesempenho(aluno, 1, 0, 0, 0, 0);
        estudar(aluno, assuntoMat, true);
        assertDesempenho(aluno, 2, 1, 0, 0, 10);

        estudar(aluno, assuntoPort, false);
        // A questão recuperada dá pontos, mas o diagnóstico mantém a primeira tentativa incorreta.
        assertDesempenho(aluno, 2, 1, 1, 0, 16);
        assertEquals(3L, id("select count(*) from dbo.respostas_aluno where aluno_id=?", aluno.id()));
        assertEquals(1L, id("select nivel_atual from dbo.desempenho_adaptativo where aluno_id=?", aluno.id()));
        assertEquals(0L, id("select total_acertos+total_erros from dbo.desempenho_adaptativo where aluno_id=?", aluno.id()));

        var catalogo = api(get(ALUNO + "/materias"), aluno, null, 200);
        assertEquals(10L, porId(catalogo, "id", matematica).path("pontosIndividuais").asLong());
        assertEquals(6L, porId(catalogo, "id", portugues).path("pontosIndividuais").asLong());
        conferirAssunto(aluno, matematica, assuntoMat, 2, 10);
        conferirAssunto(aluno, portugues, assuntoPort, 1, 6);
    }

    /** Dois professores da mesma turma devem obter diagnósticos diferentes, conforme sua matéria. */
    @Test
    void doisProfessoresLeemDiagnosticosDaPropriaMateriaNaTurmaENoDetalhe() throws Exception {
        var mat = professor(matematica);
        var port = professor(portugues);
        var bilingue = aluno();
        var soMatematica = aluno();
        var semHistorico = aluno();
        estudar(bilingue, assuntoMat, true);
        estudar(bilingue, assuntoPort, false);
        estudar(soMatematica, assuntoMat, false);

        String rota = PROFESSOR + "/turmas/" + turma;
        var turmaMat = api(get(rota), mat, null, 200);
        var turmaPort = api(get(rota), port, null, 200);
        assertEquals(1.5, turmaMat.at("/resumo/nivelMedio").asDouble());
        assertEquals(1.0, turmaPort.at("/resumo/nivelMedio").asDouble());
        assertEquals(1, turmaMat.at("/resumo/semDados").asInt());
        assertEquals(2, turmaPort.at("/resumo/semDados").asInt());
        assertEquals(1, turmaPort.at("/resumo/atencao").asInt());

        conferirAluno(turmaMat, mat, rota, bilingue, assuntoMat, 2, 1, 0, 0, 100.0, "NO_CAMINHO");
        conferirAluno(turmaMat, mat, rota, soMatematica, assuntoMat, 1, 0, 1, 1, 0.0, "ATENCAO");
        conferirAluno(turmaPort, port, rota, bilingue, assuntoPort, 1, 0, 1, 1, 0.0, "ATENCAO");
        conferirAluno(turmaPort, port, rota, soMatematica, assuntoPort, 1, 0, 0, 0, null, "SEM_DADOS");
        conferirAluno(turmaMat, mat, rota, semHistorico, assuntoMat, 1, 0, 0, 0, null, "SEM_DADOS");

        conferirReferencias(mat, rota, bilingue, soMatematica, assuntoMat, 1.5, 2, 10);
        conferirReferencias(port, rota, bilingue, soMatematica, assuntoPort, 1, 1, 6);
    }

    @Test
    void preservaDiagnosticoLegadoPrivadoEExibeMediaSemCombinarSequencias() throws Exception {
        var docente = professor(matematica);
        var aluno = aluno();
        long outroAssunto = assuntoComQuestao(matematica);
        db.update("""
                update dbo.desempenho_adaptativo set nivel_atual=7, total_acertos=5, total_erros=2, erros_consecutivos=1
                where aluno_id=?
                """, aluno.id());
        entidades.clear();
        assertDesempenho(aluno, 7, 5, 2, 1, 0);

        String rota = PROFESSOR + "/turmas/" + turma;
        var turmaInicial = api(get(rota), docente, null, 200);
        assertTrue(turmaInicial.at("/resumo/nivelMedio").isNull());
        assertEquals("SEM_DADOS", porId(turmaInicial.path("alunos"), "id", aluno.id()).path("situacao").asText());
        estudar(aluno, assuntoMat, true);
        estudar(aluno, outroAssunto, false);

        assertDesempenho(aluno, 2, 1, 1, 0, 16);
        var leitura = api(get(rota), docente, null, 200);
        assertEquals(1.5, leitura.at("/resumo/nivelMedio").asDouble());
        var detalhe = api(get(rota + "/alunos/" + aluno.id()), docente, null, 200);
        assertEquals(2, detalhe.at("/aluno/nivelAtual").asInt());
        assertEquals(50.0, detalhe.at("/aluno/taxaAcerto").asDouble());
        assertEquals(0, detalhe.at("/aluno/errosConsecutivos").asInt());
        assertEquals(1, porId(detalhe.path("assuntos"), "id", outroAssunto).path("errosConsecutivos").asInt());
        assertEquals(7L, id("select nivel_atual from dbo.desempenho_adaptativo where aluno_id=?", aluno.id()));
        assertEquals(7L, id("select total_acertos+total_erros from dbo.desempenho_adaptativo where aluno_id=?", aluno.id()));
    }

    private void conferirAluno(JsonNode turmaJson, Conta professor, String turmaRota, Conta aluno, long assunto,
                                int nivel, int acertos, int erros, int consecutivos, Double taxa,
                                String situacao) throws Exception {
        var resumo = porId(turmaJson.path("alunos"), "id", aluno.id());
        var detalhe = api(get(turmaRota + "/alunos/" + aluno.id()), professor, null, 200);
        var topico = porId(detalhe.path("assuntos"), "id", assunto);
        assertEquals(resumo, detalhe.path("aluno"));
        for (var dados : List.of(resumo, topico)) {
            assertEquals(nivel, dados.path("nivelAtual").asInt());
            assertEquals(acertos, dados.path("acertos").asInt());
            assertEquals(erros, dados.path("erros").asInt());
            assertEquals(consecutivos, dados.path("errosConsecutivos").asInt());
            assertEquals(situacao, dados.path("situacao").asText());
            if (taxa == null) assertTrue(dados.path("taxaAcerto").isNull());
            else assertEquals(taxa.doubleValue(), dados.path("taxaAcerto").asDouble());
        }
        if (acertos + erros == 0) {
            assertTrue(resumo.path("ultimaAtividade").isNull());
        } else {
            var atividade = db.queryForObject("""
                    select max(r.respondido_em) from dbo.respostas_aluno r join dbo.sessoes_estudo s on s.id=r.sessao_id
                    where r.aluno_id=? and s.assunto_id=?
                    """, LocalDateTime.class, aluno.id(), assunto);
            assertEquals(atividade, LocalDateTime.parse(resumo.path("ultimaAtividade").asText()));
        }
    }

    private void conferirReferencias(Conta professor, String rota, Conta aluno, Conta outro, long assunto,
                                     double nivel, int comDados, int pontos) throws Exception {
        var mapa = api(get(rota + "/mapa"), professor, null, 200);
        var medida = porId(mapa.path("medias"), "assuntoId", assunto);
        assertEquals(nivel, medida.path("nivelMedio").asDouble());
        assertEquals(comDados, medida.path("alunosQuePraticaram").asInt());
        var panorama = api(get(rota + "/panorama"), professor, null, 200);
        var rankings = api(get(rota + "/rankings"), professor, null, 200);
        assertEquals(pontos, porId(rankings.path("rankingPontuacao"), "alunoId", aluno.id()).path("pontos").asInt());
        assertEquals(porId(panorama.path("alunos"), "alunoId", aluno.id()).path("evolucao30d"),
                porId(rankings.path("rankingEvolucao"), "alunoId", aluno.id()).path("evolucao30d"));
        var grupo = api(post(PROFESSOR + "/grupos"), professor,
                Map.of("nome", "Grupo de teste", "tipo", "GRUPO", "turmasIds", List.of(turma)), 201);
        String grupoRota = PROFESSOR + "/grupos/" + grupo.path("id").asLong();
        api(post(grupoRota + "/participantes"), professor, Map.of("alunosIds", List.of(aluno.id(), outro.id())), 200);
        var indicadores = api(get(grupoRota + "/indicadores"), professor, null, 200);
        assertEquals(nivel, indicadores.at("/participantes/nivel/media").asDouble());
        assertEquals(comDados, indicadores.at("/participantes/nivel/comDados").asInt());
        assertEquals(2 - comDados, indicadores.at("/participantes/nivel/semDados").asInt());
        assertTrue(indicadores.at("/naoParticipantes/nivel/media").isNull());
    }

    private void assertDesempenho(Conta conta, int nivel, int acertos, int erros, int consecutivos,
                                  int pontos) throws Exception {
        var dados = api(get(ALUNO + "/desempenho"), conta, null, 200);
        assertEquals(nivel, dados.path("nivelAtual").asInt());
        assertEquals(acertos, dados.path("totalAcertos").asInt());
        assertEquals(erros, dados.path("totalErros").asInt());
        assertEquals(consecutivos, dados.path("errosConsecutivos").asInt());
        assertFalse(dados.path("nivelMaximo").asBoolean());
        assertEquals(pontos, dados.path("pontosIndividuais").asInt());
    }

    private void conferirAssunto(Conta conta, long materia, long assunto, int nivel, int pontos) throws Exception {
        var item = porId(api(get(ALUNO + "/materias/" + materia + "/assuntos"), conta, null, 200), "id", assunto);
        assertEquals(nivel, item.path("nivelAtual").asInt());
        assertEquals(pontos, item.path("pontosIndividuais").asInt());
    }

    private void estudar(Conta aluno, long assunto, boolean acerto) throws Exception {
        var primeiro = api(post(SESSOES), aluno, Map.of("assuntoId", assunto), 201);
        String rota = SESSOES + "/" + primeiro.at("/sessao/id").asLong();
        long questao = primeiro.at("/questaoAtual/id").asLong();
        String resposta = rota + "/questoes/" + questao + "/resposta";
        var pedido = Map.of("alternativa", acerto ? "A" : "B", "passo", 1);
        var feedback = api(put(resposta), aluno, pedido, 200);
        assertEquals(acerto, feedback.path("correta").asBoolean());
        assertEquals(feedback, api(put(resposta), aluno, pedido, 200));
        if (!acerto) {
            var repetida = api(put(rota + "/questoes/2"), aluno, null, 200);
            assertEquals(questao, repetida.at("/questaoAtual/id").asLong());
            var recuperacao = Map.of("alternativa", "A", "passo", 2);
            var recuperado = api(put(resposta), aluno, recuperacao, 200);
            assertEquals(6, recuperado.path("pontosConcedidos").asInt());
            assertEquals(recuperado, api(put(resposta), aluno, recuperacao, 200));
        }
        api(put(rota + "/encerrar"), aluno, null, 200);
    }

    private long assuntoComQuestao(long materia) {
        long assunto = id("insert dbo.assuntos(materia_id,nome) output inserted.id values (?,?)", materia, UUID.randomUUID().toString());
        long atividade = id("insert dbo.atividades(titulo) output inserted.id values (N'Atividade do desempenho')");
        db.update("""
                insert dbo.questoes(atividade_id,assunto_id,dificuldade,enunciado,alternativa_a,alternativa_b,resposta_correta)
                values (?,?,1,N'Questão de teste',N'Certa',N'Errada',N'A')
                """, atividade, assunto);
        return assunto;
    }

    private Conta aluno() throws Exception {
        return Conta.de(api(post("/api/auth/register"), null, Map.of("nome", "Aluno de teste", "turmaId", turma,
                "email", email(), "senha", segredoTemporario()), 201));
    }

    private Conta professor(long materia) throws Exception {
        String email = email();
        String senha = segredoTemporario();
        long professor = id("""
                insert dbo.usuarios(nome,email,senha,perfil) output inserted.id values (N'Docente',?,?,'PROFESSOR')
                """, email, encoder.encode(senha));
        db.update("insert dbo.professores(usuario_id,materia_id) values (?,?)", professor, materia);
        db.update("insert dbo.professor_turmas(professor_id,turma_id) values (?,?)", professor, turma);
        return Conta.de(api(post("/api/auth/login"), null, Map.of("email", email, "senha", senha), 200));
    }

    private JsonNode api(MockHttpServletRequestBuilder req, Conta conta, Object pedido, int codigo) throws Exception {
        if (conta != null) req.header("Authorization", "Bearer " + conta.token());
        if (pedido != null) req.contentType(APPLICATION_JSON).content(json.writeValueAsBytes(pedido));
        return json.readTree(mvc.perform(req).andExpect(status().is(codigo)).andReturn().getResponse().getContentAsByteArray());
    }

    private static JsonNode porId(JsonNode itens, String campo, long id) {
        for (var item : itens) if (item.path(campo).asLong() == id) return item;
        throw new AssertionError("Registro esperado ausente: " + campo + "=" + id);
    }

    private long id(String sql, Object... argumentos) {
        return db.queryForObject(sql, Long.class, argumentos);
    }

    private static String email() {
        return UUID.randomUUID() + "@example.test";
    }

    private record Conta(long id, String token) {
        static Conta de(JsonNode auth) {
            return new Conta(auth.path("id").asLong(), auth.path("token").asText());
        }
    }
}
