package com.educria.service;

import com.educria.dto.QuestaoRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = IntegracaoSqlServer.VARIAVEL_URL, matches = ".+")
class CoberturaBancoQuestoesIT extends IntegracaoSqlServer {
    private static final String QUESTOES = "/api/professor/questoes";
    private static final String COBERTURA = QUESTOES + "/cobertura";
    private static final int[] LINHA_VAZIA = new int[10];
    private static final String DESCRICAO_DEMO_LEGADA = "Banco de questões do protótipo EDUCRIA (q1 a q80).";
    private static final String DESCRICAO_DEMO_ATUAL = "Banco demonstrativo de Matemática do EDUCRIA (questões 1 a 160).";
    private static final String PIZZA_LEGADA = "Duas pizzas são cortadas em 8 fatias cada. Cinco pessoas comem 3 fatias. Quantas sobram?";
    private static final String PIZZA_CORRIGIDA = "Duas pizzas são cortadas em 8 fatias cada. Cinco pessoas comem 3 fatias cada uma. Quantas sobram?";

    @Autowired private JdbcTemplate db;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PasswordEncoder encoder;

    @Test
    void contaSomenteAsQuestoesElegiveisDaMateriaDoProfessor() throws Exception {
        long matematica = materia("Matemática");
        long portugues = materia("Português");
        String professorMatematica = professor(matematica);
        String professorPortugues = professor(portugues);
        JsonNode matematicaAntes = cobertura(professorMatematica);
        JsonNode portuguesAntes = cobertura(professorPortugues);

        long fracoes = cadastrarAssunto(matematica, "Frações para validação");
        long vazio = cadastrarAssunto(matematica, "Assunto sem questões para validação");
        long leitura = cadastrarAssunto(portugues, "Leitura para validação");
        long ativa = cadastrarAtividade(true);
        long inativa = cadastrarAtividade(false);

        inserirQuestao(ativa, fracoes, 1, true, "A", null);
        inserirQuestao(ativa, fracoes, 1, true, "b", "  2  ");
        inserirQuestao(ativa, fracoes, 10, true, "B", "2");
        inserirQuestao(ativa, leitura, 4, true, "A", "2");
        inserirQuestao(inativa, fracoes, 2, true, "A", "2");
        inserirQuestao(ativa, fracoes, 3, false, "A", "2");
        inserirQuestao(ativa, fracoes, 4, true, "B", "\t\n\u2003");
        inserirQuestao(ativa, fracoes, 5, true, "B", null);
        inserirQuestao(ativa, fracoes, 6, true, "X", "2");
        inserirQuestao(ativa, fracoes, null, true, "A", "2");
        inserirQuestao(ativa, null, 7, true, "A", "2");

        JsonNode coberturaMatematica = cobertura(professorMatematica);
        assertEquals(matematicaAntes.path("assuntos").size() + 2, coberturaMatematica.path("assuntos").size());
        assertTotais(coberturaMatematica, matematicaAntes.path("total").asLong() + 3,
                matematicaAntes.path("combinacoesVazias").asLong() + 18);
        assertLinha(coberturaMatematica, fracoes, 2, 0, 0, 0, 0, 0, 0, 0, 0, 1);
        assertLinha(coberturaMatematica, vazio, LINHA_VAZIA);
        assertFalse(temAssunto(coberturaMatematica, leitura));

        JsonNode coberturaPortugues = cobertura(professorPortugues);
        assertEquals(portuguesAntes.path("assuntos").size() + 1, coberturaPortugues.path("assuntos").size());
        assertTotais(coberturaPortugues, portuguesAntes.path("total").asLong() + 1,
                portuguesAntes.path("combinacoesVazias").asLong() + 9);
        assertLinha(coberturaPortugues, leitura, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0);
        assertFalse(temAssunto(coberturaPortugues, fracoes));
        assertFalse(temAssunto(coberturaPortugues, vazio));
    }

