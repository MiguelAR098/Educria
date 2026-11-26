package com.educria.service;

import com.educria.domain.enums.TipoAtividadeColetiva;
import com.educria.dto.LancarAtividadeRequest;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = IntegracaoSqlServer.VARIAVEL_URL, matches = ".+")
class RankingsDaTurmaIT extends IntegracaoSqlServer {
    @Autowired private JdbcTemplate db;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PasswordEncoder encoder;
    @Autowired private PlatformTransactionManager transacoes;
    @Autowired private AtividadeColetivaService coletivas;
    @Autowired private AdminService administracao;

    @Test
    void agregaPontosDoAssuntoHistoricoEPreservaIndicadorDoPanorama() throws Exception {
        long turma = turma();
        long outraTurma = turma();
        long matematica = materia("Matemática");
        long portugues = materia("Português");
        long fracoes = id("select id from dbo.assuntos where nome=N'frações' and materia_id=?", matematica);
        long leitura = id("insert dbo.assuntos(materia_id,nome) output inserted.id values (?,N'Leitura de teste')", portugues);
        Pessoa professorMat = professor(matematica, turma);
        Pessoa professorPort = professor(portugues, turma);

        long ana = aluno("Ana", turma).id();
        long bea = aluno("Bea", turma).id();
        long caio = aluno("Caio", turma).id();
        long davi = aluno("Davi", turma).id();
        long eva = aluno("Eva", turma).id();
        long fora = aluno("Fora da turma", outraTurma).id();
        long questaoMat = questao(fracoes);
        long questaoEditada = questao(fracoes);
        long questaoPort = questao(leitura);

        resposta(ana, fracoes, questaoMat, 1, 3, true, 50);
        resposta(bea, fracoes, questaoEditada, 1, 3, true, 50);
        resposta(caio, fracoes, questaoMat, 2, 2, false, 0);
        resposta(davi, fracoes, questaoMat, 4, 3, false, null);
        resposta(ana, leitura, questaoPort, 1, 2, true, 90);
        resposta(fora, fracoes, questaoMat, 1, 2, true, 100);
        db.update("update dbo.questoes set assunto_id=?, dificuldade=10 where id=?", leitura, questaoEditada);
        db.update("insert dbo.pontuacao_coletiva(turma_id,pontos_totais) values (?,4000)", turma);

        JsonNode matematico = rankings(turma, professorMat.token());
        assertEquals(turma, matematico.path("turmaId").asLong());
        assertEquals(matematica, matematico.at("/materia/id").asLong());
        assertEquals("Matemática", matematico.at("/materia/nome").asText());
        assertEquals(30, matematico.path("periodoEvolucaoDias").asInt());
        JsonNode pontos = matematico.path("rankingPontuacao");
        assertEquals(5, pontos.size());
        linha(pontos.get(0), ana, 1, "pontos", 50);
        linha(pontos.get(1), bea, 1, "pontos", 50);
        linha(pontos.get(2), caio, 3, "pontos", 0);
        semMedida(pontos.get(3), davi, "pontos");
        semMedida(pontos.get(4), eva, "pontos");

        JsonNode evolucao = matematico.path("rankingEvolucao");
        linha(evolucao.get(0), ana, 1, "evolucao30d", 0.25);
        linha(evolucao.get(1), bea, 1, "evolucao30d", 0.25);
        linha(evolucao.get(2), caio, 3, "evolucao30d", 0);
        linha(evolucao.get(3), davi, 4, "evolucao30d", -0.12);
        semMedida(evolucao.get(4), eva, "evolucao30d");
        assertCompativelComPanorama(turma, professorMat.token(), evolucao);

        JsonNode outro = rankings(turma, professorPort.token());
        assertEquals(portugues, outro.at("/materia/id").asLong());
        linha(outro.at("/rankingPontuacao/0"), ana, 1, "pontos", 90);
        linha(outro.at("/rankingEvolucao/0"), ana, 1, "evolucao30d", 1);
        for (int i = 1; i < 5; i++) assertTrue(outro.at("/rankingPontuacao/" + i + "/pontos").isNull());
        assertCompativelComPanorama(turma, professorPort.token(), outro.path("rankingEvolucao"));
    }

