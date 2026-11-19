package com.educria.dto;

import jakarta.validation.constraints.Positive;

public record IniciarSessaoRequest(@Positive Long assuntoId) {}
