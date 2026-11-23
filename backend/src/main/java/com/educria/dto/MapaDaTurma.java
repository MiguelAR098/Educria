package com.educria.dto;

import java.util.List;

/**
 * Nível de cada aluno em cada assunto da matéria do professor.
 * medias segue a ordem de assuntos; maisFrageis traz até 4 assuntos, do menor nível médio para o maior.
 */
public record MapaDaTurma(Long turmaId, String turmaNome, String materia, List<AssuntoSimples> assuntos,
                          List<LinhaDoMapa> alunos, List<AssuntoDaTurma> medias, List<AssuntoDaTurma> maisFrageis) {}
