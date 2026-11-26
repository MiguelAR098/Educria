package com.educria.service;

import java.util.Optional;

/**
 * Calcula os pontos de um acerto sem efeitos colaterais. O serviço grava o valor na tentativa
 * e atribui zero nos erros. A pontuação coletiva é registrada separadamente nas operações de turma.
 */
final class PontuacaoIndividual {
    private PontuacaoIndividual() {}

    /**
     * A base vale dez vezes a dificuldade. Um acerto sem tentativa anterior vale 100%.
     * Se a última tentativa foi um erro, vale 60%. Se foi um acerto, vale 30%.
     *
     * @param dificuldade dificuldade da questão ou {@code null} em questões legadas
     * @param acertoAnterior resultado mais recente do mesmo aluno para esta questão, em qualquer sessão
     * @return pontos inteiros do acerto, ou zero quando a dificuldade está ausente
     */
    static int acerto(Integer dificuldade, Optional<Boolean> acertoAnterior) {
        // Uma questão sem dificuldade pode existir em uma sessão legada.
        if (dificuldade == null) return 0;
        int base = dificuldade * 10;
        if (acertoAnterior.isEmpty()) return base;
        int percentual = acertoAnterior.get() ? 30 : 60;
        return base * percentual / 100;
    }
}
