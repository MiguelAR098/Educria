package com.educria.dto;

import com.educria.domain.enums.TipoAtividadeColetiva;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Pontos aceitos: 20, 40, 60 ou 80 (validado no serviço e no banco). */
public record LancarAtividadeRequest(
        @NotNull(message = "Escolha o tipo da atividade")
        TipoAtividadeColetiva tipo,

        @NotBlank(message = "Dê um título curto para a atividade")
        @Size(max = 120, message = "O título deve ter até 120 caracteres")
        String titulo,

        @NotNull(message = "Escolha os pontos da turma")
        Integer pontos,

        @Size(max = 300, message = "O resumo deve ter até 300 caracteres")
        String resumo) {}
