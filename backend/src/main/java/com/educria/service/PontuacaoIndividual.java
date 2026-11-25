package com.educria.service;

import java.util.Optional;

final class PontuacaoIndividual {
    private PontuacaoIndividual() {}

    static int acerto(Integer dificuldade, Optional<Boolean> acertoAnterior) {
        // Uma questão sem dificuldade pode existir em uma sessão legada.
        if (dificuldade == null) return 0;
        int base = dificuldade * 10;
        if (acertoAnterior.isEmpty()) return base;
        int percentual = acertoAnterior.get() ? 30 : 60;
        return base * percentual / 100;
    }
}
