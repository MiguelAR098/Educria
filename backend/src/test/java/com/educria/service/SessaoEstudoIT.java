package com.educria.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = IntegracaoSqlServer.VARIAVEL_URL, matches = ".+")
class SessaoEstudoIT extends IntegracaoSqlServer {
    private static final String BASE = "/api/aluno/sessoes";
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final String AGENDA = "/api/aluno/revisoes";
    @MockBean private Clock relogio;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate db;
    private final List<Long> usuarios = new ArrayList<>();
    private final String prefixo = UUID.randomUUID().toString();
    private long materia, assunto, vazio, atividade;
    private String token;

    @BeforeEach
    void preparar() throws Exception {
        when(relogio.getZone()).thenReturn(FUSO);
        when(relogio.instant()).thenReturn(Instant.parse("2027-01-01T02:59:59Z"));
        materia = id("insert into materias(nome) output inserted.id values (?)", prefixo);
        assunto = id("insert into assuntos(materia_id,nome) output inserted.id values (?, 'Assunto teste')", materia);
        vazio = id("insert into assuntos(materia_id,nome) output inserted.id values (?, N'Sem questões')", materia);
        atividade = id("insert into atividades(titulo) output inserted.id values (?)", prefixo);
        db.update("""
                insert into questoes(atividade_id, assunto_id, dificuldade, enunciado, alternativa_a,
                    alternativa_b, alternativa_c, alternativa_d, resposta_correta, explicacao)
                select ?, ?, 1, concat(N'Questão ', n), 'a', 'b', 'c', 'd', 'A', N'Explicação'
                from (values (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11),(12)) as itens(n)
                """, atividade, assunto);
        token = cadastrar("titular");
    }

    @AfterEach
    void limpar() {
        for (Long usuario : usuarios) db.update("delete from usuarios where id=?", usuario);
        db.update("delete from atividades where id=?", atividade);
        db.update("delete from assuntos where materia_id=?", materia);
        db.update("delete from materias where id=?", materia);
    }

    @Test
    void isolaTodasAsOperacoesPeloJwt() throws Exception {
        JsonNode estado = abrir(assunto, 201);
        String rota = BASE + "/" + estado.at("/sessao/id").asLong();
        String outro = cadastrar("outro");
        String resposta = rota + "/questoes/" + estado.at("/questaoAtual/id").asLong() + "/resposta";
        String alternativa = body(Map.of("alternativa", "A"));
        call(put(resposta).content(alternativa), outro, 404);
        int anonimo = mvc.perform(put(resposta).contentType(MediaType.APPLICATION_JSON).content(alternativa))
                .andReturn().getResponse().getStatus();
        assertTrue(anonimo == 401 || anonimo == 403);
        call(get(rota), outro, 404);
        for (String acao : List.of("interromper", "retomar", "encerrar", "questoes/2")) {
            call(put(rota + "/" + acao), outro, 404);
        }
        assertEquals(0, call(get(BASE), outro, 200).size());
        assertEquals(1, call(get(BASE), token, 200).size());
        assertEquals(estado, call(get(rota), token, 200));
        int semJwt = mvc.perform(get(rota)).andReturn().getResponse().getStatus();
        assertTrue(semJwt == 401 || semJwt == 403);

        long admin = id("""
                insert into usuarios(nome,email,senha,perfil) output inserted.id
                select 'Admin',?,senha,'ADMIN' from usuarios where id=?
                """, prefixo+"-admin@example.test", usuarios.getFirst());
        usuarios.add(admin);
        db.update("insert into admins(usuario_id) values (?)", admin);
        String jwtAdmin = call(post("/api/auth/login").content(body(Map.of("email", prefixo+"-admin@example.test",
                "senha", prefixo))), null, 200).get("token").asText();
        call(get(BASE + "/historico"), jwtAdmin, 403);
        call(get(rota + "/resultado"), jwtAdmin, 403);
        call(get(AGENDA), jwtAdmin, 403);
        int statusAgenda = mvc.perform(get(AGENDA)).andReturn().getResponse().getStatus();
        assertTrue(statusAgenda == 401 || statusAgenda == 403);
        call(get(rota), jwtAdmin, 403);
        call(post(BASE), jwtAdmin, 403);
        call(put(resposta).content(alternativa), jwtAdmin, 403);
        assertEquals(0, id("select count(*) from respostas_aluno where sessao_id=?", estado.at("/sessao/id").asLong()));
    }

    @Test
    void interrupcaoRetomaMesmaQuestaoEEncerramentoETerminal() throws Exception {
        JsonNode inicial = abrir(assunto, 201);
        String rota = BASE + "/" + inicial.at("/sessao/id").asLong();
        assertEquals(inicial, abrir(assunto, 200));
        abrir(vazio, 409);
        JsonNode segunda = call(put(rota + "/questoes/2"), token, 200);
        JsonNode pausa = call(put(rota + "/interromper"), token, 200);
        assertEquals("INTERROMPIDA", pausa.at("/sessao/situacao").asText());
        assertEquals(pausa, call(get(rota), token, 200));
        assertEquals(pausa, call(put(rota + "/interromper"), token, 200));
        call(put(rota + "/questoes/3"), token, 409);
        JsonNode retomada = call(put(rota + "/retomar"), token, 200);
        assertEquals("ATIVA", retomada.at("/sessao/situacao").asText());
        assertEquals(2, retomada.at("/sessao/questoesApresentadas").asInt());
        assertEquals(segunda.get("questaoAtual"), retomada.get("questaoAtual"));
        call(put(rota + "/interromper"), token, 200);
        JsonNode fim = call(put(rota + "/encerrar"), token, 200);
        assertEquals("ENCERRADA", fim.at("/sessao/situacao").asText());
        assertEquals(fim, call(put(rota + "/encerrar"), token, 200));
        call(put(rota + "/retomar"), token, 409);
        call(put(rota + "/questoes/3"), token, 409);
        assertNotEquals(inicial.at("/sessao/id"), abrir(assunto, 201).at("/sessao/id"));
    }

    @Test
    void consultaSessaoLegadaSemQuestoesEPermiteEncerrar() throws Exception {
        long sessao = inserirSessao(assunto, "ATIVA");
        String rota = BASE + "/" + sessao;

        var estado = call(get(rota), token, 200);
        assertTrue(estado.get("questaoAtual").isNull());
        assertFalse(estado.has("feedback"));
        assertEquals(0, estado.get("pendencias").asInt());
        assertEquals("ENCERRADA", call(put(rota + "/encerrar"), token, 200).at("/sessao/situacao").asText());
    }

