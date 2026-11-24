package com.educria.service;

import com.educria.domain.entity.Aluno;
import com.educria.domain.entity.PontuacaoColetiva;
import com.educria.domain.entity.Turma;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.PontuacaoColetivaRepository;
import com.educria.repository.TurmaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final TurmaRepository turmaRepository;
    private final PontuacaoColetivaRepository pontuacaoColetivaRepository;
    private final AlunoRepository alunoRepository;

    @Transactional
    public Turma criarTurma(String nome, String codigo, Integer anoLetivo) {
        Turma turma = Turma.builder()
                .nome(nome)
                .codigo(codigo)
                .anoLetivo(anoLetivo)
                .build();

        PontuacaoColetiva pontuacao = PontuacaoColetiva.builder()
                .pontosTotais(0)
                .build();

        turma.vincularPontuacao(pontuacao);
        return turmaRepository.save(turma);
    }

    @Transactional
    public void lancarPontosColetivos(Long turmaId, int pontos) {
        PontuacaoColetiva pc = pontuacaoColetivaRepository.findByTurmaId(turmaId)
                .orElseThrow(() -> new ApiException("Pontuação da turma não encontrada", HttpStatus.NOT_FOUND));

        pc.adicionarPontos(pontos);
        pontuacaoColetivaRepository.save(pc);
    }

    @Transactional
    public void vincularAlunoTurma(Long turmaId, Long alunoId) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ApiException("Turma não encontrada", HttpStatus.NOT_FOUND));
        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new ApiException("Aluno não encontrado", HttpStatus.NOT_FOUND));

        aluno.setTurma(turma);
        alunoRepository.save(aluno);
    }

    @Transactional
    public void desvincularAlunoTurma(Long turmaId, Long alunoId) {
        if (alunoRepository.desvincularDaTurma(alunoId, turmaId) == 0) {
            throw new ApiException("Vínculo do aluno com a turma não encontrado", HttpStatus.NOT_FOUND);
        }
    }
}
