package com.educria.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

public enum TipoGrupoPedagogico {
    GRUPO,
    PROJETO;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static TipoGrupoPedagogico doJson(JsonNode valor) {
        if (!valor.isTextual()) throw new IllegalArgumentException("Esperado tipo GRUPO ou PROJETO");
        return valueOf(valor.textValue());
    }
}
