package com.educria.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Dados validados pelo controlador antes da correção. O passo positivo identifica a tentativa.
 * Mantê-lo opcional permite que clientes antigos ainda enviem a primeira resposta à questão.
 */
public record EnviarResposta(
        @NotNull(message = "Informe uma alternativa")
        @Pattern(regexp = "[A-D]", message = "Escolha A, B, C ou D")
        String alternativa, @jakarta.validation.constraints.Positive Integer passo) {}
