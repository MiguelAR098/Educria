package com.educria.service;

import com.educria.domain.entity.Professor;
import com.educria.domain.entity.Turma;
import com.educria.dto.EnviarRecadoRequest;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.ProfessorRepository;
import com.educria.repository.RecadoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RecadoServiceTest {
    private static final String EMAIL = "professor@example.test";

    private final ProfessorRepository professores = mock(ProfessorRepository.class);
    private final AlunoRepository alunos = mock(AlunoRepository.class);
    private final RecadoRepository recados = mock(RecadoRepository.class);
    private final RecadoService servico = new RecadoService(professores, alunos, recados);
    private final Professor professor = mock(Professor.class);

    private static EnviarRecadoRequest pedido() {
        return new EnviarRecadoRequest("Passa na minha mesa na quarta.");
    }

    @Test
    void professorNaoEnviaRecadoEmTurmaQueNaoLeciona() {
        when(professor.turmaPorId(7L)).thenReturn(Optional.empty());
        when(professores.buscarComTurmas(EMAIL)).thenReturn(Optional.of(professor));

        var erro = assertThrows(ApiException.class, () -> servico.enviar(EMAIL, 7L, 3L, pedido()));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
        verifyNoInteractions(alunos, recados);
    }

    @Test
    void alunoDeOutraTurmaNaoRecebeRecado() {
        Turma turma = mock(Turma.class);
        when(turma.getId()).thenReturn(7L);
        when(professor.turmaPorId(7L)).thenReturn(Optional.of(turma));
        when(professores.buscarComTurmas(EMAIL)).thenReturn(Optional.of(professor));
        when(alunos.findByIdAndTurmaId(3L, 7L)).thenReturn(Optional.empty());

        var erro = assertThrows(ApiException.class, () -> servico.enviar(EMAIL, 7L, 3L, pedido()));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatus());
        verifyNoInteractions(recados);
    }
}
