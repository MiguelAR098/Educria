package com.educria.domain.enums;

import org.junit.jupiter.api.Test;

import static com.educria.domain.enums.SituacaoAluno.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SituacaoAlunoTest {

    @Test
    void semRespostasNaoTemSituacao() {
        assertEquals(SEM_DADOS, calcular(0, null, 0));
    }

    @Test
    void taxaBaixaComAmostraSuficienteEstaEmRisco() {
        assertEquals(EM_RISCO, calcular(10, 20.0, 0));
        assertEquals(EM_RISCO, calcular(5, 39.9, 0));
    }

    @Test
    void amostraPequenaNaoGeraRiscoPelaTaxa() {
        assertEquals(NO_CAMINHO, calcular(4, 0.0, 0));
    }

    @Test
    void erroSeguidoPedeAtencaoMesmoComTaxaBoa() {
        assertEquals(ATENCAO, calcular(10, 90.0, 1));
        assertEquals(ATENCAO, calcular(2, 50.0, 1));
    }

    @Test
    void taxaIntermediariaPedeAtencao() {
        assertEquals(ATENCAO, calcular(10, 40.0, 0));
        assertEquals(ATENCAO, calcular(10, 59.9, 0));
    }

    @Test
    void taxaBoaEstaNoCaminho() {
        assertEquals(NO_CAMINHO, calcular(10, 60.0, 0));
        assertEquals(NO_CAMINHO, calcular(10, 100.0, 0));
    }
}
