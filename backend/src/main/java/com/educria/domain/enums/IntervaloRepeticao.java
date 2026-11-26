package com.educria.domain.enums;

import java.util.Arrays;

/**
 * Intervalos fixos de 1, 3, 7, 16, 35 e 70 dias. A ordem das constantes define a progressão,
 * e o mapeamento da entidade persiste seus nomes, como {@code D1} e {@code D70}.
 */
public enum IntervaloRepeticao {

    D1(1),
    D3(3),
    D7(7),
    D16(16),
    D35(35),
    D70(70);

    private final int dias;

    IntervaloRepeticao(int dias) {
        this.dias = dias;
    }

    public int getDias() {
        return dias;
    }

    /** Retorna o próximo intervalo da sequência ou conserva o atual ao atingir o teto. */
    public IntervaloRepeticao proximo() {
        IntervaloRepeticao[] valores = values();
        int idx = this.ordinal();
        if (idx < valores.length - 1) {
            return valores[idx + 1];
        }
        return this; // já no máximo
    }

    public static IntervaloRepeticao doPrimeiro() {
        return D1;
    }

    public static IntervaloRepeticao fromDias(int dias) {
        return Arrays.stream(values())
                .filter(i -> i.dias == dias)
                .findFirst()
                .orElse(D1);
    }
}
