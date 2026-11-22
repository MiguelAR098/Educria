package com.educria.dto;

import java.util.List;

public record TurmaComAlunos(Long id, String nome, String codigo, Integer anoLetivo,
                             ResumoDaTurma resumo, List<AlunoDaTurma> alunos) {}
