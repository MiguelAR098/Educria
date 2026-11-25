package com.educria.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PontuacaoIndividualTest {
    @ParameterizedTest
    @CsvSource({"1, 10, 6, 3", "5, 50, 30, 15", "10, 100, 60, 30"})
    void aplicaPercentualDaTentativaAnterior(int dificuldade, int inedita, int recuperacao, int revisao) {
        assertEquals(inedita, PontuacaoIndividual.acerto(dificuldade, Optional.empty()));
        assertEquals(recuperacao, PontuacaoIndividual.acerto(dificuldade, Optional.of(false)));
        assertEquals(revisao, PontuacaoIndividual.acerto(dificuldade, Optional.of(true)));
    }

    @Test
    void conservaCompatibilidadeComQuestaoLegadaSemDificuldade() {
        assertEquals(0, PontuacaoIndividual.acerto(null, Optional.empty()));
    }
}
