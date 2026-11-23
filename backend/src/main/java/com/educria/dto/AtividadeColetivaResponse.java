package com.educria.dto;

import com.educria.domain.enums.TipoAtividadeColetiva;

import java.time.LocalDateTime;

/** lancadaPor é nulo se o professor que lançou foi removido. */
public record AtividadeColetivaResponse(Long id, TipoAtividadeColetiva tipo, String titulo, int pontos,
                                        String resumo, String lancadaPor, LocalDateTime criadoEm) {}
