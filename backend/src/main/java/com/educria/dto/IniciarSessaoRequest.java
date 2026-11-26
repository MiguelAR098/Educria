package com.educria.dto;

import jakarta.validation.constraints.Positive;

/** Recebe um assunto explícito ou delega a escolha ao recomendador quando o ID estiver ausente. */
public record IniciarSessaoRequest(@Positive Long assuntoId) {}
