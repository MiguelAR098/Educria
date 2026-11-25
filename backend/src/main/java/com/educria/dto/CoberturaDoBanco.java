package com.educria.dto;

import java.util.List;

public record CoberturaDoBanco(List<Assunto> assuntos, long total, long combinacoesVazias) {
    /** A primeira contagem corresponde à dificuldade 1. */
    public record Assunto(Long id, String nome, List<Long> contagens, long total) {}
}
