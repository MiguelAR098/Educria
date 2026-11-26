package com.educria.service;

import com.educria.domain.enums.TipoGrupoPedagogico;
import com.educria.dto.GrupoPedagogicoDto.Criacao;
import com.educria.dto.GrupoPedagogicoDto.Inclusao;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = IntegracaoSqlServer.VARIAVEL_URL, matches = ".+")
class GruposPedagogicosIT extends IntegracaoSqlServer {
    private static final String ROTA = "/api/professor/grupos";
    @Autowired private JdbcTemplate db;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PasswordEncoder encoder;
    @Autowired private GrupoPedagogicoService grupos;
    @Autowired private EntityManager entidades;

    @Test
    void criaEmDuasTurmasEditaArquivaEPreservaVinculosEAprendizagem() throws Exception {
        long turmaA = turma("Turma A");
        long turmaB = turma("Turma B");
        Pessoa dono = professor(turmaA, turmaB);
        String token = login(dono);
        Pessoa primeiro = aluno("Ana", turmaA);
        Pessoa segundo = aluno("Bea", turmaB);
        long resposta = respostaLegitima(primeiro.id());
        var respostaAntes = db.queryForMap("select * from dbo.respostas_aluno where id=?", resposta);

        JsonNode criado = criar(token, "  Monitoria  ", "GRUPO", turmaB, turmaA);
        long id = criado.path("id").asLong();
        String rota = ROTA + "/" + id;
        assertEquals("Monitoria", criado.path("nome").asText());
        assertEquals(turmaA, criado.at("/turmas/0/id").asLong());
        assertEquals(turmaB, criado.at("/turmas/1/id").asLong());
        assertTrue(api(get(rota), token, null, 200).path("participantes").isEmpty());

        JsonNode editado = api(put(rota), token, Map.of("nome", "Monitoria de frações", "descricao", "Atividades guiadas"), 200);
        assertEquals("Atividades guiadas", editado.path("descricao").asText());
        assertEquals("GRUPO", editado.path("tipo").asText());
        var lista = api(get(ROTA).param("tipo", "GRUPO").param("arquivado", "false"), token, null, 200);
        assertEquals(1, lista.size());
        assertEquals(id, lista.get(0).path("id").asLong());
        assertTrue(api(get(ROTA).param("tipo", "PROJETO"), token, null, 200).isEmpty());

        var pedido = Map.of("alunosIds", List.of(segundo.id(), primeiro.id(), primeiro.id()));
        JsonNode participantes = api(post(rota + "/participantes"), token, pedido, 200).path("participantes");
        assertEquals(2, participantes.size());
        assertEquals(primeiro.id(), participantes.get(0).path("alunoId").asLong());
        assertEquals(turmaA, participantes.get(0).path("turmaOrigemId").asLong());
        assertEquals(turmaB, participantes.get(1).path("turmaAtualId").asLong());
        assertEquals(participantes, api(post(rota + "/participantes"), token, pedido, 200).path("participantes"));

        long idProjeto = criar(token, "Feira", "PROJETO", turmaA).path("id").asLong();
        api(post(ROTA + "/" + idProjeto + "/participantes"), token, Map.of("alunosIds", List.of(primeiro.id())), 200);
        assertEquals(2, id("select count(*) from dbo.grupo_participacoes where aluno_id=? and saida_em is null", primeiro.id()));

        String rotaAluno = rota + "/participantes/" + primeiro.id();
        api(delete(rotaAluno), token, null, 204);
        api(delete(rotaAluno), token, null, 204);
        assertEquals(1, id("select count(*) from dbo.grupo_participacoes where grupo_id=? and aluno_id=? and saida_em is not null",
                id, primeiro.id()));
        api(post(rota + "/participantes"), token, Map.of("alunosIds", List.of(primeiro.id())), 200);
        assertEquals(2, id("select count(*) from dbo.grupo_participacoes where grupo_id=? and aluno_id=?", id, primeiro.id()));

        var arquivado = api(patch(rota + "/arquivar"), token, null, 200).path("arquivadoEm");
        assertTrue(arquivado.isTextual());
        assertEquals(arquivado, api(patch(rota + "/arquivar"), token, null, 200).path("arquivadoEm"));
        assertEquals(2, api(get(rota), token, null, 200).path("participantes").size());
        assertEquals(1, api(get(ROTA).param("arquivado", "true"), token, null, 200).size());
        api(put(rota), token, Map.of("nome", "Imutável"), 409);
        api(post(rota + "/participantes"), token, pedido, 409);
        api(delete(rotaAluno), token, null, 409);
        assertEquals(respostaAntes, db.queryForMap("select * from dbo.respostas_aluno where id=?", resposta));
    }

