package com.educria.service;

import com.educria.dto.AssuntoSimples;
import com.educria.dto.MapaDaTurma;
import com.educria.service.CalculoDoMapa.AlunoNoMapa;
import com.educria.service.CalculoDoMapa.Registro;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculoDoMapaTest {
    private static final List<AssuntoSimples> ASSUNTOS = List.of(
            new AssuntoSimples(1L, "frações"), new AssuntoSimples(2L, "porcentagem"), new AssuntoSimples(3L, "equações"));
    private static final List<AlunoNoMapa> ALUNOS = List.of(
            new AlunoNoMapa(10L, "Ana"), new AlunoNoMapa(11L, "Bruno"), new AlunoNoMapa(12L, "Carla"));

    private static MapaDaTurma calcular(List<Registro> registros) {
        return CalculoDoMapa.calcular(5L, "8º A", "Matemática", ASSUNTOS, ALUNOS, registros);
    }

    @Test
    void celulasSeguemAOrdemDosAssuntosEAlunoSemRegistroComecaNoNivel1SemPratica() {
        MapaDaTurma mapa = calcular(List.of(new Registro(10L, 2L, 4, 6)));

        var ana = mapa.alunos().get(0).celulas();
        assertEquals(4, ana.get(1).nivel());
        assertTrue(ana.get(1).praticou());
        assertEquals(1, ana.get(0).nivel());
        assertFalse(ana.get(0).praticou());
        assertFalse(mapa.alunos().get(2).celulas().get(1).praticou());
    }

    @Test
    void nivelNaoPraticadoNaoEntraNaMedia() {
        MapaDaTurma mapa = calcular(List.of(
                new Registro(10L, 1L, 2, 5),
                new Registro(11L, 1L, 6, 5),
                new Registro(12L, 1L, 1, 0)));

        var fracoes = mapa.medias().get(0);
        assertEquals(4.0, fracoes.nivelMedio());
        assertEquals(2, fracoes.alunosQuePraticaram());
        assertEquals(1, fracoes.noNivelBaixo());
        assertNull(mapa.medias().get(2).nivelMedio());
    }

    @Test
    void maisFrageisOrdenaPeloMenorNivelMedioEIgnoraAssuntoSemPratica() {
        MapaDaTurma mapa = calcular(List.of(
                new Registro(10L, 1L, 8, 5),
                new Registro(10L, 2L, 2, 5),
                new Registro(11L, 2L, 3, 5)));

        assertEquals(2, mapa.maisFrageis().size());
        assertEquals("porcentagem", mapa.maisFrageis().get(0).nome());
        assertEquals("frações", mapa.maisFrageis().get(1).nome());
        assertEquals(1, mapa.medias().get(1).noNivelBaixo());
    }
}
