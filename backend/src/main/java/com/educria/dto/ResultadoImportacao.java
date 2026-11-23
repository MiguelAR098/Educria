package com.educria.dto;

import java.util.List;

/** erros traz até 20 mensagens, no formato "Item 3: motivo". */
public record ResultadoImportacao(int importadas, int repetidas, int recusadas, List<String> erros) {}
