package com.educria.dto;

import java.time.LocalDateTime;

/** Uma resposta com o nível do assunto antes e depois dela (nulos em respostas antigas, sem sessão). */
public record RespostaNivel(Long alunoId, Long assuntoId, String assunto, Integer nivelAnterior,
                            Integer nivelAtual, Boolean correta, LocalDateTime respondidoEm) {}