    @Test
    void coberturaExigeProfessorAutenticado() throws Exception {
        String senha = segredoTemporario();
        String emailAdmin = emailDeTeste();
        long admin = cadastrarUsuario(emailAdmin, senha, "ADMIN");
        db.update("insert dbo.admins(usuario_id) values (?)", admin);
        String tokenAdmin = login(emailAdmin, senha);
        String tokenAluno = api(post("/api/auth/register"), null,
                Map.of("nome", "Estudante", "email", emailDeTeste(), "senha", segredoTemporario()), 201)
                .path("token").asText();

        api(get(COBERTURA), null, null, 403);
        api(get(COBERTURA), "token-invalido", null, 401);
        api(get(COBERTURA), tokenAluno, null, 403);
        api(get(COBERTURA), tokenAdmin, null, 403);
    }

    @Test
    void exportaGabaritoMinusculoEIgnoraQuestaoLegadaIncompleta() throws Exception {
        long materia = materia("Português");
        String token = professor(materia);
        long assunto = cadastrarAssunto(materia, "Interpretação para exportação");
        long atividade = cadastrarAtividade(true);
        inserirQuestao(atividade, assunto, 2, true, "A", null);
        db.update("""
                insert dbo.questoes(atividade_id,assunto_id,dificuldade,enunciado,
                    alternativa_a,alternativa_b,alternativa_c,alternativa_d,resposta_correta)
                values (?,?,2,N'Qual alternativa é uma palavra?',N'1',N'livro',N'2',N'3',N'b')
                """, atividade, assunto);

        JsonNode exportadas = api(get(QUESTOES + "/exportar"), token, null, 200);
        assertEquals(1, exportadas.size());
        assertEquals("Interpretação para exportação", exportadas.at("/0/conceito").asText());
        assertEquals(1, exportadas.at("/0/correta").asInt(-1));
        assertEquals("livro", exportadas.at("/0/alts/1").asText());
        assertEquals(2, id("select count_big(*) from dbo.questoes where assunto_id=?", assunto));
    }

    @Test
    void listaMaisDeDoisMilECemIdsComEstatisticasDeRespostas() throws Exception {
        long materia = materia("Português");
        String token = professor(materia);
        long assunto = cadastrarAssunto(materia, "Banco para estatísticas");
        long atividade = cadastrarAtividade(true);
        db.update("""
                insert dbo.questoes(atividade_id,assunto_id,dificuldade,enunciado,resposta_correta)
                select top (2101) ?,?,4,N'Questão de validação de volume',N'A'
                from sys.all_objects a cross join sys.all_objects b
                """, atividade, assunto);
        long questao = id("select min(id) from dbo.questoes where assunto_id=?", assunto);
        long aluno = api(post("/api/auth/register"), null, Map.of("nome", "Estudante", "email", emailDeTeste(),
                "senha", segredoTemporario()), 201).path("id").asLong();
        db.update("insert dbo.respostas_aluno(aluno_id,questao_id,correta) values (?,?,1)", aluno, questao);

        JsonNode listagem = api(get(QUESTOES).param("assuntoId", Long.toString(assunto)), token, null, 200).path("questoes");
        assertEquals(2101, listagem.size());
        assertEquals(questao, listagem.at("/0/id").asLong());
        assertEquals(1, listagem.at("/0/totalRespostas").asLong());
        assertEquals(100, listagem.at("/0/taxaAcerto").asDouble());
        assertEquals(0, listagem.at("/1/totalRespostas").asLong(-1));
    }

