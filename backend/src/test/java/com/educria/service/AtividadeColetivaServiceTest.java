package com.educria.service;

import com.educria.domain.entity.Professor;
import com.educria.domain.enums.TipoAtividadeColetiva;
import com.educria.dto.LancarAtividadeRequest;
import com.educria.exception.ApiException;
import com.educria.repository.AtividadeColetivaRepository;
import com.educria.repository.PontuacaoColetivaRepository;
import com.educria.repository.ProfessorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AtividadeColetivaServiceTest {
    private static final String EMAIL = "professor@example.test";

    private final ProfessorRepository professores = mock(ProfessorRepository.class);
    private final PontuacaoColetivaRepository pontuacoes = mock(PontuacaoColetivaRepository.class);
    private final AtividadeColetivaRepository atividades = mock(AtividadeColetivaRepository.class);
    private final AtividadeColetivaService servico = new AtividadeColetivaService(professores, pontuacoes, atividades);

    private static LancarAtividadeRequest pedido(int pontos) {
        return new LancarAtividadeRequest(TipoAtividadeColetiva.PROJETO, "Cartaz sobre frações", pontos, null);
    }

    @Test
    void pontosForaDasOpcoesSaoRecusadosAntesDeQualquerConsulta() {
        var erro = assertThrows(ApiException.class, () -> servico.lancar(EMAIL, 1L, pedido(55)));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
        verifyNoInteractions(professores, pontuacoes, atividades);
    }

    @Test
    void professorNaoLancaAtividadeEmTurmaQueNaoLeciona() {
        Professor professor = mock(Professor.class);
        when(professor.turmaPorId(7L)).thenReturn(Optional.empty());
        when(professores.buscarComTurmas(EMAIL)).thenReturn(Optional.of(professor));

        var erro = assertThrows(ApiException.class, () -> servico.lancar(EMAIL, 7L, pedido(40)));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
        verifyNoInteractions(pontuacoes, atividades);
    }
}
