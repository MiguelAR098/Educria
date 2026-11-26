package com.educria.dto;

import java.util.List;

/** Visão do professor sobre um aluno: situação geral, assuntos da sua matéria e evolução dos últimos 30 dias. */
public record DetalheDoAluno(AlunoDaTurma aluno, Long turmaId, String turmaNome, String materia,
                             List<AssuntoNaMatriz> assuntos, RelatorioEvolucao evolucao) {}
