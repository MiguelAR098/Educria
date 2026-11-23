package com.educria.service;

import com.educria.dto.QuestaoRequest;
import com.educria.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BancoDeQuestoesServiceTest {

    private static QuestaoRequest pedido(String a, String b, String c, String d, String correta) {
        return new QuestaoRequest(1L, 3, "Quanto é 7 + 8?", a, b, c, d, correta, null);
    }

    @Test
    void aceitaQuestaoValidaEAceitaLetraMinuscula() {
        assertDoesNotThrow(() -> BancoDeQuestoesService.validar(pedido("13", "15", "16", "17", "b")));
    }

    @Test
    void recusaRespostaCorretaForaDeAAteD() {
        var erro = assertThrows(ApiException.class,
                () -> BancoDeQuestoesService.validar(pedido("13", "15", "16", "17", "E")));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
    }

    @Test
    void recusaAlternativasRepetidasIgnorandoMaiusculasEEspacos() {
        var erro = assertThrows(ApiException.class,
                () -> BancoDeQuestoesService.validar(pedido("15", "quinze", " QUINZE ", "17", "A")));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
    }
}
