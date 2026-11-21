package com.educria.dto;

import com.educria.domain.enums.SituacaoSessao;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ResultadoSessao(Indicadores sessao, Indicadores anterior, List<Revisao> revisoes, List<Item> questoes) {
    public record Item(int numero, String enunciado, String alternativa, Boolean correta, Integer passo) {}
    /** Erros contabilizados são os primeiros eventos incorretos do dia atribuídos a esta sessão. */
    public record Indicadores(long id, String assunto, SituacaoSessao situacao, Instant iniciadaEm,
                              int apresentadas, int respondidas, long tentativas, long acertos,
                              long tentativasIncorretas, long erros, Integer nivelAntes, Integer nivelDepois) {}
    public record Revisao(long questaoId, String enunciado, LocalDate proximaRevisao) {}
    public record Historico(List<Indicadores> sessoes, Long proximoCursor) {}
}
