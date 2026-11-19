package com.educria.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EnviarResposta(
        @NotNull(message = "Informe uma alternativa")
        @Pattern(regexp = "[A-D]", message = "Escolha A, B, C ou D")
        String alternativa) {}
