package com.educria.service;

import com.educria.domain.entity.GrupoPedagogico;
import com.educria.domain.entity.Professor;
import com.educria.domain.entity.Turma;
import com.educria.domain.enums.TipoGrupoPedagogico;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GrupoPedagogicoTest {
    @Test
    void arquivamentoMantemPrimeiroInstanteEContexto() {
        Instant criado = Instant.parse("2026-03-12T14:00:00Z");
        Instant arquivado = criado.plusSeconds(60);
        Professor professor = Professor.builder().id(3L).build();
        Turma turma = Turma.builder().id(4L).build();
        var grupo = new GrupoPedagogico(professor, "Oficina", null, TipoGrupoPedagogico.PROJETO, Set.of(turma), criado);

        assertNull(grupo.getArquivadoEm());
        grupo.atualizar("Oficina de Matemática", "Resolução de problemas em equipe");
        grupo.arquivar(arquivado);
        grupo.arquivar(arquivado.plusSeconds(60));

        assertEquals(arquivado, grupo.getArquivadoEm());
        assertEquals("Oficina de Matemática", grupo.getNome());
        assertEquals(Set.of(turma), grupo.getTurmas());
        assertEquals(professor, grupo.getProfessor());
        assertEquals(TipoGrupoPedagogico.PROJETO, grupo.getTipo());
    }
}
