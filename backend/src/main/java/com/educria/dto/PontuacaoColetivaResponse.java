package com.educria.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PontuacaoColetivaResponse {
    private Long turmaId;
    private String nomeTurma;
    private int pontosTotais;
}