    @Test
    void rejeitaRespostaComTitularDivergenteEAlteracaoDePassoOcupado() throws Exception {
        cadastrar("outro");
        var estado = abrir(assunto, 201);
        long sessao = estado.at("/sessao/id").asLong();
        long questao = estado.at("/questaoAtual/id").asLong();
        assertThrows(DataIntegrityViolationException.class, () -> db.update("""
                insert into respostas_aluno (aluno_id, sessao_id, questao_id, passo, resposta_dada, correta, reaprender)
                values (?, ?, ?, 1, 'B', 0, 1)
                """, usuarios.getLast(), sessao, questao));

        var consultada = call(get(BASE + "/" + sessao), token, 200);
        assertFalse(consultada.has("feedback"));
        assertEquals(0, consultada.get("pendencias").asInt());
        assertEquals(0, call(get(BASE + "/" + sessao + "/resultado"), token, 200).at("/sessao/tentativas").asInt());
        call(put(rotaResposta(estado)).content(body(Map.of("alternativa", "B", "passo", 1))), token, 200);
        call(put(rotaResposta(estado)).content(body(Map.of("alternativa", "A", "passo", 1))), token, 409);
        assertEquals(1, call(get(BASE + "/" + sessao + "/resultado"), token, 200).at("/sessao/tentativas").asInt());
        assertEquals(1, id("select count(*) from respostas_aluno where sessao_id=?", sessao));
    }

    @Test
    void recomendacaoUsaNivelDoTitularELimitaADezQuestoesUnicas() throws Exception {
        db.update("update desempenho_por_assunto set nivel_atual=8 where aluno_id=? and assunto_id<>?",
                usuarios.getFirst(), assunto);
        JsonNode atual = call(post(BASE).content("{}"), token, 201);
        assertEquals(assunto, atual.at("/sessao/assuntoId").asLong());
        String rota = BASE + "/" + atual.at("/sessao/id").asLong();
        call(put(rota + "/questoes/3"), token, 409);
        Set<Long> ids = new HashSet<>();
        ids.add(atual.at("/questaoAtual/id").asLong());
        for (int posicao = 2; posicao <= 10; posicao++) {
            atual = call(put(rota + "/questoes/" + posicao), token, 200);
            assertTrue(ids.add(atual.at("/questaoAtual/id").asLong()));
            assertEquals(assunto, atual.at("/questaoAtual/assuntoId").asLong());
            assertEquals(posicao, atual.at("/sessao/questoesApresentadas").asInt());
            assertFalse(atual.get("questaoAtual").has("respostaCorreta"));
            assertFalse(atual.get("questaoAtual").has("explicacao"));
        }
        assertEquals(atual, call(put(rota + "/questoes/10"), token, 200));
        call(put(rota + "/questoes/11"), token, 409);
        assertEquals(10, id("select count(*) from sessao_questoes where sessao_id=?", atual.at("/sessao/id").asLong()));
        assertEquals(0, id("select count(*) from respostas_aluno where aluno_id=?", usuarios.getFirst()));
    }

    @Test
    void assuntoSemConteudoNaoCriaSessaoEAssuntoEsgotadoNaoTroca() throws Exception {
        abrir(vazio, 409);
        abrir(Long.MAX_VALUE, 404);
        assertEquals(0, call(get(BASE), token, 200).size());
        db.update("delete from questoes where atividade_id=? and id<>(select min(id) from questoes where atividade_id=?)",
                atividade, atividade);
        JsonNode inicio = abrir(assunto, 201);
        String rota = BASE + "/" + inicio.at("/sessao/id").asLong();
        call(put(rota + "/questoes/2"), token, 409);
        assertEquals(inicio, call(get(rota), token, 200));
    }

    @Test
    void requisicoesSimultaneasNaoDuplicamSessaoNemPosicao() throws Exception {
        try (var pool = Executors.newFixedThreadPool(4)) {
            var aberturas = IntStream.range(0,4).<Callable<JsonNode>>mapToObj(i -> () ->
                    call(post(BASE).content(body(Map.of("assuntoId", assunto))), token, 200, 201)).toList();
            Set<Long> sessoes = new HashSet<>();
            for (var futuro : pool.invokeAll(aberturas)) sessoes.add(futuro.get().at("/sessao/id").asLong());
            assertEquals(1, sessoes.size());
            String rota = BASE + "/" + sessoes.iterator().next() + "/questoes/2";
            var avancos = IntStream.range(0,4).<Callable<JsonNode>>mapToObj(i -> () -> call(put(rota), token, 200)).toList();
            Set<Long> ids = new HashSet<>();
            for (var futuro : pool.invokeAll(avancos)) ids.add(futuro.get().at("/questaoAtual/id").asLong());
            assertEquals(1, ids.size());
            assertEquals(2, call(get(BASE), token, 200).get(0).get("questoesApresentadas").asInt());
        }
    }

    @Test
    void respostaExigeAlternativaValidaEQuestaoAtualDaSessaoAtiva() throws Exception {
        JsonNode inicio = abrir(assunto, 201);
        long sessao = inicio.at("/sessao/id").asLong();
        long primeira = inicio.at("/questaoAtual/id").asLong();
        String rota = BASE + "/" + sessao;
        String primeiraRota = rota + "/questoes/" + primeira + "/resposta";
        for (String invalida : List.of("", "{}", "{", "{\"alternativa\":null}", "{\"alternativa\":[]}",
                body(Map.of("alternativa", "E")), body(Map.of("alternativa", "AB")), body(Map.of("alternativa", "a")))) {
            call(put(primeiraRota).content(invalida), token, 400);
        }
        call(put(rota + "/questoes/invalida/resposta").content(body(Map.of("alternativa", "B"))), token, 400);
        long fora = id("select max(id) from questoes where atividade_id=? and id<>?", atividade, primeira);
        String escolha = body(Map.of("alternativa", "B"));
        call(put(rota + "/questoes/" + fora + "/resposta").content(escolha), token, 404);
        db.update("update questoes set alternativa_b=null where id=?", primeira);
        call(put(primeiraRota).content(escolha), token, 400);
        call(put(rota + "/interromper"), token, 200);
        call(put(primeiraRota).content(escolha), token, 409);
        call(put(rota + "/retomar"), token, 200);
        long segunda = call(put(rota + "/questoes/2"), token, 200).at("/questaoAtual/id").asLong();
        call(put(primeiraRota).content(escolha), token, 409);
        call(put(rota + "/encerrar"), token, 200);
        call(put(rota + "/questoes/" + segunda + "/resposta").content(escolha), token, 409);
        assertEquals(0, id("select count(*) from respostas_aluno where sessao_id=?", sessao));
        assertEquals(0, call(get(rota), token, 200).get("pontosSessao").asLong());
    }

