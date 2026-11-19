package com.educria.dto;

import com.educria.domain.enums.IntervaloRepeticao;
import java.time.LocalDate;

public record RevisaoDoAluno(Long questaoId, String enunciado, String assunto,
                            LocalDate proximaRevisao, int intervaloDias) {
    public RevisaoDoAluno(Long questaoId, String enunciado, String assunto,
                          LocalDate proximaRevisao, IntervaloRepeticao intervalo) {
        this(questaoId, enunciado, assunto, proximaRevisao, intervalo.getDias());
    }
}
