package com.educria.dto;

import com.educria.domain.enums.TipoGrupoPedagogico;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class GrupoPedagogicoDto {
    private GrupoPedagogicoDto() {}

    public record Criacao(@NotBlank @Size(max = 120) String nome, @Size(max = 1000) String descricao,
                          @NotNull TipoGrupoPedagogico tipo,
                          @NotEmpty @Size(max = 50) List<@NotNull @Positive Long> turmasIds) {}

    public record Atualizacao(@NotBlank @Size(max = 120) String nome, @Size(max = 1000) String descricao) {}

    public record Inclusao(@NotEmpty @Size(max = 100) List<@NotNull @Positive Long> alunosIds) {}

    public record Turma(Long id, String nome) {}

    public record Resumo(Long id, String nome, String descricao, TipoGrupoPedagogico tipo,
                         Instant criadoEm, Instant arquivadoEm, List<Turma> turmas) {}

    public record Participante(Long alunoId, String nome, Long turmaOrigemId, Long turmaAtualId, Instant entradaEm) {}

    public record Detalhe(Resumo grupo, List<Participante> participantes) {}
}
