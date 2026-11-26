package com.educria.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entrada do banco de questões. As anotações conferem presença, tamanho e dificuldade de 1 a 10.
 * O serviço verifica a letra do gabarito, a distinção entre as alternativas e o acesso ao assunto.
 */
public record QuestaoRequest(
        @NotNull(message = "Escolha o assunto")
        Long assuntoId,

        @NotNull(message = "Informe a dificuldade")
        @Min(value = 1, message = "A dificuldade vai de 1 a 10")
        @Max(value = 10, message = "A dificuldade vai de 1 a 10")
        Integer dificuldade,

        @NotBlank(message = "Escreva o enunciado")
        @Size(max = 2000, message = "O enunciado deve ter até 2000 caracteres")
        String enunciado,

        @NotBlank(message = "Preencha a alternativa A")
        @Size(max = 500, message = "Cada alternativa deve ter até 500 caracteres")
        String alternativaA,

        @NotBlank(message = "Preencha a alternativa B")
        @Size(max = 500, message = "Cada alternativa deve ter até 500 caracteres")
        String alternativaB,

        @NotBlank(message = "Preencha a alternativa C")
        @Size(max = 500, message = "Cada alternativa deve ter até 500 caracteres")
        String alternativaC,

        @NotBlank(message = "Preencha a alternativa D")
        @Size(max = 500, message = "Cada alternativa deve ter até 500 caracteres")
        String alternativaD,

        @NotBlank(message = "Escolha a resposta correta")
        String respostaCorreta,

        @Size(max = 1000, message = "A explicação deve ter até 1000 caracteres")
        String explicacao) {}
