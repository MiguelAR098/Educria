package com.educria.dto;

import java.util.List;

/**
 * evolucao30d é a variação do nível médio nos assuntos da matéria (nula se o aluno não respondeu nos últimos
 * 30 dias). erraMaisEm traz até 2 assuntos com 2 ou mais erros no período.
 */
public record IndicadoresDoAluno(Long alunoId, Double evolucao30d, int revisoesVencidas, List<String> erraMaisEm) {}
