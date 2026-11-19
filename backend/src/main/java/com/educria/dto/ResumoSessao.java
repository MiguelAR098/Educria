package com.educria.dto;

import com.educria.domain.enums.SituacaoSessao;
import java.time.Instant;

public record ResumoSessao(Long id, Long assuntoId, SituacaoSessao situacao,
                          int questoesApresentadas, Instant iniciadaEm, Instant atualizadaEm) {}
