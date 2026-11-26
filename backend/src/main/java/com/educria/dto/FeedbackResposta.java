package com.educria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record FeedbackResposta(String alternativa, boolean correta, String explicacao,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer nivelAnterior,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer nivelAtual, boolean reaprender,
        TipoOrientacao tipoOrientacao,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long errosDaQuestao,
        @JsonInclude(JsonInclude.Include.NON_NULL) String alternativaCorreta,
        @JsonInclude(JsonInclude.Include.NON_NULL) String textoAlternativaCorreta) {

    public enum TipoOrientacao {
        EXPLICACAO, DICA_INICIAL, DICA_APRIMORADA, RESOLUCAO
    }
}
