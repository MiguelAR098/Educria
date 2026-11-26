package com.educria.service;

import com.educria.domain.entity.Aluno;
import com.educria.domain.entity.Professor;
import com.educria.domain.entity.Turma;
import com.educria.dto.AssuntoSimples;
import com.educria.dto.MapaDaTurma;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.AssuntoRepository;
import com.educria.repository.DesempenhoPorAssuntoRepository;
import com.educria.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Reúne os dados do mapa após validar o vínculo do professor com a turma solicitada. */
@Service
@RequiredArgsConstructor
public class MapaDaTurmaService {

    private final ProfessorRepository professorRepository;
    private final AssuntoRepository assuntoRepository;
    private final AlunoRepository alunoRepository;
    private final DesempenhoPorAssuntoRepository desempenhoRepository;

    /** Mapa de uma turma, restrito aos assuntos da matéria do professor. */
    @Transactional(readOnly = true)
    public MapaDaTurma mapa(String email, Long turmaId) {
        Professor professor = professorRepository.buscarComTurmas(email)
                .orElseThrow(() -> new ApiException("Professor não encontrado", HttpStatus.NOT_FOUND));
        Turma turma = professor.turmaPorId(turmaId)
                .orElseThrow(() -> new ApiException("Você não leciona nesta turma", HttpStatus.FORBIDDEN));
        Long materiaId = professor.getMateria().getId();

        List<AssuntoSimples> assuntos = assuntoRepository.daMateria(materiaId).stream()
                .map(a -> new AssuntoSimples(a.getId(), a.getNome()))
                .toList();
        List<CalculoDoMapa.AlunoNoMapa> alunos = alunoRepository.findByTurmaId(turmaId).stream()
                .sorted(Comparator.comparing(Aluno::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(a -> new CalculoDoMapa.AlunoNoMapa(a.getId(), a.getNome()))
                .toList();
        List<CalculoDoMapa.Registro> registros = desempenhoRepository.daTurma(turmaId, materiaId).stream()
                .map(d -> new CalculoDoMapa.Registro(
                        d.getAluno().getId(),
                        d.getAssunto().getId(),
                        d.getNivelAtual(),
                        d.getTotalAcertos() + d.getTotalErros()))
                .toList();

        return CalculoDoMapa.calcular(turma.getId(), turma.getNome(), professor.getMateria().getNome(),
                assuntos, alunos, registros);
    }
}