    @Test
    void refleteCadastroEdicaoAtivacaoImportacaoENovosAssuntos() throws Exception {
        String token = professor(materia("Português"));
        JsonNode antes = cobertura(token);
        long totalAntes = antes.path("total").asLong();
        long vaziasAntes = antes.path("combinacoesVazias").asLong();
        String interpretacao = "Interpretação para validação";
        long primeiro = api(post("/api/professor/assuntos"), token, Map.of("nome", interpretacao), 201).path("id").asLong();
        long segundo = api(post("/api/professor/assuntos"), token, Map.of("nome", "Argumentação para validação"), 201)
                .path("id").asLong();

        JsonNode semQuestoes = cobertura(token);
        assertTotais(semQuestoes, totalAntes, vaziasAntes + 20);
        assertEquals(interpretacao, linha(semQuestoes, primeiro).path("nome").asText());
        assertLinha(semQuestoes, primeiro, LINHA_VAZIA);
        assertLinha(semQuestoes, segundo, LINHA_VAZIA);

        long questao = api(post(QUESTOES), token, pedido(primeiro, 2), 201).path("id").asLong();
        JsonNode criada = cobertura(token);
        assertTotais(criada, totalAntes + 1, vaziasAntes + 19);
        assertLinha(criada, primeiro, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0);

        api(put(QUESTOES + "/" + questao), token, pedido(segundo, 10), 200);
        JsonNode movida = cobertura(token);
        assertTotais(movida, totalAntes + 1, vaziasAntes + 19);
        assertLinha(movida, primeiro, LINHA_VAZIA);
        assertLinha(movida, segundo, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1);

        String ativacao = QUESTOES + "/" + questao + "/ativa";
        api(patch(ativacao), token, Map.of("ativa", false), 200);
        JsonNode desativada = cobertura(token);
        assertTotais(desativada, totalAntes, vaziasAntes + 20);
        assertLinha(desativada, segundo, LINHA_VAZIA);
        api(patch(ativacao), token, Map.of("ativa", true), 200);
        assertEquals(movida, cobertura(token));

        JsonNode importacao = api(post(QUESTOES + "/importar"), token, List.of(Map.of(
                "conceito", interpretacao, "dificuldade", 6, "enunciado", "Qual frase expressa uma opinião?",
                "alts", List.of("Achei o livro divertido", "Há doze páginas", "O livro tem uma capa", "O livro foi publicado"),
                "correta", 0, "explicacao", "A primeira frase expressa uma avaliação pessoal.")), 200);
        assertEquals(1, importacao.path("importadas").asInt());
        JsonNode importada = cobertura(token);
        assertTotais(importada, totalAntes + 2, vaziasAntes + 18);
        assertLinha(importada, primeiro, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0);
    }

    @Test
    void preencheAsDezDificuldadesEmBaseSemCargaERepetirNaoDuplica() throws Exception {
        String token = professor(materia("Matemática"));
        assertEquals(0, id("select count_big(*) from dbo.questoes"));
        assertTrue(db.queryForObject("select @@TRANCOUNT", Integer.class) > 0);
        String seed = cargaDemo();
        db.execute(seed);
        var ids = db.queryForList("select id from dbo.questoes where atividade_id=-1 order by ordem", Long.class);
        db.execute(seed);
        assertEquals(ids, db.queryForList("select id from dbo.questoes where atividade_id=-1 order by ordem", Long.class));
        assertEquals(160, ids.size());
        assertEquals(160, id("select count_big(distinct ordem) from dbo.questoes where atividade_id=-1"));
        assertEquals("1/6", db.queryForObject("select alternativa_c from dbo.questoes where atividade_id=-1 and ordem=26",
                String.class));
        assertTrue(db.queryForObject("select enunciado from dbo.questoes where atividade_id=-1 and ordem=71",
                String.class).contains("3 fatias cada uma"));
        assertTrue(db.queryForObject("select enunciado from dbo.questoes where atividade_id=-1 and ordem=140",
                String.class).contains("guardou toda a sobra"));

        JsonNode matriz = cobertura(token);
        assertEquals(8, matriz.path("assuntos").size());
        assertTotais(matriz, 160, 0);
        for (JsonNode assunto : matriz.path("assuntos")) {
            assertLinha(matriz, assunto.path("id").asLong(), 2, 2, 2, 2, 2, 2, 2, 2, 2, 2);
        }
    }

