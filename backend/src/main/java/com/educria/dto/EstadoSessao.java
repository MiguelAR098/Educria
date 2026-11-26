package com.educria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Resumo devolvido ao aluno após uma consulta ou transição da sessão.
 * O feedback só aparece quando a tentativa atual já tem uma resposta registrada.
 */
public record EstadoSessao(ResumoSessao sessao, QuestaoParaAluno questaoAtual,
                          @JsonInclude(JsonInclude.Include.NON_NULL) FeedbackResposta feedback,
                          int passo, boolean repetindo, int pendencias, long pontosSessao) {}
