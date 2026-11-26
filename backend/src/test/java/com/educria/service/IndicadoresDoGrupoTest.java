package com.educria.service;

import com.educria.dto.IndicadoresDoGrupo.Medida;
import com.educria.repository.IndicadoresDoGrupoRepository.AlunoDoRecorte;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndicadoresDoGrupoTest {
    @Test
    void calculaAsDuasMediasComPesoIgualPorEstudanteEDenominadoresProprios() {
        var ana = new CalculoDaTurma.MedidasDeAprendizagem();
        ana.registrar(6, 2);
        ana.registrar(8, 4);
        var bruno = new CalculoDaTurma.MedidasDeAprendizagem();
        bruno.registrar(3, 3);
        var coorte = List.of(aluno(1), aluno(2), aluno(3));

        var resultado = IndicadoresDoGrupoService.resumir(coorte, Map.of(1L, ana, 2L, bruno), Set.of(1L), 8);

        assertEquals(3, resultado.total());
        assertEquals(new Medida(5.0, 2, 1), resultado.nivel());
        assertEquals(new Medida(1.0, 1, 2), resultado.evolucao30d());
    }

    @Test
    void preservaZeroMedidoEUsaNullQuandoNaoHaDados() {
        var medidas = new CalculoDaTurma.MedidasDeAprendizagem();
        medidas.registrar(2, 2);
        var comDados = IndicadoresDoGrupoService.resumir(List.of(aluno(1)), Map.of(1L, medidas), Set.of(1L), 8);
        var semDados = IndicadoresDoGrupoService.resumir(List.of(aluno(2)), Map.of(), Set.of(2L), 8);
        var vazio = IndicadoresDoGrupoService.resumir(List.of(), Map.of(), Set.of(), 0);

        assertEquals(new Medida(0.0, 1, 0), comDados.evolucao30d());
        assertEquals(new Medida(null, 0, 1), semDados.nivel());
        assertEquals(new Medida(null, 0, 1), semDados.evolucao30d());
        assertEquals(new Medida(null, 0, 0), vazio.evolucao30d());
    }

    private static AlunoDoRecorte aluno(long id) {
        return new AlunoDoRecorte(id, null, null);
    }
}
