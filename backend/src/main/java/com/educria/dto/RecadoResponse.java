package com.educria.dto;

import java.time.LocalDateTime;

/** "de" é nulo se o professor que enviou foi removido. */
public record RecadoResponse(Long id, String texto, String de, LocalDateTime criadoEm, boolean lido) {}
