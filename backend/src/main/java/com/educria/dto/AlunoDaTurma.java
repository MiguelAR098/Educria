package com.educria.dto;

import com.educria.domain.enums.SituacaoAluno;

import java.time.LocalDateTime;

/** Taxa de acerto em % (0 a 100), nula se o aluno ainda não respondeu. */
public record AlunoDaTurma(Long id, String nome, String matricula, int nivelAtual, int errosConsecutivos,
                           int acertos, int erros, Double taxaAcerto, SituacaoAluno situacao,
                           LocalDateTime ultimaAtividade) {}
