package com.educria.dto;

/**
 * Dados que podem ser exibidos antes de o aluno responder. A projeção traz a identificação, a classificação,
 * o enunciado e as alternativas da questão, deixando o gabarito e a explicação para o fluxo de correção.
 */
public record QuestaoParaAluno(Long id, Long assuntoId, int dificuldade, String enunciado,
                              String alternativaA, String alternativaB, String alternativaC,
                              String alternativaD) {}
