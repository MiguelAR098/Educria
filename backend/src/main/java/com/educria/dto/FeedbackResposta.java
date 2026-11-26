package com.educria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Retorno pedagógico da tentativa. A resolução pode incluir o gabarito quando a regra da sessão permite.
 * {@code @JsonInclude} omite campos opcionais ausentes, como pontos de tentativas legadas.
 */
public record FeedbackResposta(String alternativa, boolean correta, String explicacao,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer nivelAnterior,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer nivelAtual, boolean reaprender,
        TipoOrientacao tipoOrientacao,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long errosDaQuestao,
        @JsonInclude(JsonInclude.Include.NON_NULL) String alternativaCorreta,
        @JsonInclude(JsonInclude.Include.NON_NULL) String textoAlternativaCorreta,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer pontosConcedidos) {

    public enum TipoOrientacao {
        EXPLICACAO, DICA_INICIAL, DICA_APRIMORADA, RESOLUCAO
    }
}
