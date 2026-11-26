package com.educria.service;

import com.educria.dto.QuestaoRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeitorDeQuestoesTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, Long> ASSUNTOS = Map.of("porcentagem", 7L, "fracoes", 3L);

    private static JsonNode json(String texto) throws Exception {
        return JSON.readTree(texto);
    }

    @Test
    void leItemNoFormatoDoPrototipo() throws Exception {
        QuestaoRequest pedido = LeitorDeQuestoes.ler(json("""
                {"conceito":"porcentagem","dificuldade":2,"enunciado":"Quanto é 20% de 90?",
                 "alts":["16","18","20","22"],"correta":1,"explicacao":"20% de 90 é 18."}
                """), ASSUNTOS);

        assertEquals(7L, pedido.assuntoId());
        assertEquals(2, pedido.dificuldade());
        assertEquals("B", pedido.respostaCorreta());
        assertEquals("18", pedido.alternativaB());
        assertEquals("20% de 90 é 18.", pedido.explicacao());
    }

    @Test
    void conceitoIgnoraAcentoMaiusculaEEspacos() throws Exception {
        QuestaoRequest pedido = LeitorDeQuestoes.ler(json("""
                {"conceito":"  Frações ","dificuldade":1,"enunciado":"1/2 de 10?",
                 "alts":["2","5","8","10"],"correta":"b"}
                """), ASSUNTOS);

        assertEquals(3L, pedido.assuntoId());
        assertEquals("B", pedido.respostaCorreta());
        assertNull(pedido.explicacao());
    }

    @Test
    void recusaConceitoQueNaoEDaMateria() throws Exception {
        var erro = assertThrows(IllegalArgumentException.class, () -> LeitorDeQuestoes.ler(json("""
                {"conceito":"geometria","dificuldade":1,"enunciado":"x","alts":["a","b","c","d"],"correta":0}
                """), ASSUNTOS));

        assertTrue(erro.getMessage().contains("geometria"));
    }

    @Test
    void recusaAlternativasForaDeQuatroEGabaritoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> LeitorDeQuestoes.ler(json("""
                {"conceito":"porcentagem","dificuldade":1,"enunciado":"x","alts":["a","b","c"],"correta":0}
                """), ASSUNTOS));
        assertThrows(IllegalArgumentException.class, () -> LeitorDeQuestoes.ler(json("""
                {"conceito":"porcentagem","dificuldade":1,"enunciado":"x","alts":["a","b","c","d"],"correta":4}
                """), ASSUNTOS));
        assertThrows(IllegalArgumentException.class, () -> LeitorDeQuestoes.ler(json("""
                {"conceito":"porcentagem","dificuldade":11,"enunciado":"x","alts":["a","b","c","d"],"correta":0}
                """), ASSUNTOS));
    }

    @Test
    void recusaItemQueNaoEUmObjeto() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> LeitorDeQuestoes.ler(json("\"texto solto\""), ASSUNTOS));
    }

    @Test
    void recusaNumerosInteirosQueSoFicariamValidosAposTruncamento() {
        assertThrows(IllegalArgumentException.class, () -> LeitorDeQuestoes.ler(json("""
                {"conceito":"porcentagem","dificuldade":4294967297,"enunciado":"x",
                 "alts":["a","b","c","d"],"correta":0}
                """), ASSUNTOS));
        assertThrows(IllegalArgumentException.class, () -> LeitorDeQuestoes.ler(json("""
                {"conceito":"porcentagem","dificuldade":1,"enunciado":"x",
                 "alts":["a","b","c","d"],"correta":18446744073709551616}
                """), ASSUNTOS));
    }
}
