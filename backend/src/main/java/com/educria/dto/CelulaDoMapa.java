package com.educria.dto;

/** praticou é falso se o aluno ainda não respondeu nada do assunto (o nível 1 é só o ponto de partida). */
public record CelulaDoMapa(int nivel, boolean praticou) {}
