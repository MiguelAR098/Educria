package com.educria.domain;

/**
 * Estado imutável da progressão, reutilizado pelo diagnóstico de cada assunto e pelo diagnóstico global legado.
 * O {@code record} reúne o nível e a sequência de erros. As entidades guardam os valores resultantes no banco.
 */
public record NivelAdaptativo(int nivel, int errosConsecutivos) {

    public static final int MINIMO = 1;
    public static final int MAXIMO = 10;

    public NivelAdaptativo {
        if (nivel < MINIMO || nivel > MAXIMO) {
            throw new IllegalArgumentException("O nível deve estar entre 1 e 10");
        }
    }

    /**
     * Um acerto sobe um nível até 10 e limpa a sequência. O primeiro erro preserva o nível.
     * A partir do segundo erro seguido, um nível acima de 1 cai um ponto e a sequência recomeça.
     * No nível 1, os erros mantêm o piso e continuam sendo contados até um acerto.
     *
     * @param acertou resultado da resposta que participa da progressão
     * @return novo estado, com o nível e a sequência após a resposta
     */
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
