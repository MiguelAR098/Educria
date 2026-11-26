package com.educria.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Relatório de um aluno com janela inclusiva, totais, série diária e detalhamento por assunto. */
public record RelatorioEvolucao(LocalDate dataInicial, LocalDate dataFinal, Resumo resumo,
                                List<Dia> evolucaoDiaria, List<Assunto> assuntos) {
    /**
     * A taxa é a porcentagem de acertos entre as primeiras respostas de cada par sessão–questão
     * ocorridas no período. Sem primeiras respostas identificáveis, a taxa é nula.
     * Erros contabilizados somam um por aluno, questão e dia, atribuído ao assunto da primeira resposta incorreta.
     */
    public record Resumo(long sessoesIniciadas, long tentativas, long acertos, long tentativasIncorretas, long erros,
                         long questoesUnicas, long tentativasReaprendizagem,
                         BigDecimal taxaAcertoPrimeiraTentativa) {}

    public record Dia(LocalDate data, long tentativas, long acertos, long tentativasIncorretas, long erros) {}

    /** Os níveis são os extremos das respostas do período e podem ser nulos em registros legados. */
    public record Assunto(long assuntoId, String nome, long sessoesIniciadas, long tentativas,
                          long acertos, long tentativasIncorretas, long erros, long questoesUnicas,
                          BigDecimal taxaAcertoPrimeiraTentativa,
                          Integer nivelInicialObservado, Integer nivelFinalObservado) {}
}
