package com.educria.dto;

import java.util.List;

/** celulas segue a mesma ordem de MapaDaTurma.assuntos. */
public record LinhaDoMapa(Long alunoId, String nome, List<CelulaDoMapa> celulas) {}
