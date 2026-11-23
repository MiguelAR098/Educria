package com.educria.dto;

import java.util.List;

/** Últimos recados do aluno (mais recentes primeiro) e quantos ainda não foram lidos. */
public record RecadosDoAluno(int naoLidos, List<RecadoResponse> recados) {}
