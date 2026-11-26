package com.educria.service;

import com.educria.service.CalculoDaTurma.AlunoNaTurma;
import com.educria.service.ClassificacaoDaTurma.Classificado;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassificacaoDaTurmaTest {
    private static final AlunoNaTurma ANA = new AlunoNaTurma(1L, "Ana");
    private static final AlunoNaTurma ANA_2 = new AlunoNaTurma(2L, "ana");
    private static final AlunoNaTurma BRUNO = new AlunoNaTurma(3L, "Bruno");
    private static final AlunoNaTurma CARLA = new AlunoNaTurma(4L, "Carla");
    private static final AlunoNaTurma DIEGO = new AlunoNaTurma(5L, "Diego");
    private static final List<AlunoNaTurma> ALUNOS = List.of(DIEGO, CARLA, ANA_2, BRUNO, ANA);

    @Test
    void classificaPontosLongosComEmpatesEDistingueZeroDeAusencia() {
        var ranking = ClassificacaoDaTurma.ordenar(ALUNOS, Map.of(1L, 3_000_000_000L, 2L, 3_000_000_000L, 4L, 0L));
        assertEquals(List.of(ANA, ANA_2, CARLA, BRUNO, DIEGO), ranking.stream().map(Classificado::aluno).toList());
        assertEquals(Arrays.asList(1, 1, 3, null, null), ranking.stream().map(Classificado::posicao).toList());
        assertEquals(Arrays.asList(3_000_000_000L, 3_000_000_000L, 0L, null, null),
                ranking.stream().map(Classificado::valor).toList());
    }

    @Test
    void ordenaEvolucaoPositivaZeroENegativaAntesDaAusencia() {
        var ranking = ClassificacaoDaTurma.ordenar(ALUNOS, Map.of(1L, -1.5, 2L, 0.0, 3L, 2.25, 5L, 0.0));
        assertEquals(List.of(BRUNO, ANA_2, DIEGO, ANA, CARLA), ranking.stream().map(Classificado::aluno).toList());
        assertEquals(Arrays.asList(1, 2, 2, 4, null), ranking.stream().map(Classificado::posicao).toList());
    }

    @Test
    void turmaSemAlunosRetornaListaVazia() {
        assertEquals(List.of(), ClassificacaoDaTurma.ordenar(List.of(), Map.<Long, Long>of()));
    }
}
