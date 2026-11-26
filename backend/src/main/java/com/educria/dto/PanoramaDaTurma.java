package com.educria.dto;

import java.util.List;

/** Reúne alertas, tendência histórica e indicadores dos alunos na matéria que o professor leciona na turma. */
public record PanoramaDaTurma(Long turmaId, List<AlertaDaTurma> alertas, List<PontoDaTendencia> tendencia,
                              List<IndicadoresDoAluno> alunos) {}
