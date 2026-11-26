package com.educria.dto;

/** Dados que podem ser exibidos antes de o aluno responder. */
public record QuestaoParaAluno(Long id, Long assuntoId, int dificuldade, String enunciado,
                              String alternativaA, String alternativaB, String alternativaC,
                              String alternativaD) {}