    @Test
    void corrigeNoServidorERecuperaFeedbackSomenteDepoisDoEnvio() throws Exception {
        JsonNode inicio = abrir(assunto, 201);
        long sessao = inicio.at("/sessao/id").asLong();
        String rota = BASE + "/" + sessao;
        long q1 = inicio.at("/questaoAtual/id").asLong();
        String resposta = rota + "/questoes/" + q1 + "/resposta";
        assertFalse(inicio.has("feedback"));
        assertFalse(call(get(rota), token, 200).toString().contains("explicacao"));
        assertFalse(inicio.get("questaoAtual").has("respostaCorreta"));
        var incorreta = call(put(resposta).content(body(Map.of("alternativa","C", "correta",true))), token, 200);
        assertTrue(incorreta.get("reaprender").asBoolean());
        assertFalse(incorreta.get("correta").asBoolean());
        assertEquals(0, incorreta.get("pontosConcedidos").asInt());
        assertNotEquals("Explicação", incorreta.get("explicacao").asText());
        assertNiveis(incorreta, 1, 1);
        assertEquals(incorreta, call(get(rota), token, 200).get("feedback"));
        assertEquals(1, id("""
                select count(*) from respostas_aluno where aluno_id=? and sessao_id=? and questao_id=?
                and resposta_dada='C' and correta=0
                """, usuarios.getFirst(), sessao, q1));
        JsonNode proxima = call(put(rota + "/questoes/2"), token, 200);
        assertFalse(proxima.has("feedback"));
        assertFalse(call(get(rota), token, 200).has("feedback"));
        String segundaRota = rota + "/questoes/" + proxima.at("/questaoAtual/id").asLong() + "/resposta";
        assertTrue(call(put(segundaRota).content(body(Map.of("alternativa","A"))), token, 200).get("correta").asBoolean());
        call(put(rota + "/encerrar"), token, 409);
        assertEquals(incorreta, call(put(resposta).content(body(Map.of("alternativa","C"))), token, 200));
        call(put(resposta).content(body(Map.of("alternativa","A"))), token, 409);
        assertEquals(2, id("select count(*) from respostas_aluno where sessao_id=?", sessao));
    }

