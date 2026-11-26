package com.educria.dto;

import com.educria.domain.enums.TipoGrupoPedagogico;

import java.time.LocalDateTime;
import java.util.List;

/** Comparação agregada por recorte, com a matéria, o intervalo e as turmas de origem dos participantes. */
public record IndicadoresDoGrupo(Long grupoId, String nome, TipoGrupoPedagogico tipo, Materia materia,
                                 Periodo periodo, String comparacaoEscopo, List<Origem> turmasDeOrigem,
                                 Recorte participantes, Recorte naoParticipantes) {
    public record Materia(Long id, String nome) {}
    public record Periodo(int dias, LocalDateTime inicio, LocalDateTime fimExclusivo, String fuso) {}
    public record Origem(Long id, String nome, long participantes) {}
    public record Recorte(int total, Medida nivel, Medida evolucao30d) {}
    /** As contagens informam a cobertura da medida. Média nula indica que nenhum aluno tinha dados comparáveis. */
    public record Medida(Double media, int comDados, int semDados) {}
}