    @Test
    void ampliaCargaOriginalSemAlterarEdicoesInativasOuHistorico() throws Exception {
        String seed = cargaDemo();
        prepararCargaLegada(seed);
        db.update("update dbo.questoes set ativa=0, enunciado=N'Enunciado preservado' where atividade_id=-1 and ordem=1");
        long questao = id("select id from dbo.questoes where atividade_id=-1 and ordem=26");
        long assunto = id("select assunto_id from dbo.questoes where id=?", questao);
        long aluno = api(post("/api/auth/register"), null, Map.of("nome", "Estudante", "email", emailDeTeste(),
                "senha", segredoTemporario()), 201).path("id").asLong();
        long sessao = id("""
                insert dbo.sessoes_estudo(aluno_id,assunto_id,situacao,iniciada_em,atualizada_em)
                output inserted.id values (?,?,'ENCERRADA',sysdatetimeoffset(),sysdatetimeoffset())
                """, aluno, assunto);
        db.update("insert dbo.sessao_questoes(sessao_id,indice,questao_id) values (?,0,?)", sessao, questao);
        db.update("""
                insert dbo.respostas_aluno(aluno_id,questao_id,sessao_id,passo,resposta_dada,correta,pontos_individuais)
                values (?,?,?,1,N'B',1,10)
                """, aluno, questao, sessao);
        db.update("insert dbo.repeticao_espacada(aluno_id,questao_id,intervalo_atual,proxima_revisao) values (?,?,'D1','2030-01-01')",
                aluno, questao);

        String questoesDemo = "select * from dbo.questoes where atividade_id=-1 order by ordem";
        String descricaoDemo = "select enunciado from dbo.atividades where id=-1";
        var questoesOriginaisEsperadas = db.queryForList(questoesDemo);
        questoesOriginaisEsperadas.get(26 - 1).put("alternativa_c", "1/6");
        questoesOriginaisEsperadas.get(71 - 1).put("enunciado", PIZZA_CORRIGIDA);
        var antesRespostas = db.queryForList("select * from dbo.respostas_aluno where sessao_id=?", sessao);
        var antesRevisoes = db.queryForList("select * from dbo.repeticao_espacada where aluno_id=?", aluno);
        db.execute(seed);
        var questoesAmpliadas = db.queryForList(questoesDemo);
        assertEquals(160, questoesAmpliadas.size());
        assertEquals(questoesOriginaisEsperadas, questoesAmpliadas.subList(0, 80));
        assertEquals(DESCRICAO_DEMO_ATUAL, db.queryForObject(descricaoDemo, String.class));
        db.execute(seed);
        assertEquals(questoesAmpliadas, db.queryForList(questoesDemo));
        assertEquals(DESCRICAO_DEMO_ATUAL, db.queryForObject(descricaoDemo, String.class));
        assertEquals(antesRespostas, db.queryForList("select * from dbo.respostas_aluno where sessao_id=?", sessao));
        assertEquals(antesRevisoes, db.queryForList("select * from dbo.repeticao_espacada where aluno_id=?", aluno));
        assertTotais(cobertura(professor(materia("Matemática"))), 159, 0);
    }

    @Test
    void preservaQuestoesEditadasOuDesativadasNaCargaLegada() throws Exception {
        String seed = cargaDemo();
        prepararCargaLegada(seed);
        db.update("update dbo.questoes set explicacao=N'Explicação editada' where atividade_id=-1 and ordem=26");
        db.update("update dbo.questoes set ativa=0 where atividade_id=-1 and ordem=71");
        String questoesProtegidas = "select * from dbo.questoes where atividade_id=-1 and ordem in (26, 71) order by ordem";
        var antes = db.queryForList(questoesProtegidas);

        db.execute(seed);

        assertEquals(160, id("select count_big(*) from dbo.questoes where atividade_id=-1"));
        assertEquals(antes, db.queryForList(questoesProtegidas));
    }

