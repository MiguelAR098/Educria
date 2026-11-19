package com.educria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record EstadoSessao(ResumoSessao sessao, QuestaoParaAluno questaoAtual,
                          @JsonInclude(JsonInclude.Include.NON_NULL) FeedbackResposta feedback) {
    public EstadoSessao(ResumoSessao sessao, QuestaoParaAluno questaoAtual) {
        this(sessao, questaoAtual, null);
    }
}
