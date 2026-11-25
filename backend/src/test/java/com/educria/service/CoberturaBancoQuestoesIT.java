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
    void reproduzADistribuicaoDasOitentaQuestoesOriginais() throws Exception {
        String token = professor(materia("Matemática"));
        assertTrue(db.queryForObject("select @@TRANCOUNT", Integer.class) > 0);
        var seed = new ClassPathResource("db/sqlserver/demo/afterMigrate__questoes_de_matematica.sql");
        db.execute(seed.getContentAsString(StandardCharsets.UTF_8));

        JsonNode matriz = cobertura(token);
        assertEquals(8, matriz.path("assuntos").size());
        assertTotais(matriz, 80, 40);
        for (JsonNode assunto : matriz.path("assuntos")) {
            assertLinha(matriz, assunto.path("id").asLong(), 2, 2, 2, 2, 2, 0, 0, 0, 0, 0);
        }
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
