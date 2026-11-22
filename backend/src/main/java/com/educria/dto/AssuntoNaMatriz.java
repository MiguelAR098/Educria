package com.educria.dto;

import com.educria.domain.enums.SituacaoAluno;

/** Taxa de acerto em % (0 a 100), nula se o aluno ainda não praticou o assunto. */
public record AssuntoNaMatriz(Long id, String nome, int nivelAtual, int errosConsecutivos,
                              int acertos, int erros, Double taxaAcerto, SituacaoAluno situacao) {}
