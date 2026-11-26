package com.educria.dto;

import java.time.LocalDateTime;

/** Resposta com o nome do remetente quando há professor vinculado ao recado. */
public record RecadoResponse(Long id, String texto, String de, LocalDateTime criadoEm, boolean lido) {}
