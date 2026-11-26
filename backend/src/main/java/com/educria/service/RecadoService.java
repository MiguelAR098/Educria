package com.educria.service;

import com.educria.domain.entity.Aluno;
import com.educria.domain.entity.Professor;
import com.educria.domain.entity.Recado;
import com.educria.domain.entity.Turma;
import com.educria.dto.EnviarRecadoRequest;
import com.educria.dto.RecadoResponse;
import com.educria.dto.RecadosDoAluno;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.ProfessorRepository;
import com.educria.repository.RecadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecadoService {

    private static final int TAMANHO_LISTA = 50;

    private final ProfessorRepository professorRepository;
    private final AlunoRepository alunoRepository;
    private final RecadoRepository recadoRepository;

    /** Professor envia um recado a um aluno de uma turma em que leciona. */
    @Transactional
    public RecadoResponse enviar(String emailProfessor, Long turmaId, Long alunoId, EnviarRecadoRequest request) {
        Professor professor = buscarProfessor(emailProfessor);
        Aluno aluno = alunoDaTurma(professor, turmaId, alunoId);

        Recado recado = recadoRepository.save(Recado.builder()
                .aluno(aluno)
                .professor(professor)
                .texto(request.texto().trim())
                .build());

        return paraResposta(recado);
    }

    /** Recados já enviados a um aluno da turma, para o professor acompanhar. */
    @Transactional(readOnly = true)
    public List<RecadoResponse> doAlunoParaProfessor(String emailProfessor, Long turmaId, Long alunoId) {
        Professor professor = buscarProfessor(emailProfessor);
        alunoDaTurma(professor, turmaId, alunoId);
        return listar(alunoId);
    }

    /** Recados do próprio aluno logado. */
    @Transactional(readOnly = true)
    public RecadosDoAluno meusRecados(String emailAluno) {
        Aluno aluno = buscarAluno(emailAluno);
        return new RecadosDoAluno(
                (int) recadoRepository.countByAlunoIdAndLidoEmIsNull(aluno.getId()),
                listar(aluno.getId()));
    }

    @Transactional
    public void marcarComoLidos(String emailAluno) {
        Aluno aluno = buscarAluno(emailAluno);
        recadoRepository.marcarTodosComoLidos(aluno.getId(), LocalDateTime.now());
    }

    private List<RecadoResponse> listar(Long alunoId) {
        return recadoRepository.doAluno(alunoId, PageRequest.of(0, TAMANHO_LISTA)).stream()
                .map(RecadoService::paraResposta)
                .toList();
    }

    private Professor buscarProfessor(String email) {
        return professorRepository.buscarComTurmas(email)
                .orElseThrow(() -> new ApiException("Professor não encontrado", HttpStatus.NOT_FOUND));
    }

    private Aluno buscarAluno(String email) {
        return alunoRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Aluno não encontrado", HttpStatus.NOT_FOUND));
    }

    private Aluno alunoDaTurma(Professor professor, Long turmaId, Long alunoId) {
        Turma turma = professor.turmaPorId(turmaId)
                .orElseThrow(() -> new ApiException("Você não leciona nesta turma", HttpStatus.FORBIDDEN));
        return alunoRepository.findByIdAndTurmaId(alunoId, turma.getId())
                .orElseThrow(() -> new ApiException("Aluno não encontrado nesta turma", HttpStatus.NOT_FOUND));
    }

    private static RecadoResponse paraResposta(Recado r) {
        return new RecadoResponse(
                r.getId(),
                r.getTexto(),
                r.getProfessor() != null ? r.getProfessor().getNome() : null,
                r.getCriadoEm(),
                r.getLidoEm() != null);
    }
}
