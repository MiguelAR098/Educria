package com.educria.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Agenda do titular. A virada do dia permite atualizar no cliente a separação entre revisões pendentes e futuras. */
public record AgendaRevisoes(LocalDate hoje, Instant proximaVirada,
                            List<RevisaoDoAluno> pendentes, List<RevisaoDoAluno> futuras) {}
