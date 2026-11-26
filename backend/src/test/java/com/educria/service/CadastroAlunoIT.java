package com.educria.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = IntegracaoSqlServer.VARIAVEL_URL, matches = ".+")
class CadastroAlunoIT extends IntegracaoSqlServer {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate banco;
    @Autowired private PasswordEncoder encoder;

    static Stream<Map.Entry<String, String>> dadosAcimaDosLimites() {
        return Stream.of(
                Map.entry("email", "validacao@" + "a".repeat(47) + "." + "b".repeat(43) + ".test"),
                Map.entry("matricula", "A".repeat(31)),
                Map.entry("senha", "á".repeat(36) + "x"));
    }

    @ParameterizedTest(name = "rejeita o campo acima do limite {index}")
    @MethodSource("dadosAcimaDosLimites")
    void rejeitaCadastroAcimaDosLimitesSemCriarConta(Map.Entry<String, String> dado) throws Exception {
        var pedido = cadastro();
        pedido.put(dado.getKey(), dado.getValue());
        byte[] corpo = json.writeValueAsBytes(pedido);
        var resposta = mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest()).andReturn().getResponse();
        assertFalse(resposta.getContentAsString().contains(dado.getValue()));
        assertEquals(0, totalContas(pedido.get("email")));
    }

    @Test
    void aceitaSenhaDeAte72BytesGravaBCryptEAutentica() throws Exception {
        var pedido = cadastro();
        String senha = "á".repeat(36);
        pedido.put("senha", senha);
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(json.writeValueAsBytes(pedido)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senha").doesNotExist());
        String hash = banco.queryForObject("select senha from dbo.usuarios where email = ?", String.class, pedido.get("email"));
        assertTrue(hash != null && !senha.equals(hash) && encoder.matches(senha, hash));
        var login = Map.of("email", pedido.get("email"), "senha", senha);
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(json.writeValueAsBytes(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void cadastrosSimultaneosDoMesmoEmailCriamUmaContaEDevolvemConflito() throws Exception {
        var pedido = cadastro();
        byte[] corpo = json.writeValueAsBytes(pedido);
        var inicio = new CyclicBarrier(2);
        try (var requisicoes = Executors.newFixedThreadPool(2)) {
            var primeira = requisicoes.submit(() -> cadastrar(inicio, corpo));
            var segunda = requisicoes.submit(() -> cadastrar(inicio, corpo));
            var codigos = Stream.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS))
                    .sorted().toList();
            assertEquals(List.of(201, 409), codigos);
        }
        assertEquals(1, totalContas(pedido.get("email")));
    }

    @Test
    void naoRegistraMensagensBrutasDeErroDoDriverSql() {
        var logger = LoggerFactory.getLogger("org.hibernate.engine.jdbc.spi.SqlExceptionHelper");
        assertFalse(logger.isErrorEnabled());
        assertFalse(logger.isWarnEnabled());
    }

    private int cadastrar(CyclicBarrier inicio, byte[] corpo) throws Exception {
        inicio.await(5, TimeUnit.SECONDS);
        return mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(corpo))
                .andReturn().getResponse().getStatus();
    }

    private int totalContas(String email) {
        return banco.queryForObject("select count(*) from dbo.usuarios where email = ?", Integer.class, email);
    }

    private static HashMap<String, String> cadastro() {
        return new HashMap<>(Map.of("nome", "Aluno sintético", "email", UUID.randomUUID() + "@example.test",
                "senha", segredoTemporario()));
    }
}
