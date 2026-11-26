package com.educria.service;

import com.educria.domain.NivelAdaptativo;
import com.educria.domain.entity.DesempenhoPorAssunto;
import com.educria.domain.enums.SituacaoAluno;

import java.util.Collection;

record ResumoDoDesempenho(Double nivelMedio, int acertos, int erros, int errosConsecutivos) {
    static ResumoDoDesempenho dosAssuntos(Collection<DesempenhoPorAssunto> assuntos) {
        var medidas = new CalculoDaTurma.MedidasDeAprendizagem();
        int acertos = 0;
        int erros = 0;
        int praticados = 0;
        int consecutivos = 0;
        for (var assunto : assuntos) {
            if (assunto.getTotalAcertos() == 0 && assunto.getTotalErros() == 0) continue;
            medidas.registrar(assunto.getNivelAtual(), null);
            acertos += assunto.getTotalAcertos();
            erros += assunto.getTotalErros();
            praticados++;
            consecutivos = assunto.getErrosConsecutivos();
        }
        // A sequência pertence ao assunto. O campo inteiro do resumo não representa uma sequência entre assuntos.
        return new ResumoDoDesempenho(medidas.nivelMedio(), acertos, erros, praticados == 1 ? consecutivos : 0);
    }

    int nivelAtual() {
        return nivelMedio == null ? NivelAdaptativo.MINIMO : (int) Math.round(nivelMedio);
    }

    Double taxaAcerto() {
        return taxaAcerto(acertos, erros);
    }

    static Double taxaAcerto(int acertos, int erros) {
        long respostas = (long) acertos + erros;
        return respostas > 0 ? Math.round(acertos * 1000.0 / respostas) / 10.0 : null;
    }

    SituacaoAluno situacao() {
        return SituacaoAluno.calcular(acertos + erros, taxaAcerto(), errosConsecutivos);
    }
}
