package com.educria.domain.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestaoJsonTest {
    @Test
    void mantemGabaritoEExplicacaoNoServidor() {
        Questao questao = Questao.builder()
                .enunciado("Quanto é 7 + 8?")
                .alternativaA("13").alternativaB("15").alternativaC("16").alternativaD("17")
                .respostaCorreta("B").explicacao("7 mais 8 é 15.")
                .build();

        var json = new ObjectMapper().valueToTree(questao);
        assertEquals("15", json.get("alternativaB").asText());
        assertFalse(json.has("respostaCorreta"));
        assertFalse(json.has("explicacao"));
        assertTrue(questao.isRespostaCorreta("B"));
    }
}
