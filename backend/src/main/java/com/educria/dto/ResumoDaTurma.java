package com.educria.dto;

/** Nível médio considera só alunos que já responderam; nulo se ninguém respondeu. */
public record ResumoDaTurma(int totalAlunos, Double nivelMedio, int emRisco, int atencao, int noCaminho, int semDados) {}