    @Test
    void selecionaEPontuaAsDificuldadesAltasEmSessoesReais() throws Exception {
        db.execute(cargaDemo());
        JsonNode cadastro = api(post("/api/auth/register"), null, Map.of("nome", "Estudante", "email", emailDeTeste(),
                "senha", segredoTemporario()), 201);
        String token = cadastro.path("token").asText();
        long aluno = cadastro.path("id").asLong();
        List<Long> assuntos = db.queryForList("select id from dbo.assuntos where materia_id=? order by id", Long.class,
                materia("Matemática"));
        String sessoes = "/api/aluno/sessoes";
        int soma = 0;
        for (int dificuldade = 6; dificuldade <= 10; dificuldade++) {
            long assunto = assuntos.get(dificuldade - 6);
            db.update("update dbo.desempenho_por_assunto set nivel_atual=? where aluno_id=? and assunto_id=?",
                    dificuldade, aluno, assunto);
            JsonNode estado = api(post(sessoes), token, Map.of("assuntoId", assunto), 201);
            JsonNode questao = estado.path("questaoAtual");
            assertEquals(dificuldade, questao.path("dificuldade").asInt());
            assertFalse(questao.has("respostaCorreta"));
            assertFalse(questao.has("explicacao"));
            long questaoId = questao.path("id").asLong();
            assertEquals(1, id("select count_big(*) from dbo.questoes where id=? and atividade_id=-1 and ordem>80", questaoId));
            String letra = db.queryForObject("select resposta_correta from dbo.questoes where id=?", String.class, questaoId);
            String sessao = sessoes + "/" + estado.at("/sessao/id").asLong();
            JsonNode feedback = api(put(sessao + "/questoes/" + questaoId + "/resposta"), token,
                    Map.of("passo", 1, "alternativa", letra), 200);
            assertTrue(feedback.path("correta").asBoolean());
            int pontos = dificuldade * 10;
            soma += pontos;
            assertEquals(pontos, feedback.path("pontosConcedidos").asInt());
            assertEquals(Math.min(dificuldade + 1, 10), feedback.path("nivelAtual").asInt());
            assertEquals(1, id("select count_big(*) from dbo.repeticao_espacada where aluno_id=? and questao_id=?", aluno, questaoId));
            api(put(sessao + "/encerrar"), token, null, 200);
            assertEquals(pontos, api(get(sessao + "/resultado"), token, null, 200).at("/sessao/pontosIndividuais").asInt());
        }
        assertEquals(soma, api(get("/api/aluno/desempenho"), token, null, 200).path("pontosIndividuais").asInt());
    }

