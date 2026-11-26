package com.educria.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AgendaRevisoes(LocalDate hoje, Instant proximaVirada,
                            List<RevisaoDoAluno> pendentes, List<RevisaoDoAluno> futuras) {}