    @Test
    void enviosConcorrentesPreservamUmaUnicaResposta() throws Exception {
        JsonNode inicio = abrir(assunto, 201);
        long sessao = inicio.at("/sessao/id").asLong();
        String rota = BASE + "/" + sessao + "/questoes/" + inicio.at("/questaoAtual/id").asLong() + "/resposta";
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<Callable<JsonNode>> pedidos = new ArrayList<>();
            for (String letra : List.of("D", "B", "D", "B")) {
                pedidos.add(() -> call(put(rota).content(body(Map.of("alternativa", letra))), token, 200, 409));
            }
            var retornos = pool.invokeAll(pedidos);
            String gravada = db.queryForObject("select resposta_dada from respostas_aluno where sessao_id=?", String.class, sessao);
            for (int i = 0; i < retornos.size(); i++) {
                JsonNode retorno = retornos.get(i).get();
                if (gravada.equals(i % 2 == 0 ? "D" : "B")) assertEquals(gravada, retorno.get("alternativa").asText());
                else assertEquals(409, retorno.get("status").asInt());
            }
        }
        assertEquals(1, id("select count(*) from respostas_aluno where sessao_id=?", sessao));
        assertEquals(1, id("select total_erros from desempenho_por_assunto where aluno_id=? and assunto_id=?",
                usuarios.getFirst(), assunto));
    }

    @Test
    void reenviosConcorrentesConservamPontosOriginaisAposEdicaoEPausa() throws Exception {
        var estado = abrir(assunto, 201);
        long sessao = estado.at("/sessao/id").asLong();
        long questao = estado.at("/questaoAtual/id").asLong();
        String rota = BASE + "/" + sessao;
        db.update("update questoes set dificuldade=10 where id=?", questao);
        String pedido = body(Map.of("alternativa", "A", "passo", 1, "pontosConcedidos", 999));
        try (var pool = Executors.newFixedThreadPool(4)) {
            var envios = IntStream.range(0, 4).<Callable<JsonNode>>mapToObj(i -> () ->
                    call(put(rotaResposta(estado)).content(pedido), token, 200)).toList();
            for (var retorno : pool.invokeAll(envios)) {
                assertEquals(100, retorno.get().get("pontosConcedidos").asInt());
            }
        }
        assertEquals(1, id("select count(*) from respostas_aluno where sessao_id=?", sessao));
        assertEquals(100, id("select pontos_individuais from respostas_aluno where sessao_id=?", sessao));
        db.update("update questoes set dificuldade=1 where id=?", questao);
        assertEquals(100, call(put(rota + "/interromper"), token, 200).get("pontosSessao").asLong());
        assertEquals(100, call(get(rota), token, 200).get("pontosSessao").asLong());
        assertEquals(100, call(put(rota + "/retomar"), token, 200).at("/feedback/pontosConcedidos").asInt());
        assertEquals(100, call(put(rotaResposta(estado)).content(pedido), token, 200).get("pontosConcedidos").asInt());
        assertEquals(100, call(get(rota), token, 200).get("pontosSessao").asLong());
    }

    @Test
    void consultaUltimaRespostaDeOutraSessaoMesmoSemAgendamento() throws Exception {
        long aluno = usuarios.getFirst();
        long questao = id("select min(id) from questoes where atividade_id=?", atividade);
        db.update("delete from questoes where atividade_id=? and id<>?", atividade, questao);
        db.update("update questoes set dificuldade=5 where id=?", questao);
        for (boolean correta : List.of(true, false)) {
            long legada = inserirSessao(assunto, "ENCERRADA");
            db.update("insert into sessao_questoes(sessao_id, indice, questao_id) values (?,0,?)", legada, questao);
            db.update("""
                    insert into respostas_aluno(aluno_id, sessao_id, questao_id, passo, resposta_dada, correta, respondido_em)
                    values (?,?,?,1,?,?,'2000-01-01T12:00:00')
                    """, aluno, legada, questao, correta ? "A" : "B", correta);
        }
        assertEquals(0, id("select count(*) from repeticao_espacada where aluno_id=?", aluno));
        var estado = abrir(assunto, 201);
        var acerto = body(Map.of("alternativa", "A", "passo", 1));
        assertEquals(30, call(put(rotaResposta(estado)).content(acerto), token, 200).get("pontosConcedidos").asInt());
        assertEquals(30, id("select sum(pontos_individuais) from respostas_aluno where aluno_id=?", aluno));

        String colega = cadastrar("outro-pontos");
        var estadoColega = call(post(BASE).content(body(Map.of("assuntoId", assunto))), colega, 201);
        assertEquals(50, call(put(rotaResposta(estadoColega)).content(acerto), colega, 200).get("pontosConcedidos").asInt());
        call(get(BASE + "/" + estado.at("/sessao/id").asLong()), colega, 404);
    }

    @Test
    void evolucaoIsoladaProtegePrimeiroErroEConservaHistorico() throws Exception {
        long aluno = usuarios.getFirst();
        cadastrar("colega");
        long turma = id("insert into turmas(nome) output inserted.id values (?)", prefixo);
        try {
            db.update("update alunos set turma_id=? where usuario_id=?", turma, aluno);
            db.update("insert into pontuacao_coletiva(turma_id,pontos_totais) values (?,37)", turma);
            var global = (ObjectNode) call(get("/api/aluno/desempenho"), token, 200);
            assertEquals(0, global.remove("pontosIndividuais").asLong());
            db.update("update desempenho_por_assunto set nivel_atual=6 where aluno_id=? and assunto_id=?", aluno, assunto);
            JsonNode estado = abrir(assunto, 201);
            String sessao = BASE + "/" + estado.at("/sessao/id").asLong();
            String primeira = rotaResposta(estado);
            var erro = body(Map.of("alternativa","C"));
            var protegido = call(put(primeira).content(erro), token, 200);
            assertNiveis(protegido, 6, 6);
            assertEquals(protegido, call(put(primeira).content(erro), token, 200));
            call(put(primeira).content(body(Map.of("alternativa","A"))), token, 409);
            estado = call(put(sessao + "/questoes/2"), token, 200);
            assertNiveis(call(put(rotaResposta(estado)).content(erro), token, 200), 6, 5);
            assertEquals(protegido, call(put(primeira).content(erro), token, 200));
            assertEquals(2, id("select total_erros from desempenho_por_assunto where aluno_id=? and assunto_id=?", aluno, assunto));
            assertEquals(0, id("""
                    select count(*) from desempenho_por_assunto where (aluno_id=? or aluno_id=?)
                    and (aluno_id<>? or assunto_id<>?) and (nivel_atual<>1 or erros_consecutivos+total_erros+total_acertos<>0)
                    """, aluno, usuarios.getLast(), aluno, assunto));
            estado = call(put(sessao + "/questoes/3"), token, 200);
            var acerto = call(put(rotaResposta(estado)).content(body(Map.of("alternativa", "A"))), token, 200);
            assertNiveis(acerto, 5, 6);
            assertEquals(10, acerto.get("pontosConcedidos").asInt());
            var atualizado = (ObjectNode) call(get("/api/aluno/desempenho"), token, 200);
            assertEquals(10, atualizado.remove("pontosIndividuais").asLong());
            assertEquals(global, atualizado);
            assertEquals(37, id("select pontos_totais from pontuacao_coletiva where turma_id=?", turma));
        } finally {
            db.update("update alunos set turma_id=null where turma_id=?", turma);
            db.update("delete from turmas where id=?", turma);
        }
    }

    @Test
    void inicializaDesempenhoAusenteEIgnoraRespostaLegadaSemNiveis() throws Exception {
        long aluno = usuarios.getFirst();
        db.update("delete from desempenho_por_assunto where aluno_id=? and assunto_id=?", aluno, assunto);
        var estado = abrir(assunto, 201);
        long sessao = estado.at("/sessao/id").asLong();
        db.update("""
                insert into respostas_aluno(aluno_id,sessao_id,questao_id,passo,resposta_dada,correta)
                values (?,?,?,1,'A',1)
                """, aluno, sessao, estado.at("/questaoAtual/id").asLong());
        var acerto = body(Map.of("alternativa","A"));
        var legado = call(put(rotaResposta(estado)).content(acerto), token, 200);
        assertFalse(legado.has("nivelAnterior"));
        assertFalse(legado.has("nivelAtual"));
        assertFalse(legado.has("pontosConcedidos"));
        assertEquals(0, id("select count(*) from desempenho_por_assunto where aluno_id=? and assunto_id=?", aluno, assunto));
        estado = call(put(BASE + "/" + sessao + "/questoes/2"), token, 200);
        assertNiveis(call(put(rotaResposta(estado)).content(acerto), token, 200), 1, 2);
        assertEquals(1, id("select total_acertos from desempenho_por_assunto where aluno_id=? and assunto_id=?", aluno, assunto));
    }

    @Test
    void falhaNaGravacaoDoDesempenhoReverteTambemAResposta() throws Exception {
        long aluno = usuarios.getFirst();
        // Força a restrição de total não negativo a falhar no flush da entidade.
        db.update("update desempenho_por_assunto set total_acertos=? where aluno_id=? and assunto_id=?",
                Integer.MAX_VALUE, aluno, assunto);
        var estado = abrir(assunto, 201);
        call(put(rotaResposta(estado)).content(body(Map.of("alternativa","A"))), token, 500);
        assertEquals(0, id("select count(*) from respostas_aluno where aluno_id=?", aluno));
        assertEquals(0, call(get(BASE + "/" + estado.at("/sessao/id").asLong()), token, 200)
                .get("pontosSessao").asLong());
        assertEquals(1, id("select nivel_atual from desempenho_por_assunto where aluno_id=? and assunto_id=?", aluno, assunto));
        assertEquals(0, id("select count(*) from repeticao_espacada where aluno_id=?", aluno));
        assertEquals(Integer.MAX_VALUE, id("select total_acertos from desempenho_por_assunto where aluno_id=? and assunto_id=?", aluno, assunto));
    }

    @Test
    void agendaPorQuestaoRespeitaViradaLocalIntervalosEReenvio() throws Exception {
        db.update("delete from questoes where atividade_id=? and id<>(select min(id) from questoes where atividade_id=?)",
                atividade, atividade);
        String colega = cadastrar("outra-conta");
        LocalDate dia = LocalDate.of(2026, 12, 31); // 02:59:59 UTC de 1º de janeiro
        var estado = abrir(assunto, 201);
        long questao = estado.at("/questaoAtual/id").asLong();
        int[] intervalos = {1, 3, 7, 16, 35, 70, 70, 1};
        for (int i = 0; i < intervalos.length; i++) {
            int dias = intervalos[i];
            String alternativa = "A";
            String rota = rotaResposta(estado);
            if (i == intervalos.length - 1) {
                call(put(rota).content(body(Map.of("alternativa","B", "passo",1))), token, 200);
                var semAgenda = call(get(AGENDA), token, 200);
                assertTrue(semAgenda.get("pendentes").isEmpty());
                assertTrue(semAgenda.get("futuras").isEmpty());
                estado = call(put(BASE + "/" + estado.at("/sessao/id").asLong() + "/questoes/2"), token, 200);
                assertTrue(estado.get("repetindo").asBoolean());
            }
            var resposta = body(Map.of("alternativa", alternativa, "passo",estado.get("passo").asInt()));
            var feedback = call(put(rota).content(resposta), token, 200);
            int esperados = i == 0 ? 10 : i == intervalos.length - 1 ? 6 : 3;
            assertEquals(esperados, feedback.get("pontosConcedidos").asInt());
            var agenda = call(get(AGENDA), token, 200);
            assertEquals(dia.toString(), agenda.get("hoje").asText());
            assertEquals(0, agenda.get("pendentes").size());
            assertEquals(1, agenda.get("futuras").size());
            var futura = agenda.at("/futuras/0");
            LocalDate vence = dia.plusDays(dias);
            assertEquals(vence.toString(), futura.get("proximaRevisao").asText());
            assertEquals(dias, futura.get("intervaloDias").asInt());
            assertEquals(questao, futura.get("questaoId").asLong());
            assertFalse(futura.has("respostaCorreta"));
            assertFalse(futura.has("explicacao"));
            call(put(BASE + "/" + estado.at("/sessao/id").asLong() + "/encerrar"), token, 200);
            abrir(assunto, 409);
            if (i == 0) call(post(BASE).content("{}"), token, 409);
            when(relogio.instant()).thenReturn(vence.atStartOfDay(FUSO).toInstant());
            if (i == 0) assertEquals("2027-01-01T03:00:00Z", agenda.get("proximaVirada").asText());
            call(put(rota).content(resposta), token, 200); // repetir o envio no outro dia preserva o vencimento
            var vencida = call(get(AGENDA), token, 200);
            assertEquals(futura, vencida.at("/pendentes/0"));
            assertEquals(0, vencida.get("futuras").size());
            assertEquals(0, call(get(AGENDA), colega, 200).get("pendentes").size());
            if (i < intervalos.length - 1) {
                estado = abrir(assunto, 201);
                assertEquals(questao, estado.at("/questaoAtual/id").asLong());
            }
            dia = vence;
        }
    }

    @Test
    void apresentaNoMaximoTresRevisoesMesmoAposRetomarSemMisturarAgendaLegada() throws Exception {
        long aluno = usuarios.getFirst();
        db.update("""
                insert into repeticao_espacada(aluno_id,atividade_id,proxima_revisao)
                values (?,?,'2026-01-01')
                """, aluno, atividade);
        assertEquals(0, call(get(AGENDA), token, 200).get("pendentes").size());
        var ids = db.queryForList("select top (4) id from questoes where atividade_id=? order by id", Long.class, atividade);
        for (var q : ids) {
            db.update("insert into repeticao_espacada(aluno_id,questao_id,proxima_revisao) values (?,?,'2026-12-01')", aluno, q);
        }
        var estado = abrir(assunto, 201);
        String rota = BASE + "/" + estado.at("/sessao/id").asLong();
        assertTrue(ids.contains(estado.at("/questaoAtual/id").asLong()));
        for (int n = 2; n <= 3; n++) {
            var proxima = call(put(rota + "/questoes/" + n), token, 200);
            assertTrue(ids.contains(proxima.at("/questaoAtual/id").asLong()));
            assertEquals(proxima, call(put(rota + "/questoes/" + n), token, 200));
        }
        call(put(rota + "/interromper"), token, 200);
        call(put(rota + "/retomar"), token, 200);
        assertFalse(ids.contains(call(put(rota + "/questoes/4"), token, 200).at("/questaoAtual/id").asLong()));
        assertEquals(4, call(get(AGENDA), token, 200).get("pendentes").size());
    }

    @Test
    void primeiraSessaoExibeSomenteResultadosGravadosEAgendaDoTitular() throws Exception {
        assertTrue(call(get(BASE + "/historico"), token, 200).get("sessoes").isEmpty());
        var estado = abrir(assunto, 201);
        String rota = BASE + "/" + estado.at("/sessao/id").asLong();
        long primeira = estado.at("/questaoAtual/id").asLong();
        call(put(rotaResposta(estado)).content(body(Map.of("alternativa","A"))), token, 200);
        estado = call(put(rota + "/questoes/2"), token, 200);
        call(put(rotaResposta(estado)).content(body(Map.of("alternativa","B"))), token, 200);
        call(put(rota + "/interromper"), token, 200);
        var resultado = call(get(rota + "/resultado"), token, 200);
        var indicadores = resultado.get("sessao");
        assertTrue(resultado.get("anterior").isNull());
        assertEquals(1, indicadores.get("acertos").asInt());
        assertEquals(1, indicadores.get("erros").asInt());
        assertEquals(1, indicadores.get("nivelAntes").asInt());
        assertEquals(2, indicadores.get("nivelDepois").asInt());
        assertEquals("INTERROMPIDA", indicadores.get("situacao").asText());
        assertFalse(indicadores.has("pontosPrivados"));
        assertEquals(2, resultado.get("questoes").size());
        assertTrue(resultado.at("/questoes/0/correta").isBoolean());
        assertTrue(resultado.at("/questoes/0/correta").asBoolean());
        assertEquals("A", resultado.at("/questoes/0/alternativa").asText());
        assertEquals(1, resultado.at("/questoes/0/numero").asInt());
        assertTrue(resultado.at("/questoes/1/correta").isBoolean());
        assertFalse(resultado.at("/questoes/1/correta").asBoolean());
        assertEquals("B", resultado.at("/questoes/1/alternativa").asText());
        assertFalse(resultado.at("/questoes/1").has("respostaCorreta"));
        assertEquals(1, resultado.get("revisoes").size());
        assertEquals(primeira, resultado.at("/revisoes/0/questaoId").asLong());
        assertEquals("2027-01-01", resultado.at("/revisoes/0/proximaRevisao").asText());
        assertFalse(resultado.at("/revisoes/0").has("respostaCorreta"));
        assertEquals(indicadores, call(get(BASE + "/historico"), token, 200).at("/sessoes/0"));
        // Uma resposta anterior à V7 não tem níveis históricos. Não emprestar o nível da segunda.
        db.update("update respostas_aluno set nivel_anterior=null, nivel_atual=null where sessao_id=? and questao_id=?",
                indicadores.get("id").asLong(), primeira);
        assertTrue(call(get(rota + "/resultado"), token, 200).at("/sessao/nivelAntes").isNull());
    }

    @Test
    void interrompidaPreservaResultadoParcialSemContarQuestoesEmBrancoComoErros() throws Exception {
        var estado = abrir(assunto, 201);
        String rota = BASE + "/" + estado.at("/sessao/id").asLong();
        var vazio = call(get(rota + "/resultado"), token, 200);
        assertTrue(vazio.at("/sessao/nivelAntes").isNull());
        assertTrue(vazio.at("/sessao/nivelDepois").isNull());
        assertTrue(vazio.get("revisoes").isEmpty());
        assertTrue(vazio.at("/questoes/0/correta").isNull());
        assertTrue(vazio.at("/questoes/0/alternativa").isNull());
        call(put(rotaResposta(estado)).content(body(Map.of("alternativa","B"))), token, 200);
        call(put(rota + "/questoes/2"), token, 200);
        call(put(rota + "/interromper"), token, 200);
        var resultado = call(get(rota + "/resultado"), token, 200);
        assertEquals("INTERROMPIDA", resultado.at("/sessao/situacao").asText());
        assertEquals(2, resultado.at("/sessao/apresentadas").asInt());
        assertEquals(0, resultado.at("/sessao/acertos").asInt());
        assertEquals(1, resultado.at("/sessao/erros").asInt());
        assertTrue(resultado.get("revisoes").isEmpty());
        assertEquals(resultado, call(get(rota + "/resultado"), token, 200));
    }

    @Test
    void historicoEComparacaoIgnoramSessoesDeOutrosAlunos() throws Exception {
        var primeira = abrir(assunto, 201);
        long id1 = primeira.at("/sessao/id").asLong();
        call(put(rotaResposta(primeira)).content(body(Map.of("alternativa","A"))), token, 200);
        call(put(BASE + "/" + id1 + "/encerrar"), token, 200);
        String outro = cadastrar("outro-historico");
        var alheia = call(post(BASE).content(body(Map.of("assuntoId", assunto))), outro, 201);
        long outroId = alheia.at("/sessao/id").asLong();
        call(put(BASE + "/" + outroId + "/encerrar"), outro, 200);
        var segunda = abrir(assunto, 201);
        long id2 = segunda.at("/sessao/id").asLong();
        assertTrue(call(get(BASE + "/" + id2 + "/resultado"), token, 200).get("anterior").isNull());
        call(put(BASE + "/" + id2 + "/interromper"), token, 200);
        var parcial = call(get(BASE + "/" + id2 + "/resultado"), token, 200);
        assertTrue(parcial.get("anterior").isNull());
        assertEquals(1, parcial.at("/sessao/apresentadas").asInt());
        call(put(BASE + "/" + id2 + "/encerrar"), token, 200);
        var resultado = call(get(BASE + "/" + id2 + "/resultado"), token, 200);
        assertEquals(id1, resultado.at("/anterior/id").asLong());
        assertEquals(1, resultado.at("/anterior/acertos").asInt());
        assertEquals(0, resultado.at("/sessao/acertos").asInt());
        assertTrue(call(get(BASE + "/" + outroId + "/resultado"), outro, 200).get("anterior").isNull());
        call(get(BASE + "/" + id1 + "/resultado"), outro, 404);
        call(get(BASE + "/" + outroId + "/resultado"), token, 404);
        call(get(BASE + "/" + Long.MAX_VALUE + "/resultado"), outro, 404);
        var lista = call(get(BASE + "/historico"), token, 200).get("sessoes");
        assertEquals(2, lista.size());
        assertEquals(id2, lista.get(0).get("id").asLong());
        assertEquals(id1, lista.get(1).get("id").asLong());
        assertEquals(1, call(get(BASE + "/historico"), outro, 200).get("sessoes").size());
        for (var caminho : List.of(BASE + "/historico", BASE + "/" + id1 + "/resultado")) {
            int status = mvc.perform(get(caminho)).andReturn().getResponse().getStatus();
            assertTrue(status == 403 || status == 401);
        }
    }

    private long inserirSessao(long assuntoId, String situacao) {
        return id("""
                insert into sessoes_estudo(aluno_id, assunto_id, situacao, iniciada_em, atualizada_em)
                output inserted.id values (?, ?, ?, sysdatetimeoffset(), sysdatetimeoffset())
                """, usuarios.getFirst(), assuntoId, situacao);
    }

    @Test
    void comparaPorIdDeAssuntoAlemDaPrimeiraPagina() throws Exception {
        inserirSessao(assunto, "ENCERRADA");
        long anterior = inserirSessao(assunto, "ENCERRADA");
        long outraMateria = id("insert into materias(nome) output inserted.id values (?)", prefixo + "-outra");
        try {
            long homonimo = id("insert into assuntos(materia_id,nome) output inserted.id values (?, 'Assunto teste')", outraMateria);
            for (int n = 0; n < 25; n++) inserirSessao(homonimo, "ENCERRADA");
            long atual = inserirSessao(assunto, "ENCERRADA");
            inserirSessao(assunto, "ENCERRADA");
            assertEquals(anterior, call(get(BASE + "/" + atual + "/resultado"), token, 200).at("/anterior/id").asLong());
        } finally {
            db.update("delete from sessoes_estudo where assunto_id in (select id from assuntos where materia_id=?)", outraMateria);
            db.update("delete from assuntos where materia_id=?", outraMateria);
            db.update("delete from materias where id=?", outraMateria);
        }
    }

    @Test
    void primeiraConclusaoIgnoraSessoesIncompletasDeQualquerAssunto() throws Exception {
        for (long anterior : List.of(vazio, assunto)) {
            for (String situacao : List.of("ATIVA", "INTERROMPIDA")) {
                long parcial = inserirSessao(anterior, situacao);
                long atual = inserirSessao(assunto, "ENCERRADA");
                assertTrue(call(get(BASE + "/" + atual + "/resultado"), token, 200).get("anterior").isNull());
                db.update("delete from sessoes_estudo where id in (?, ?)", parcial, atual);
            }
        }
    }

    @Test
    void paginaHistoricoSemRepetirSessoes() throws Exception {
        for (int i = 0; i < 21; i++) inserirSessao(assunto, "ENCERRADA");
        var pagina = call(get(BASE + "/historico"), token, 200);
        assertEquals(20, pagina.get("sessoes").size());
        long cursor = pagina.get("proximoCursor").asLong();
        assertEquals(cursor, pagina.at("/sessoes/19/id").asLong());
        var ultima = call(get(BASE + "/historico").param("antesDe", Long.toString(cursor)), token, 200);
        assertEquals(1, ultima.get("sessoes").size());
        assertTrue(ultima.at("/sessoes/0/id").asLong() < cursor);
        assertTrue(ultima.get("proximoCursor").isNull());
    }

    @Test
    void intercalaDuasPendenciasSemInflarQuestoesOuDesempenho() throws Exception {
        var estado = abrir(assunto, 201);
        long sessao = estado.at("/sessao/id").asLong();
        String rota = BASE + "/" + sessao;
        List<Long> vistas = new ArrayList<>();
        for (int passo = 1; passo <= 14; passo++) {
            long questao = estado.at("/questaoAtual/id").asLong();
            boolean repetida = vistas.contains(questao);
            assertEquals(repetida, estado.get("repetindo").asBoolean());
            if (!repetida) vistas.add(questao);
            assertEquals(vistas.size(), estado.at("/sessao/questoesApresentadas").asInt());
            assertTrue(vistas.size() <= 10);
            // Dois erros iniciais, e a primeira questão erra mais duas vezes.
            if (passo == 4 || passo == 7 || passo == 10) assertEquals(vistas.getFirst(), questao);
            if (passo == 5) assertEquals(vistas.get(1), questao);
            boolean errar = Set.of(1, 2, 4, 7).contains(passo);
            var pedido = body(Map.of("alternativa", errar ? "B" : "A", "passo", passo));
            var feedback = call(put(rotaResposta(estado)).content(pedido), token, 200);
            assertEquals(feedback, call(put(rotaResposta(estado)).content(pedido), token, 200));
            if (passo <= 2) assertEquals("DICA_INICIAL", feedback.get("tipoOrientacao").asText());
            if (passo == 4) assertEquals("DICA_APRIMORADA", feedback.get("tipoOrientacao").asText());
            if (passo == 5) {
                assertEquals("EXPLICACAO", feedback.get("tipoOrientacao").asText());
                assertFalse(feedback.has("alternativaCorreta"));
            }
            if (passo == 7) assertEquals("RESOLUCAO", feedback.get("tipoOrientacao").asText());
            if (passo == 4) {
                // O reenvio do passo 1 chega quando a questão já reapareceu.
                call(put(rotaResposta(estado)).content(body(Map.of("alternativa","A", "passo",1))), token, 409);
                var pausa = call(put(rota + "/interromper"), token, 200);
                assertEquals(2, pausa.get("pendencias").asInt());
                assertEquals(pausa, call(get(rota), token, 200));
                call(put(rota + "/encerrar"), token, 409);
                assertEquals(feedback, call(put(rota + "/retomar"), token, 200).get("feedback"));
            }
            if (passo < 14) {
                estado = call(put(rota + "/questoes/" + (passo + 1)), token, 200);
                assertFalse(estado.has("feedback"));
            }
        }
        assertEquals(10, vistas.size());
        call(put(rota + "/questoes/15"), token, 409);
        assertEquals(0, call(put(rota + "/encerrar"), token, 200).get("pendencias").asInt());
        db.update("update respostas_aluno set respondido_em=? where sessao_id=?", LocalDate.of(2026, 12, 31).atTime(12, 0), sessao);
        var resultado = call(get(rota + "/resultado"), token, 200);
        assertEquals(10, resultado.at("/sessao/apresentadas").asInt());
        assertEquals(10, resultado.at("/sessao/respondidas").asInt());
        assertEquals(10, resultado.at("/sessao/acertos").asInt());
        assertEquals(14, resultado.at("/sessao/tentativas").asInt());
        assertEquals(4, resultado.at("/sessao/tentativasIncorretas").asInt());
        assertEquals(2, resultado.at("/sessao/erros").asInt());
        assertEquals(14, resultado.get("questoes").size());
        long seguinte = inserirSessao(assunto, "ENCERRADA");
        assertEquals(resultado.get("sessao"), call(get(BASE + "/" + seguinte + "/resultado"), token, 200).get("anterior"));
        assertEquals(10, resultado.get("revisoes").size());
        assertEquals(8, id("select total_acertos from desempenho_por_assunto where aluno_id=? and assunto_id=?",
                usuarios.getFirst(), assunto));
        assertEquals(2, id("select total_erros from desempenho_por_assunto where aluno_id=? and assunto_id=?",
                usuarios.getFirst(), assunto));
    }

    @Test
    void revelaDepoisDeTresErrosSemResolverAPendenciaEReiniciaContagemEmOutraSessao() throws Exception {
        db.update("""
                delete from questoes where atividade_id = ?
                and id <> (select min(id) from questoes where atividade_id = ?)
                """, atividade, atividade);
        db.update("update desempenho_por_assunto set nivel_atual=6 where aluno_id=? and assunto_id=?",
                usuarios.getFirst(), assunto);

        var estado = abrir(assunto, 201);
        long sessao = estado.at("/sessao/id").asLong();
        String rota = BASE + "/" + sessao;
        String urlResposta = rotaResposta(estado);
        var orientacoes = List.of("DICA_INICIAL", "DICA_APRIMORADA", "RESOLUCAO", "RESOLUCAO");
        JsonNode primeira = null;
        for (int passo = 1; passo <= 4; passo++) {
            assertEquals(urlResposta, rotaResposta(estado));
            assertFalse(estado.has("feedback"));
            var pedido = body(Map.of("alternativa", "B", "passo", passo, "errosDaQuestao", 99));
            var feedback = call(put(urlResposta).content(pedido), token, 200);
            assertEquals(orientacoes.get(passo - 1), feedback.get("tipoOrientacao").asText());
            assertEquals(passo, feedback.get("errosDaQuestao").asInt());
            assertFalse(feedback.get("correta").asBoolean());
            assertTrue(feedback.get("reaprender").asBoolean());
            assertNiveis(feedback, 6, 6);
            assertEquals(feedback, call(get(rota), token, 200).get("feedback"));
            if (passo == 1) primeira = feedback;
            if (passo < 3) {
                assertFalse(feedback.has("alternativaCorreta"));
                assertFalse(feedback.has("textoAlternativaCorreta"));
                assertNotEquals("Explicação", feedback.get("explicacao").asText());
            } else {
                assertEquals("A", feedback.get("alternativaCorreta").asText());
                assertEquals("a", feedback.get("textoAlternativaCorreta").asText());
                assertEquals("Explicação", feedback.get("explicacao").asText());
            }
            if (passo == 2) {
                var antesDaPausa = call(get(rota + "/resultado"), token, 200).get("sessao");
                assertEquals(feedback, call(put(rota + "/interromper"), token, 200).get("feedback"));
                var pausada = call(get(rota + "/resultado"), token, 200).get("sessao");
                for (String campo : List.of("tentativas", "tentativasIncorretas", "erros")) {
                    assertEquals(antesDaPausa.get(campo), pausada.get(campo));
                }
                assertEquals(feedback, call(put(rota + "/retomar"), token, 200).get("feedback"));
                assertEquals(antesDaPausa, call(get(rota + "/resultado"), token, 200).get("sessao"));
            }
            if (passo < 4) estado = call(put(rota + "/questoes/" + (passo + 1)), token, 200);
        }

        assertEquals(primeira, call(put(urlResposta).content(body(Map.of("alternativa", "B", "passo", 1))), token, 200));
        assertEquals(4, id("select count(*) from respostas_aluno where sessao_id=? and correta=0", sessao));
        assertEquals(0, id("select count(*) from repeticao_espacada where aluno_id=?", usuarios.getFirst()));
        call(put(rota + "/encerrar"), token, 409);
        db.update("update respostas_aluno set respondido_em=? where sessao_id=?", LocalDate.of(2026, 12, 31).atTime(12, 0), sessao);
        var historico = call(get(rota + "/resultado"), token, 200).get("sessao");
        assertEquals(0, historico.get("acertos").asInt());
        assertEquals(4, historico.get("tentativas").asInt());
        assertEquals(4, historico.get("tentativasIncorretas").asInt());
        assertEquals(1, historico.get("erros").asInt());
        var evolucao = call(get("/api/aluno/evolucao").param("assuntoId", Long.toString(assunto)), token, 200).get("resumo");
        assertEquals(4, evolucao.get("tentativas").asInt());
        assertEquals(4, evolucao.get("tentativasIncorretas").asInt());
        assertEquals(1, evolucao.get("erros").asInt());
        assertEquals(3, evolucao.get("tentativasReaprendizagem").asInt());
        assertEquals(0, evolucao.get("acertos").asInt());

        String outro = cadastrar("outro-feedback");
        var estadoOutro = call(post(BASE).content(body(Map.of("assuntoId", assunto))), outro, 201);
        var feedbackOutro = call(put(rotaResposta(estadoOutro)).content(body(Map.of("alternativa", "B", "passo", 1))), outro, 200);
        assertEquals(1, feedbackOutro.get("errosDaQuestao").asInt());
        assertFalse(feedbackOutro.has("alternativaCorreta"));

        estado = call(put(rota + "/questoes/5"), token, 200);
        var acerto = call(put(rotaResposta(estado)).content(body(Map.of("alternativa", "A", "passo", 5))), token, 200);
        assertTrue(acerto.get("correta").asBoolean());
        assertFalse(acerto.has("errosDaQuestao"));
        assertEquals(0, call(put(rota + "/encerrar"), token, 200).get("pendencias").asInt());

        when(relogio.instant()).thenReturn(Instant.parse("2027-01-02T03:00:00Z"));
        var nova = abrir(assunto, 201);
        assertEquals(estado.at("/questaoAtual/id"), nova.at("/questaoAtual/id"));
        var reinicio = call(put(rotaResposta(nova)).content(body(Map.of("alternativa", "B", "passo", 1))), token, 200);
        assertEquals("DICA_INICIAL", reinicio.get("tipoOrientacao").asText());
        assertEquals(1, reinicio.get("errosDaQuestao").asInt());
    }

    private String rotaResposta(JsonNode estado) {
        return BASE + "/" + estado.at("/sessao/id").asLong() + "/questoes/" + estado.at("/questaoAtual/id").asLong() + "/resposta";
    }

    private void assertNiveis(JsonNode feedback, int anterior, int atual) {
        assertEquals(anterior, feedback.get("nivelAnterior").asInt());
        assertEquals(atual, feedback.get("nivelAtual").asInt());
    }

    private JsonNode abrir(long assuntoId, int status) throws Exception {
        return call(post(BASE).content(body(Map.of("assuntoId", assuntoId))), token, status);
    }

    private long id(String sql, Object... parametros) {
        return db.queryForObject(sql, Long.class, parametros);
    }

    private String cadastrar(String nome) throws Exception {
        JsonNode usuario = call(post("/api/auth/register").content(body(Map.of("nome", nome,
                "email", prefixo + nome + "@example.test", "senha", prefixo))), null, 201);
        usuarios.add(usuario.get("id").asLong());
        return usuario.get("token").asText();
    }

    private String body(Object valor) throws Exception { return json.writeValueAsString(valor); }

    private JsonNode call(MockHttpServletRequestBuilder pedido, String jwt, int... aceitos) throws Exception {
        if (jwt != null) pedido.header("Authorization", "Bearer " + jwt);
        var resposta = mvc.perform(pedido.contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();
        assertTrue(IntStream.of(aceitos).anyMatch(n -> n == resposta.getStatus()),
                () -> "HTTP " + resposta.getStatus() + " em " + pedido);
        return json.readTree(resposta.getContentAsByteArray());
    }
}