    private static String cargaDemo() throws Exception {
        return new ClassPathResource("db/sqlserver/demo/afterMigrate__questoes_de_matematica.sql")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private void prepararCargaLegada(String seed) {
        int inicioAmpliacao = seed.indexOf("    -- Ampliação nas dificuldades");
        int inicioCatalogo = seed.indexOf("    DECLARE @materia bigint");
        assertTrue(inicioAmpliacao > 0 && inicioCatalogo > inicioAmpliacao);
        db.execute(seed.substring(0, inicioAmpliacao) + seed.substring(inicioCatalogo));
        assertEquals(80, id("select count_big(*) from dbo.questoes where atividade_id=-1"));
        assertEquals(1, db.update("update dbo.atividades set enunciado=? where id=-1", DESCRICAO_DEMO_LEGADA));
        assertEquals(1, db.update("update dbo.questoes set alternativa_c=N'1/3' where atividade_id=-1 and ordem=26"));
        assertEquals(1, db.update("update dbo.questoes set enunciado=? where atividade_id=-1 and ordem=71", PIZZA_LEGADA));
    }

    private JsonNode cobertura(String token) throws Exception {
        return api(get(COBERTURA), token, null, 200);
    }

    private static void assertTotais(JsonNode cobertura, long total, long vazias) {
        assertEquals(total, cobertura.path("total").asLong(-1));
        assertEquals(vazias, cobertura.path("combinacoesVazias").asLong(-1));
    }

    private static void assertLinha(JsonNode cobertura, long id, int... esperadas) {
        JsonNode linha = linha(cobertura, id);
        assertEquals(Arrays.stream(esperadas).sum(), linha.path("total").asInt(-1));
        JsonNode contagens = linha.path("contagens");
        assertTrue(contagens.isArray());
        assertEquals(10, contagens.size());
        for (int i = 0; i < esperadas.length; i++) {
            assertEquals(esperadas[i], contagens.get(i).asInt(-1), "Assunto " + id + ", dificuldade " + (i + 1));
        }
    }

    private static JsonNode linha(JsonNode cobertura, long id) {
        for (JsonNode linha : cobertura.path("assuntos")) {
            if (linha.path("id").asLong() == id) return linha;
        }
        throw new AssertionError("Assunto ausente da cobertura: " + id);
    }

    private static boolean temAssunto(JsonNode cobertura, long id) {
        for (JsonNode linha : cobertura.path("assuntos")) {
            if (linha.path("id").asLong() == id) return true;
        }
        return false;
    }

    private static QuestaoRequest pedido(long assunto, int dificuldade) {
        return new QuestaoRequest(assunto, dificuldade, "Qual palavra completa a frase?",
                "casa", "livro", "porta", "janela", "A", null);
    }

    private long materia(String nome) {
        return id("select id from dbo.materias where nome=?", nome);
    }

    private long cadastrarAssunto(long materia, String nome) {
        return id("insert dbo.assuntos(materia_id,nome) output inserted.id values (?,?)", materia, nome);
    }

    private long cadastrarAtividade(boolean ativa) {
        return id("insert dbo.atividades(titulo,ativa) output inserted.id values (N'Atividade de validação',?)", ativa);
    }

    private void inserirQuestao(long atividade, Long assunto, Integer dificuldade, boolean ativa,
                                String gabarito, String alternativaB) {
        db.update("""
                insert dbo.questoes(atividade_id,assunto_id,dificuldade,ativa,enunciado,alternativa_a,alternativa_b,resposta_correta)
                values (?,?,?,?,N'Questão de validação',N'1',?,?)
                """, atividade, assunto, dificuldade, ativa, alternativaB, gabarito);
    }

    private String professor(long materia) throws Exception {
        String email = emailDeTeste();
        String senha = segredoTemporario();
        long professor = cadastrarUsuario(email, senha, "PROFESSOR");
        db.update("insert dbo.professores(usuario_id,materia_id) values (?,?)", professor, materia);
        return login(email, senha);
    }

    private long cadastrarUsuario(String email, String senha, String perfil) {
        return id("insert dbo.usuarios(email,nome,senha,perfil) output inserted.id values (?,N'Pessoa de validação',?,?)",
                email, encoder.encode(senha), perfil);
    }

    private String login(String email, String senha) throws Exception {
        return api(post("/api/auth/login"), null, Map.of("email", email, "senha", senha), 200).path("token").asText();
    }

    private static String emailDeTeste() {
        return UUID.randomUUID() + "@example.test";
    }

    private JsonNode api(MockHttpServletRequestBuilder pedido, String token, Object corpo, int codigo) throws Exception {
        if (token != null) pedido.header("Authorization", "Bearer " + token);
        if (corpo != null) pedido.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(corpo));
        return json.readTree(mvc.perform(pedido).andExpect(status().is(codigo)).andReturn().getResponse().getContentAsByteArray());
    }

    private long id(String sql, Object... args) {
        return db.queryForObject(sql, Long.class, args);
    }
}
