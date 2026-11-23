package com.educria.dto;

import java.util.List;

/** Total coletivo atual da turma e as últimas atividades lançadas (mais recentes primeiro). */
public record HistoricoDeAtividades(int pontosTotais, List<AtividadeColetivaResponse> atividades) {}
