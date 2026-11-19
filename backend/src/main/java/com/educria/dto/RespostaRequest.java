package com.educria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RespostaRequest {

    @NotNull
    private Long questaoId;

    @NotBlank
    private String resposta; // "A", "B", "C" ou "D"
}
