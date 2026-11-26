package com.educria.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.educria.service.CalculoDaTurma.AlunoNaTurma;

/** Reutiliza a ordenação dos rankings privados para métricas comparáveis, como pontos e evolução. */
final class ClassificacaoDaTurma {
    private ClassificacaoDaTurma() {}

    record Classificado<T>(Integer posicao, AlunoNaTurma aluno, T valor) {}

    /**
     * Ordena por valor decrescente. Empates recebem a mesma posição e a posição seguinte considera
     * todos os alunos anteriores. Nome e identificador estabilizam a ordem entre iguais.
     * Alunos sem dados aparecem ao final e ficam sem posição.
     */
    static <T extends Comparable<? super T>> List<Classificado<T>> ordenar(List<AlunoNaTurma> alunos,
                                                                          Map<Long, T> valores) {
        var ordenados = alunos.stream()
                .sorted(Comparator.comparing((AlunoNaTurma aluno) -> valores.get(aluno.id()),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AlunoNaTurma::nome, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AlunoNaTurma::id))
                .toList();
        List<Classificado<T>> resultado = new ArrayList<>(ordenados.size());
        T anterior = null;
        int posicao = 0;
        for (AlunoNaTurma aluno : ordenados) {
            T valor = valores.get(aluno.id());
            if (!Objects.equals(valor, anterior)) posicao = resultado.size() + 1;
            resultado.add(new Classificado<>(valor == null ? null : posicao, aluno, valor));
            anterior = valor;
        }
        return resultado;
    }
}
