package com.educria.dto;

import com.educria.domain.enums.TipoGrupoPedagogico;

import java.time.LocalDateTime;
import java.util.List;

public record IndicadoresDoGrupo(Long grupoId, String nome, TipoGrupoPedagogico tipo, Materia materia,
                                 Periodo periodo, String comparacaoEscopo, List<Origem> turmasDeOrigem,
                                 Recorte participantes, Recorte naoParticipantes) {
    public record Materia(Long id, String nome) {}
    public record Periodo(int dias, LocalDateTime inicio, LocalDateTime fimExclusivo, String fuso) {}
    public record Origem(Long id, String nome, long participantes) {}
    public record Recorte(int total, Medida nivel, Medida evolucao30d) {}
    public record Medida(Double media, int comDados, int semDados) {}
}
