package com.educria.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DesempenhoResponse {
    private int nivelAtual;
    private int errosConsecutivos;
    private int totalAcertos;
    private int totalErros;
    private boolean nivelMaximo;
    private long pontosIndividuais;
}
