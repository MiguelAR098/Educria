package com.educria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record EstadoSessao(ResumoSessao sessao, QuestaoParaAluno questaoAtual,
                          @JsonInclude(JsonInclude.Include.NON_NULL) FeedbackResposta feedback,
                          int passo, boolean repetindo, int pendencias, long pontosSessao) {}
