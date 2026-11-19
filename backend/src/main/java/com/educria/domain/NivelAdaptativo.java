package com.educria.domain;

/** Regra de progressão comum ao diagnóstico global e ao de cada assunto. */
public record NivelAdaptativo(int nivel, int errosConsecutivos) {

    public static final int MINIMO = 1;
    public static final int MAXIMO = 10;

    public NivelAdaptativo {
        if (nivel < MINIMO || nivel > MAXIMO) {
            throw new IllegalArgumentException("O nível deve estar entre 1 e 10");
        }
    }

    public NivelAdaptativo aposResposta(boolean acertou) {
        if (acertou) {
            return new NivelAdaptativo(Math.min(nivel + 1, MAXIMO), 0);
        }

        int erros = errosConsecutivos + 1;
        if (erros >= 2 && nivel > MINIMO) {
            return new NivelAdaptativo(nivel - 1, 0);
        }
        return new NivelAdaptativo(nivel, erros);
    }
}
