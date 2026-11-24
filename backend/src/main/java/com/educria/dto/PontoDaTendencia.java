package com.educria.dto;

import java.time.LocalDate;

/** Nível médio da turma ao fim do dia; nulo se ninguém tinha praticado ainda. */
public record PontoDaTendencia(LocalDate dia, Double nivelMedio) {}
