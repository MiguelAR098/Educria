package com.educria.dto;

import java.util.List;

/**
 * Rankings para o professor autorizado da turma. A pontuação é acumulada na matéria,
 * enquanto a evolução compara níveis no intervalo do panorama.
 * Uma posição nula representa ausência da medida correspondente.
 */
public record RankingsDaTurma(Long turmaId, Materia materia, int periodoEvolucaoDias,
                             List<Pontuacao> rankingPontuacao, List<Evolucao> rankingEvolucao) {
    public record Materia(Long id, String nome) {}
    public record Pontuacao(Integer posicao, Long alunoId, String nome, Long pontos) {}
    public record Evolucao(Integer posicao, Long alunoId, String nome, Double evolucao30d) {}
}
