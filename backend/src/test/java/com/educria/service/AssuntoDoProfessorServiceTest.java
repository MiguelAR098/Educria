package com.educria.service;

import com.educria.domain.entity.Assunto;
import com.educria.domain.entity.Materia;
import com.educria.domain.entity.Professor;
import com.educria.repository.AssuntoRepository;
import com.educria.repository.CadastroDeAssuntoRepository;
import com.educria.repository.ProfessorRepository;
import com.educria.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AssuntoDoProfessorServiceTest {
    private static final String EMAIL = "professor@example.test";

    private final ProfessorRepository professores = mock(ProfessorRepository.class);
    private final AssuntoRepository assuntos = mock(AssuntoRepository.class);
    private final CadastroDeAssuntoRepository cadastro = mock(CadastroDeAssuntoRepository.class);
    private final AssuntoDoProfessorService servico = new AssuntoDoProfessorService(professores, assuntos, cadastro);

    private Materia materia;

    private void professorDe(String materiaNome, String... assuntosExistentes) {
        materia = mock(Materia.class);
        when(materia.getId()).thenReturn(4L);
        Professor professor = mock(Professor.class);
        when(professor.getMateria()).thenReturn(materia);
        when(professores.buscarComTurmas(EMAIL)).thenReturn(Optional.of(professor));
        List<Assunto> existentes = java.util.stream.Stream.of(assuntosExistentes)
                .map(nome -> new Assunto(materia, nome)).toList();
        when(assuntos.daMateria(4L)).thenReturn(existentes);
    }

    @Test
    void recusaNomeQueSoMudaAcentoOuCaixa() {
        professorDe("Português", "Interpretação de texto");

        var erro = assertThrows(ApiException.class, () -> servico.criar(EMAIL, "  interpretacao   DE texto "));

        assertEquals(HttpStatus.CONFLICT, erro.getStatus());
        verify(cadastro, never()).save(any());
    }

    @Test
    void cadastraAssuntoNovoComEspacosArrumados() {
        professorDe("Português", "Interpretação de texto");
        when(cadastro.save(any(Assunto.class))).thenAnswer(chamada -> chamada.getArgument(0));

        var criado = servico.criar(EMAIL, "  Classes   de palavras ");

        assertEquals("Classes de palavras", criado.nome());
        verify(cadastro).save(any(Assunto.class));
    }
}
