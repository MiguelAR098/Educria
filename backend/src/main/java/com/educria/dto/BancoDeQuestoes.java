package com.educria.dto;

import java.util.List;

/** Questões da matéria do professor e os assuntos disponíveis para cadastro e filtro. */
public record BancoDeQuestoes(String materia, List<AssuntoSimples> assuntos, List<QuestaoDoProfessor> questoes) {}
