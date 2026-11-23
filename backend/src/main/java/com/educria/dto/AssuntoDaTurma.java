package com.educria.dto;

/** Nível médio só entre quem praticou o assunto; nulo se ninguém praticou. */
public record AssuntoDaTurma(Long assuntoId, String nome, Double nivelMedio, int noNivelBaixo, int alunosQuePraticaram) {}