    @Test
    void validaCamposETodaAListaAntesDeInserir() throws Exception {
        long turma = turma("Turma autorizada");
        long alheia = turma("Turma fora do contexto");
        String token = login(professor(turma));
        Pessoa elegivel = aluno("Elegível", turma);
        Pessoa fora = aluno("Outro estudante", alheia);
        Pessoa semTurma = aluno("Estudante sem turma", null);
        String rota = ROTA + "/" + criar(token, "Teste", "GRUPO", turma).path("id").asLong();

        api(post(ROTA), token, Map.of("nome", "  ", "tipo", "GRUPO", "turmasIds", List.of(turma)), 400);
        api(post(ROTA), token, Map.of("nome", "Teste", "tipo", "INVÁLIDO", "turmasIds", List.of(turma)), 400);
        api(post(ROTA), token, Map.of("nome", "Teste", "tipo", 0, "turmasIds", List.of(turma)), 400);
        api(post(ROTA), token, Map.of("nome", "Teste", "tipo", "GRUPO", "turmasIds", List.of()), 400);
        api(post(ROTA), token, Map.of("nome", "Teste", "tipo", "GRUPO", "turmasIds", List.of(0)), 400);
        api(post(ROTA), token, Map.of("nome", "Teste", "tipo", "GRUPO", "turmasIds", List.of(turma, alheia)), 403);
        api(post(ROTA), token, Map.of("nome", "Teste", "tipo", "GRUPO", "turmasIds", List.of(Long.MAX_VALUE)), 403);
        api(post(ROTA), token, "{", 400);
        api(get(ROTA + "/" + Long.MAX_VALUE), token, null, 404);
        api(get(ROTA + "/0"), token, null, 400);
        api(post(rota + "/participantes"), token, Map.of("alunosIds", Arrays.asList(elegivel.id(), null)), 400);
        api(post(rota + "/participantes"), token, Map.of("alunosIds", List.of(elegivel.id(), fora.id())), 404);
        api(post(rota + "/participantes"), token, Map.of("alunosIds", List.of(elegivel.id(), semTurma.id())), 404);
        api(post(rota + "/participantes"), token, Map.of("alunosIds", List.of(elegivel.id(), Long.MAX_VALUE)), 404);
        assertTrue(api(get(rota), token, null, 200).path("participantes").isEmpty());
    }

