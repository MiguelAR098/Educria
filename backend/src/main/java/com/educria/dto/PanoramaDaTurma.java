package com.educria.dto;

import java.util.List;

public record PanoramaDaTurma(Long turmaId, List<AlertaDaTurma> alertas, List<PontoDaTendencia> tendencia,
                              List<IndicadoresDoAluno> alunos) {}
