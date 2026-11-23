package com.educria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarRecadoRequest(
        @NotBlank(message = "Escreva o recado")
        @Size(max = 500, message = "O recado deve ter até 500 caracteres")
        String texto) {}
