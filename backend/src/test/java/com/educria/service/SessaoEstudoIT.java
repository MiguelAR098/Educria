package com.educria.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
class SessaoEstudoIT {
    private static final String BASE = "/api/aluno/sessoes";
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate db;
    private final List<Long> usuarios = new ArrayList<>();
    private final String prefixo = UUID.randomUUID().toString();
    private long materia, assunto, vazio, atividade;
    private String token;

    @BeforeEach
    void preparar() throws Exception {
        materia = id("insert into materias(nome) values (?) returning id", prefixo);
        assunto = id("insert into assuntos(materia_id,nome) values (?, 'Assunto teste') returning id", materia);
        vazio = id("insert into assuntos(materia_id,nome) values (?, 'Sem questões') returning id", materia);
        atividade = id("insert into atividades(titulo) values (?) returning id", prefixo);
        db.update("""
                insert into questoes(atividade_id, assunto_id, dificuldade, enunciado, alternativa_a,
                    alternativa_b, alternativa_c, alternativa_d, resposta_correta, explicacao)
                select ?, ?, 1, 'Questão ' || n, 'a', 'b', 'c', 'd', 'A', 'Explicação'
                from generate_series(1,12) n
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
                insert into usuarios(nome,email,senha,perfil) select 'Admin',?,senha,'ADMIN'
                from usuarios where id=? returning id
                """, prefixo+"-admin@example.test", usuarios.getFirst());
        usuarios.add(admin);
        db.update("insert into admins(usuario_id) values (?)", admin);
        String jwtAdmin = call(post("/api/auth/login").content(body(Map.of("email", prefixo+"-admin@example.test",
                "senha", prefixo))), null, 200).get("token").asText();
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
        assertEquals(json.valueToTree(Map.of("alternativa","C","correta",false,"explicacao","Explicação")), incorreta);
        assertEquals(incorreta, call(get(rota), token, 200).get("feedback"));
        assertEquals(1, id("""
                select count(*) from respostas_aluno where aluno_id=? and sessao_id=? and questao_id=?
                and resposta_dada='C' and not correta
                """, usuarios.getFirst(), sessao, q1));
        JsonNode proxima = call(put(rota + "/questoes/2"), token, 200);
        assertFalse(proxima.has("feedback"));
        assertFalse(call(get(rota), token, 200).has("feedback"));
        String segundaRota = rota + "/questoes/" + proxima.at("/questaoAtual/id").asLong() + "/resposta";
        assertTrue(call(put(segundaRota).content(body(Map.of("alternativa","A"))), token, 200).get("correta").asBoolean());
        call(put(rota + "/encerrar"), token, 200);
        assertEquals(incorreta, call(put(resposta).content(body(Map.of("alternativa","C"))), token, 200));
        call(put(resposta).content(body(Map.of("alternativa","A"))), token, 409);
        assertEquals(2, id("select count(*) from respostas_aluno where sessao_id=?", sessao));
        var nova = abrir(assunto, 201);
        assertEquals(q1, nova.at("/questaoAtual/id").asLong());
        assertFalse(nova.has("feedback"));
        String outraRota = BASE + "/" + nova.at("/sessao/id").asLong() + "/questoes/" + q1 + "/resposta";
        assertTrue(call(put(outraRota).content(body(Map.of("alternativa","A"))), token, 200).get("correta").asBoolean());
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
