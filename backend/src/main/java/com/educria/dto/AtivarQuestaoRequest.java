package com.educria.dto;

import jakarta.validation.constraints.NotNull;

public record AtivarQuestaoRequest(@NotNull(message = "Informe se a questão fica ativa") Boolean ativa) {}
