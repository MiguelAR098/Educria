package com.educria.domain.entity;

import com.educria.domain.NivelAdaptativo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DesempenhoPorAssuntoTest {

    @Test
    void iniciaNoPisoEContaErrosMesmoSemPoderRebaixar() {
        var d = new DesempenhoPorAssunto();
        assertEquals(1, d.getNivelAtual());
        d.registrarResposta(false);
        d.registrarResposta(false);
        assertEquals(1, d.getNivelAtual());
        assertEquals(2, d.getTotalErros());
        assertEquals(2, d.getErrosConsecutivos());

        d.registrarResposta(true);
        assertEquals(2, d.getNivelAtual());
        assertEquals(0, d.getErrosConsecutivos());
    }

    @Test
    void contaAcertosNoTetoEProtegeApenasOPrimeiroErroDaSequencia() {
        var d = new DesempenhoPorAssunto();
        for (int i = 0; i < 12; i++) {
            d.registrarResposta(true);
        }
        assertEquals(10, d.getNivelAtual());
        assertEquals(12, d.getTotalAcertos());
        d.registrarResposta(false);
        assertEquals(10, d.getNivelAtual());
        assertEquals(1, d.getErrosConsecutivos());
        d.registrarResposta(false);
        assertEquals(9, d.getNivelAtual());
        assertEquals(0, d.getErrosConsecutivos());
        d.registrarResposta(false);
        assertEquals(9, d.getNivelAtual());
    }

    @Test
    void acertoInterrompeSequenciaDeErrosNoMesmoAssunto() {
        var d = new DesempenhoPorAssunto();
        d.registrarResposta(true);
        d.registrarResposta(false);
        d.registrarResposta(true);
        d.registrarResposta(false);
        assertEquals(3, d.getNivelAtual());
        assertEquals(1, d.getErrosConsecutivos());
        assertEquals(2, d.getTotalAcertos());
        assertEquals(2, d.getTotalErros());
    }

    @Test
    void resultadosEmOutroAssuntoPreservamNivelTotaisESequenciaDoPrimeiro() {
        var fracoes = new DesempenhoPorAssunto();
        var equacoes = new DesempenhoPorAssunto();
        fracoes.registrarResposta(true);
        fracoes.registrarResposta(false);
        equacoes.registrarResposta(true);
        fracoes.registrarResposta(false);
        assertEquals(1, fracoes.getNivelAtual());
        assertEquals(2, fracoes.getTotalErros());
        assertEquals(1, fracoes.getTotalAcertos());
        assertEquals(2, equacoes.getNivelAtual());
        assertEquals(0, equacoes.getTotalErros());
        assertEquals(0, equacoes.getErrosConsecutivos());
    }

    @Test
    void preservaIndicadoresGlobaisDeUmAlunoComHistorico() {
        var global = DesempenhoAdaptativo.builder()
                .nivelAtual(10).errosConsecutivos(1).totalAcertos(21).totalErros(8).build();
        assertTrue(global.isNivelMaximo());
        global.registrarResposta(false);
        assertEquals(9, global.getNivelAtual());
        assertEquals(9, global.getTotalErros());
        assertEquals(0, global.getErrosConsecutivos());
        assertFalse(global.isNivelMaximo());

        global.registrarResposta(true);
        assertEquals(22, global.getTotalAcertos());
        assertTrue(global.isNivelMaximo());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11})
    void rejeitaNivelForaDaEscala(int nivel) {
        assertThrows(IllegalArgumentException.class, () -> new NivelAdaptativo(nivel, 0));
    }
}
