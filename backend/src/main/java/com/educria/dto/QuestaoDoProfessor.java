package com.educria.dto;

/**
 * Visão completa da questão para o professor (inclui o gabarito e a explicação).
 * taxaAcerto em % (0 a 100), nula se ninguém respondeu ainda.
 */
public record QuestaoDoProfessor(Long id, Long assuntoId, String assunto, Integer dificuldade, String enunciado,
                                 String alternativaA, String alternativaB, String alternativaC, String alternativaD,
                                 String respostaCorreta, String explicacao, boolean ativa, String autor,
                                 long totalRespostas, Double taxaAcerto) {}