    @Test
    void restringeAcessoAVinculoDeProfessorERetornaTurmaVazia() throws Exception {
        long turma = turma();
        long matematica = materia("Matemática");
        Pessoa vinculado = professor(matematica, turma);
        Pessoa outro = professor(matematica, turma());
        Pessoa aluno = aluno("Estudante", turma());
        Cadastro admin = usuario("Admin", "ADMIN");
        db.update("insert dbo.admins(usuario_id) values (?)", admin.id());
        var vazio = rankings(turma, vinculado.token());
        assertTrue(vazio.path("rankingPontuacao").isArray() && vazio.path("rankingPontuacao").isEmpty());
        assertTrue(vazio.path("rankingEvolucao").isArray() && vazio.path("rankingEvolucao").isEmpty());

        String rota = rota(turma) + "/rankings";
        api(get(rota), outro.token(), null, 403);
        api(get(rota(-1) + "/rankings"), vinculado.token(), null, 403);
        api(get(rota), aluno.token(), null, 403);
        api(get(rota), login(admin).token(), null, 403);
        api(get(rota), null, null, 403);
        api(get(rota), "token-invalido", null, 401);
    }

    @Test
    void recadosProfessorMostraSomenteOsPropriosEAlunoRecebeTodos() throws Exception {
        long turma = turma();
        Pessoa matematica = professor(materia("Matemática"), turma);
        Pessoa portugues = professor(materia("Português"), turma);
        Pessoa aluno = aluno("Estudante", turma);
        String recados = rota(turma) + "/alunos/" + aluno.id() + "/recados";
        db.update("insert dbo.recados(aluno_id,texto) values (?,N'Recado legado')", aluno.id());

        long primeiro = api(post(recados), matematica.token(), Map.of("texto", "Recado de Matemática"), 201)
                .path("id").asLong();
        long segundo = api(post(recados), portugues.token(), Map.of("texto", "Recado de Português"), 201)
                .path("id").asLong();
        JsonNode doProfessor = api(get(recados), matematica.token(), null, 200);
        assertEquals(1, doProfessor.size());
        assertEquals(primeiro, doProfessor.at("/0/id").asLong());
        assertEquals(segundo, api(get(recados), portugues.token(), null, 200).at("/0/id").asLong());
        JsonNode doAluno = api(get("/api/aluno/recados"), aluno.token(), null, 200);
        assertEquals(3, doAluno.path("naoLidos").asInt());
        assertEquals(3, doAluno.path("recados").size());
    }

    @Test
    void panoramaContaSoRevisoesPendentesDeQuestoesEAtividadesAtivas() throws Exception {
        long turma = turma();
        long matematica = materia("Matemática");
        Pessoa professor = professor(matematica, turma);
        Pessoa aluno = aluno("Estudante", turma);
        long assunto = id("select id from dbo.assuntos where nome=N'frações' and materia_id=?", matematica);
        long ativa = questao(assunto);
        long desativada = questao(assunto);
        long atividadeDesativada = questao(assunto);
        db.update("update dbo.questoes set ativa=0 where id=?", desativada);
        db.update("update dbo.atividades set ativa=0 where id=(select atividade_id from dbo.questoes where id=?)",
                atividadeDesativada);
        for (long questao : new long[]{ativa, desativada, atividadeDesativada}) {
            db.update("insert dbo.repeticao_espacada(aluno_id,questao_id,proxima_revisao) values (?,?,'2020-01-01')",
                    aluno.id(), questao);
        }

        JsonNode panorama = api(get(rota(turma) + "/panorama"), professor.token(), null, 200);
        assertEquals(1, panorama.at("/alunos/0/revisoesVencidas").asInt(-1));
        assertEquals(3L, id("select count_big(*) from dbo.repeticao_espacada where aluno_id=?", aluno.id()));
    }

    @Test
    void totalizaAlunosSomenteNasTurmasVinculadasAoPerfil() throws Exception {
        long ocupada = turma();
        long vazia = turma();
        long alheia = turma();
        Pessoa professor = professor(materia("Matemática"), ocupada);
        db.update("insert dbo.professor_turmas(professor_id,turma_id) values (?,?)", professor.id(), vazia);
        aluno("Primeiro estudante", ocupada);
        aluno("Segundo estudante", ocupada);
        aluno("De outra turma", alheia);

        JsonNode perfil = api(get("/api/professor/perfil"), professor.token(), null, 200);
        Map<Long, Long> totais = new HashMap<>();
        perfil.path("turmas").forEach(turma -> totais.put(turma.path("id").asLong(), turma.path("totalAlunos").asLong(-1)));
        assertEquals(Map.of(ocupada, 2L, vazia, 0L), totais);
    }

