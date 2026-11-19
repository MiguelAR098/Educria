package com.educria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record FeedbackResposta(String alternativa, boolean correta, String explicacao,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer nivelAnterior,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer nivelAtual) {}
