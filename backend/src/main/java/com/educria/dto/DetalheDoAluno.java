package com.educria.dto;

import java.util.List;

/** Visão do professor sobre o aluno da turma, com situação, assuntos e evolução restritos à matéria que leciona. */
public record DetalheDoAluno(AlunoDaTurma aluno, Long turmaId, String turmaNome, String materia,
                             List<AssuntoNaMatriz> assuntos, RelatorioEvolucao evolucao) {}