    @Test
    void preservaSaldoColetivoQuandoSomaExcedeOLimiteDoBanco() throws Exception {
        long turma = turma();
        Pessoa professor = professor(materia("Matemática"), turma);
        Cadastro admin = usuario("Admin", "ADMIN");
        db.update("insert dbo.admins(usuario_id) values (?)", admin.id());
        int saldo = Integer.MAX_VALUE - 10;
        db.update("insert dbo.pontuacao_coletiva(turma_id,pontos_totais) values (?,?)", turma, saldo);

        api(post(rota(turma) + "/atividades"), professor.token(),
                Map.of("tipo", "PROJETO", "titulo", "Atividade da turma", "pontos", 20), 409);
        api(post("/api/admin/turmas/" + turma + "/pontos"), login(admin).token(), Map.of("pontos", 11), 409);
        assertEquals(saldo, id("select pontos_totais from dbo.pontuacao_coletiva where turma_id=?", turma));
        assertEquals(0L, id("select count_big(*) from dbo.atividades_coletivas where turma_id=?", turma));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void conservaSomaComLancamentosConcorrentesNaInicializacaoENoSaldoExistente() throws Exception {
        long turma = turma();
        Pessoa professor = professor(materia("Matemática"), turma);
        String email = db.queryForObject("select email from dbo.usuarios where id=?", String.class, professor.id());
        var projeto = new LancarAtividadeRequest(TipoAtividadeColetiva.PROJETO, "Atividade da turma", 20, null);

        try (var tarefas = Executors.newFixedThreadPool(2)) {
            var inicio = new CountDownLatch(1);
            var primeiro = tarefas.submit(() -> {
                aguardar(inicio);
                coletivas.lancar(email, turma, projeto);
            });
            var segundo = tarefas.submit(() -> {
                aguardar(inicio);
                coletivas.lancar(email, turma, projeto);
            });
            inicio.countDown();
            primeiro.get(20, TimeUnit.SECONDS);
            segundo.get(20, TimeUnit.SECONDS);
            assertEquals(40L, id("select pontos_totais from dbo.pontuacao_coletiva where turma_id=?", turma));

            var professorLancou = new CountDownLatch(1);
            var adminLancou = new CountDownLatch(1);
            var adminIniciou = new CountDownLatch(1);
            var template = new TransactionTemplate(transacoes);
            var terceiro = tarefas.submit(() -> template.executeWithoutResult(status -> {
                coletivas.lancar(email, turma, projeto);
                professorLancou.countDown();
                aguardar(adminIniciou);
                // Mantém o lock durante a tentativa do admin de ler o saldo, antes do flush no commit.
                aguardar(adminLancou, 2);
            }));
            var quarto = tarefas.submit(() -> template.executeWithoutResult(status -> {
                aguardar(professorLancou);
                adminIniciou.countDown();
                administracao.lancarPontosColetivos(turma, 40);
                adminLancou.countDown();
            }));
            terceiro.get(20, TimeUnit.SECONDS);
            quarto.get(20, TimeUnit.SECONDS);
        }

        assertEquals(100L, id("select pontos_totais from dbo.pontuacao_coletiva where turma_id=?", turma));
        assertEquals(3L, id("select count_big(*) from dbo.atividades_coletivas where turma_id=?", turma));
    }

    private static void aguardar(CountDownLatch sinal) {
        assertTrue(aguardar(sinal, 10));
    }

    private static boolean aguardar(CountDownLatch sinal, int segundos) {
        try {
            return sinal.await(segundos, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private JsonNode rankings(long turma, String token) throws Exception {
        var resposta = mvc.perform(get(rota(turma) + "/rankings").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        return json.readTree(resposta.andReturn().getResponse().getContentAsByteArray());
    }

    private void assertCompativelComPanorama(long turma, String token, JsonNode evolucao) throws Exception {
        var painel = api(get(rota(turma) + "/panorama"), token, null, 200).path("alunos");
        assertEquals(painel.size(), evolucao.size());
        for (JsonNode linha : painel) {
            JsonNode ranqueado = null;
            for (JsonNode item : evolucao) {
                if (item.path("alunoId").equals(linha.path("alunoId"))) ranqueado = item;
            }
            assertNotNull(ranqueado);
            assertEquals(linha.path("evolucao30d"), ranqueado.path("evolucao30d"));
        }
    }

    private static void linha(JsonNode linha, long aluno, int posicao, String medida, double valor) {
        assertEquals(aluno, linha.path("alunoId").asLong());
        assertEquals(posicao, linha.path("posicao").asInt());
        assertEquals(valor, linha.path(medida).asDouble(Double.NaN));
        assertEquals(4, linha.size());
    }

    private static void semMedida(JsonNode linha, long aluno, String medida) {
        assertEquals(aluno, linha.path("alunoId").asLong());
        assertTrue(linha.path("posicao").isNull());
        assertTrue(linha.path(medida).isNull());
    }

    private void resposta(long aluno, long assunto, long questao, Integer antes, Integer depois,
                          boolean correta, Integer pontos) {
        long sessao = id("""
                insert dbo.sessoes_estudo(aluno_id,assunto_id,situacao,iniciada_em,atualizada_em)
                output inserted.id values (?,?,'ENCERRADA',sysdatetimeoffset(),sysdatetimeoffset())
                """, aluno, assunto);
        db.update("insert dbo.sessao_questoes(sessao_id,indice,questao_id) values (?,0,?)", sessao, questao);
        db.update("""
                insert dbo.respostas_aluno(aluno_id,questao_id,sessao_id,passo,resposta_dada,correta,
                                           nivel_anterior,nivel_atual,pontos_individuais)
                values (?,?,?,1,?,?,?,?,?)
                """, aluno, questao, sessao, correta ? "A" : "B", correta, antes, depois, pontos);
        if (depois != null) db.update("""
                update dbo.desempenho_por_assunto set nivel_atual=?, total_acertos=?, total_erros=?
                where aluno_id=? and assunto_id=?
                """, depois, correta ? 1 : 0, correta ? 0 : 1, aluno, assunto);
    }

    private long questao(long assunto) {
        long atividade = id("insert dbo.atividades(titulo) output inserted.id values (N'Atividade de ranking')");
        return id("""
                insert dbo.questoes(atividade_id,assunto_id,dificuldade,enunciado,resposta_correta)
                output inserted.id values (?,?,5,N'Questão sintética',N'A')
                """, atividade, assunto);
    }

    private long turma() {
        return id("insert dbo.turmas(nome) output inserted.id values (N'Turma de ranking')");
    }

    private long materia(String nome) {
        return id("select id from dbo.materias where nome=?", nome);
    }

    private Pessoa professor(long materia, long turma) throws Exception {
        var pessoa = usuario("Professor", "PROFESSOR");
        db.update("insert dbo.professores(usuario_id,materia_id) values (?,?)", pessoa.id(), materia);
        db.update("insert dbo.professor_turmas(professor_id,turma_id) values (?,?)", pessoa.id(), turma);
        return login(pessoa);
    }

    private Pessoa aluno(String nome, long turma) throws Exception {
        var pessoa = usuario(nome, "ALUNO");
        db.update("insert dbo.alunos(usuario_id,turma_id) values (?,?)", pessoa.id(), turma);
        return login(pessoa);
    }

    private Cadastro usuario(String nome, String perfil) {
        String email = UUID.randomUUID() + "@example.test";
        String senha = segredoTemporario();
        long id = id("insert dbo.usuarios(email,nome,senha,perfil) output inserted.id values (?,?,?,?)",
                email, nome, encoder.encode(senha), perfil);
        return new Cadastro(id, email, senha);
    }

    private Pessoa login(Cadastro pessoa) throws Exception {
        return new Pessoa(pessoa.id(), api(post("/api/auth/login"), null,
                Map.of("email", pessoa.email(), "senha", pessoa.senha()), 200)
                .path("token").asText());
    }

    private JsonNode api(MockHttpServletRequestBuilder pedido, String token, Object corpo, int codigo) throws Exception {
        if (token != null) pedido.header("Authorization", "Bearer " + token);
        if (corpo != null) pedido.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(corpo));
        return json.readTree(mvc.perform(pedido).andExpect(status().is(codigo)).andReturn().getResponse().getContentAsByteArray());
    }

    private long id(String sql, Object... args) {
        return db.queryForObject(sql, Long.class, args);
    }

    private static String rota(long turma) {
        return "/api/professor/turmas/" + turma;
    }

    private record Pessoa(long id, String token) {}
    private record Cadastro(long id, String email, String senha) {}
}
