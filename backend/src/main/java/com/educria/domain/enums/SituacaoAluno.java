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
     * - sem respostas ............................ SEM_DADOS
     * - 5+ respostas e taxa abaixo de 40% ........ EM_RISCO
     * - 1+ erro seguido (o próximo erro rebaixa)
     *   ou 5+ respostas e taxa abaixo de 60% ..... ATENCAO
     * - demais casos ............................. NO_CAMINHO
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