    @Test
    void filtraParticipantesTransferidosEConfereProprietarioEscopoEPapelEmLeiturasEEscritas() throws Exception {
        long turma = turma("Turma original");
        long outra = turma("Turma admitida");
        long alheia = turma("Turma privada");
        Pessoa dono = professor(turma, outra);
        String token = login(dono);
        String professorColega = login(professor(turma, outra));
        String professorAlheio = login(professor(alheia));
        Pessoa participante = aluno("Aluna", turma);
        String tokenAluno = login(participante);
        String tokenAdmin = login(admin());
        long id = criar(token, "Monitoria", "GRUPO", turma, outra).path("id").asLong();
        String rota = ROTA + "/" + id;
        Map<String, List<Long>> pedido = Map.of("alunosIds", List.of(participante.id()));

        api(get(rota), null, null, 403);
        api(post(rota + "/participantes"), null, pedido, 403);
        for (String semPapel : List.of(tokenAluno, tokenAdmin)) {
            api(get(rota), semPapel, null, 403);
            api(post(rota + "/participantes"), semPapel, pedido, 403);
        }
        for (String naoDono : List.of(professorColega, professorAlheio)) {
            assertTrue(api(get(ROTA), naoDono, null, 200).isEmpty());
            api(get(rota), naoDono, null, 404);
            api(put(rota), naoDono, Map.of("nome", "Edição negada"), 404);
            api(post(rota + "/participantes"), naoDono, pedido, 404);
            api(delete(rota + "/participantes/" + participante.id()), naoDono, null, 404);
            api(patch(rota + "/arquivar"), naoDono, null, 404);
        }

        api(post(rota + "/participantes"), token, pedido, 200);
        db.update("update dbo.alunos set turma_id=? where usuario_id=?", outra, participante.id());
        var movido = api(get(rota), token, null, 200).at("/participantes/0");
        assertEquals(turma, movido.path("turmaOrigemId").asLong());
        assertEquals(outra, movido.path("turmaAtualId").asLong());

        db.update("update dbo.alunos set turma_id=? where usuario_id=?", alheia, participante.id());
        assertTrue(api(get(rota), token, null, 200).path("participantes").isEmpty());
        api(delete(rota + "/participantes/" + participante.id()), token, null, 404);
        db.update("update dbo.alunos set turma_id=null where usuario_id=?", participante.id());
        assertTrue(api(get(rota), token, null, 200).path("participantes").isEmpty());
        assertEquals(1, id("select count(*) from dbo.grupo_participacoes where grupo_id=? and aluno_id=?", id, participante.id()));

        // Simula a substituição do vínculo dentro da transação de teste.
        db.update("update dbo.professor_turmas set turma_id=? where professor_id=? and turma_id=?", alheia, dono.id(), outra);
        entidades.clear();
        assertTrue(api(get(ROTA), token, null, 200).isEmpty());
        api(get(rota), token, null, 404);
        api(post(rota + "/participantes"), token, pedido, 404);
        api(patch(rota + "/arquivar"), token, null, 404);

        db.update("update dbo.usuarios set ativo=0 where id=?", dono.id());
        entidades.clear();
        api(get(rota), token, null, 401);
        api(post(rota + "/participantes"), token, pedido, 401);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void serializaInclusoesERemocoesConcorrentesSemDuplicarVinculo() throws Exception {
        long turma = turma("Turma de concorrência");
        Pessoa dono = professor(turma);
        long aluno = aluno("Participante concorrente", turma).id();
        long grupo = grupos.criar(dono.email(), new Criacao("Concorrência", null, TipoGrupoPedagogico.GRUPO, List.of(turma))).id();

        emParalelo(() -> grupos.incluir(dono.email(), grupo, new Inclusao(List.of(aluno))));
        assertEquals(1, id("select count(*) from dbo.grupo_participacoes where grupo_id=? and saida_em is null", grupo));
        emParalelo(() -> grupos.remover(dono.email(), grupo, aluno));
        assertEquals(1, id("select count(*) from dbo.grupo_participacoes where grupo_id=? and saida_em is not null", grupo));
        assertTrue(grupos.detalhar(dono.email(), grupo).participantes().isEmpty());
    }

    private static void emParalelo(Runnable operacao) throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var liberacao = new CountDownLatch(1);
            var prontas = new CountDownLatch(2);
            var tarefas = List.of(executor.submit(() -> executar(prontas, liberacao, operacao)),
                    executor.submit(() -> executar(prontas, liberacao, operacao)));
            assertTrue(prontas.await(10, TimeUnit.SECONDS));
            liberacao.countDown();
            for (var tarefa : tarefas) tarefa.get(20, TimeUnit.SECONDS);
        }
    }

    private static void executar(CountDownLatch prontas, CountDownLatch liberacao, Runnable operacao) {
        prontas.countDown();
        try {
            if (!liberacao.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Teste concorrente não liberado");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        operacao.run();
    }

    private long respostaLegitima(long aluno) {
        long assunto = id("select min(id) from dbo.assuntos");
        long atividade = id("insert dbo.atividades(titulo) output inserted.id values (N'Atividade de teste de grupos')");
        long questao = id("""
                insert dbo.questoes(atividade_id,assunto_id,dificuldade,enunciado,resposta_correta)
                output inserted.id values (?,?,5,N'Questão de teste',N'A')
                """, atividade, assunto);
        long sessao = id("""
                insert dbo.sessoes_estudo(aluno_id,assunto_id,situacao,iniciada_em,atualizada_em)
                output inserted.id values (?,?,'ENCERRADA',sysdatetimeoffset(),sysdatetimeoffset())
                """, aluno, assunto);
        db.update("insert dbo.sessao_questoes(sessao_id,indice,questao_id) values (?,0,?)", sessao, questao);
        return id("""
                insert dbo.respostas_aluno(aluno_id,questao_id,sessao_id,passo,resposta_dada,correta,pontos_individuais)
                output inserted.id values (?,?,?,1,N'A',1,50)
                """, aluno, questao, sessao);
    }

    private long turma(String nome) {
        return id("insert dbo.turmas(nome) output inserted.id values (?)", nome);
    }

    private Pessoa professor(long... turmas) {
        long materia = id("select id from dbo.materias where nome=N'Matemática'");
        Pessoa pessoa = usuario("Professor", "PROFESSOR");
        db.update("insert dbo.professores(usuario_id,materia_id) values (?,?)", pessoa.id(), materia);
        for (long turma : turmas) db.update("insert dbo.professor_turmas(professor_id,turma_id) values (?,?)", pessoa.id(), turma);
        return pessoa;
    }

    private Pessoa aluno(String nome, Long turma) {
        Pessoa pessoa = usuario(nome, "ALUNO");
        db.update("insert dbo.alunos(usuario_id,turma_id) values (?,?)", pessoa.id(), turma);
        return pessoa;
    }

    private Pessoa admin() {
        Pessoa pessoa = usuario("Administrador", "ADMIN");
        db.update("insert dbo.admins(usuario_id) values (?)", pessoa.id());
        return pessoa;
    }

    private Pessoa usuario(String nome, String perfil) {
        String email = UUID.randomUUID() + "@example.test";
        String senha = segredoTemporario();
        long id = id("insert dbo.usuarios(email,nome,senha,perfil) output inserted.id values (?,?,?,?)",
                email, nome, encoder.encode(senha), perfil);
        return new Pessoa(id, email, senha);
    }

    private String login(Pessoa pessoa) throws Exception {
        return api(post("/api/auth/login"), null, Map.of("email", pessoa.email(), "senha", pessoa.senha()), 200).path("token").asText();
    }

    private JsonNode criar(String token, String nome, String tipo, Long... turmas) throws Exception {
        return api(post(ROTA), token, Map.of("nome", nome, "tipo", tipo, "turmasIds", List.of(turmas)), 201);
    }

    private JsonNode api(MockHttpServletRequestBuilder pedido, String token, Object corpo, int statusEsperado) throws Exception {
        if (token != null) pedido.header("Authorization", "Bearer " + token);
        if (corpo != null) pedido.contentType(MediaType.APPLICATION_JSON)
                .content(corpo instanceof String texto ? texto : json.writeValueAsString(corpo));
        return json.readTree(mvc.perform(pedido).andExpect(status().is(statusEsperado)).andReturn().getResponse().getContentAsByteArray());
    }

    private long id(String sql, Object... args) {
        return db.queryForObject(sql, Long.class, args);
    }

    private record Pessoa(long id, String email, String senha) {}
}
