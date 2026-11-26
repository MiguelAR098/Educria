package com.educria.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDtoTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void loginLeSenhaDoJsonSemIncluiLaNaRepresentacaoTextual() throws Exception {
        String senhaDeTeste = UUID.randomUUID().toString();
        var pedido = json.readValue("""
                {"email":"aluno@example.test","senha":"%s"}
                """.formatted(senhaDeTeste), LoginRequest.class);

        assertThat(pedido.getSenha()).isEqualTo(senhaDeTeste);
        assertThat(pedido.toString()).doesNotContain(senhaDeTeste);
    }

    @Test
    void cadastroLeSenhaDoJsonSemIncluiLaNaRepresentacaoTextual() throws Exception {
        String senhaDeTeste = UUID.randomUUID().toString();
        var pedido = json.readValue("""
                {"nome":"Aluno Exemplo","email":"aluno@example.test","senha":"%s"}
                """.formatted(senhaDeTeste), RegisterAlunoRequest.class);

        assertThat(pedido.getSenha()).isEqualTo(senhaDeTeste);
        assertThat(pedido.toString()).doesNotContain(senhaDeTeste);
    }

    @Test
    void respostaEntregaTokenNoJsonSemIncluiLoNaRepresentacaoTextual() {
        String tokenDeTeste = UUID.randomUUID().toString();
        var construtor = AuthResponse.builder().token(tokenDeTeste);
        var resposta = construtor.build();

        assertThat(json.valueToTree(resposta).get("token").asText()).isEqualTo(tokenDeTeste);
        assertThat(construtor.toString()).doesNotContain(tokenDeTeste);
        assertThat(resposta.toString()).doesNotContain(tokenDeTeste);
    }
}
