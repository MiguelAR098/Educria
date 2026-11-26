package com.educria.dto;

import java.time.LocalDateTime;

/** Projeção de uma resposta e dos níveis antes e depois dela. Tentativas sem observação de nível deixam ambos nulos. */
public record RespostaNivel(Long alunoId, Long assuntoId, String assunto, Integer nivelAnterior,
                            Integer nivelAtual, Boolean correta, LocalDateTime respondidoEm) {}
