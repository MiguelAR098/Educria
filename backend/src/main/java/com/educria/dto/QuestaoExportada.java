package com.educria.dto;

import java.util.List;

/** Mesmo formato aceito na importação; correta é o índice de 0 a 3 (A a D). */
public record QuestaoExportada(String conceito, Integer dificuldade, String enunciado, List<String> alts,
                               int correta, String explicacao) {}
