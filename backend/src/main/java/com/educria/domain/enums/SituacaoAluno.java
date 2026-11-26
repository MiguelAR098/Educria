package com.educria.domain.enums;

/**
 * Situação do aluno do ponto de vista do professor (calculada, não persistida).
 */
public enum SituacaoAluno {
    SEM_DADOS,
    EM_RISCO,
    ATENCAO,
    NO_CAMINHO;

    /** Mínimo de respostas para a taxa de acerto ser considerada confiável. */
    public static final int MINIMO_RESPOSTAS = 5;
    /** Taxa de acerto (%) abaixo da qual o aluno está em risco. */
    public static final double LIMITE_RISCO = 40.0;
    /** Taxa de acerto (%) abaixo da qual o aluno merece atenção. */
    public static final double LIMITE_ATENCAO = 60.0;

    /**
     * Classifica o acompanhamento pedagógico usando respostas, taxa de acerto e erros consecutivos.
     * Sem respostas ou taxa calculável, a situação é {@link #SEM_DADOS}.
     * Com pelo menos cinco respostas, taxa abaixo de 40% indica {@link #EM_RISCO}.
     * Se a condição de risco não se aplica, um erro consecutivo ou taxa abaixo de 60% com amostra
     * suficiente indica {@link #ATENCAO}. Os demais casos são {@link #NO_CAMINHO}.
     */
    public static SituacaoAluno calcular(int respostas, Double taxaAcerto, int errosConsecutivos) {
        if (respostas == 0 || taxaAcerto == null) {
            return SEM_DADOS;
        }
        boolean amostraSuficiente = respostas >= MINIMO_RESPOSTAS;
        if (amostraSuficiente && taxaAcerto < LIMITE_RISCO) {
            return EM_RISCO;
        }
        if (errosConsecutivos >= 1 || (amostraSuficiente && taxaAcerto < LIMITE_ATENCAO)) {
            return ATENCAO;
        }
        return NO_CAMINHO;
    }
}
