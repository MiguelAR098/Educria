package com.educria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovoAssuntoRequest(
        @NotBlank(message = "Escreva o nome do assunto")
        @Size(max = 100, message = "O nome do assunto deve ter até 100 caracteres")
        String nome) {}
