package com.educria.service;

import com.educria.domain.entity.Professor;
import com.educria.domain.entity.Turma;
import com.educria.dto.PerfilProfessor;
import com.educria.dto.TurmaDoProfessor;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class ProfessorService {

    private final ProfessorRepository professorRepository;
    private final AlunoRepository alunoRepository;

    @Transactional(readOnly = true)
    public PerfilProfessor perfil(String email) {
        Professor professor = professorRepository.buscarComTurmas(email)
                .orElseThrow(() -> new ApiException("Professor não encontrado", HttpStatus.NOT_FOUND));

        return new PerfilProfessor(
                professor.getId(),
                professor.getNome(),
                professor.getEmail(),
                professor.getMateria().getNome(),
                professor.getTurmas().stream()
                        .sorted(Comparator.comparing(Turma::getNome))
                        .map(t -> new TurmaDoProfessor(
                                t.getId(),
                                t.getNome(),
                                t.getCodigo(),
                                t.getAnoLetivo(),
                                alunoRepository.countByTurmaId(t.getId())))
                        .toList());
    }
}
