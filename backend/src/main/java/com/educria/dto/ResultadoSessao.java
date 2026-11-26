package com.educria.dto;

import com.educria.domain.enums.SituacaoSessao;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado privado da sessão, com referência comparável quando disponível e pontos no assunto e na matéria.
 * Uma questão pode contribuir com vários itens de tentativa. Questões sem resposta conservam o enunciado.
 */
public record ResultadoSessao(Indicadores sessao, Indicadores anterior, List<Revisao> revisoes, List<Item> questoes,
                              long pontosIndividuaisNoAssunto, long pontosIndividuaisNaMateria) {
    public record Item(int numero, String enunciado, String alternativa, Boolean correta, Integer passo) {}
    /**
     * Erros contabilizados são os primeiros eventos incorretos do dia atribuídos a esta sessão.
     * Pontuação completa indica que todos os registros de resposta têm um valor persistido.
     */
    public record Indicadores(long id, String assunto, SituacaoSessao situacao, Instant iniciadaEm,
                              int apresentadas, int respondidas, long tentativas, long acertos,
                              long tentativasIncorretas, long erros, Integer nivelAntes, Integer nivelDepois,
                              Long pontosIndividuais, boolean pontuacaoCompleta) {}
    public record Revisao(long questaoId, String enunciado, LocalDate proximaRevisao) {}
    public record Historico(List<Indicadores> sessoes, Long proximoCursor) {}
}
