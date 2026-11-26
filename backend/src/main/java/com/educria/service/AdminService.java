package com.educria.service;

import com.educria.controller.AdminController.AlunoResumo;
import com.educria.controller.AdminController.TurmaDetalhe;
import com.educria.controller.AdminController.TurmaResumo;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final TurmaRepository turmaRepository;
    private final PontuacaoColetivaRepository pontuacaoColetivaRepository;
    private final AlunoRepository alunoRepository;

    // ── Turmas ──────────────────────────────────────────────────────────────

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

    /** NOVO — lista todas as turmas com pontuação e contagem de alunos */
    @Transactional(readOnly = true)
    public List<TurmaResumo> listarTurmas() {
        return turmaRepository.findAll().stream()
                .map(t -> new TurmaResumo(
                        t.getId(),
                        t.getNome(),
                        t.getCodigo(),
                        t.getAnoLetivo(),
                        t.getPontuacaoColetiva() != null ? t.getPontuacaoColetiva().getPontosTotais() : 0,
                        alunoRepository.countByTurmaId(t.getId())
                ))
                .toList();
    }

    /** NOVO — detalhe de uma turma com lista de alunos */
    @Transactional(readOnly = true)
    public TurmaDetalhe detalharTurma(Long turmaId) {
        Turma turma = turmaRepository.findByIdWithPontuacao(turmaId)
                .orElseThrow(() -> new ApiException("Turma não encontrada", HttpStatus.NOT_FOUND));

        List<AlunoResumo> alunos = alunoRepository.findByTurmaId(turmaId).stream()
                .map(a -> new AlunoResumo(a.getId(), a.getNome(), a.getMatricula()))
                .toList();

        return new TurmaDetalhe(
                turma.getId(),
                turma.getNome(),
                turma.getCodigo(),
                turma.getAnoLetivo(),
                turma.getPontuacaoColetiva() != null ? turma.getPontuacaoColetiva().getPontosTotais() : 0,
                alunos
        );
    }

    @Transactional
    public void lancarPontosColetivos(Long turmaId, int pontos) {
        PontuacaoColetiva pc = pontuacaoColetivaRepository.findByTurmaId(turmaId)
                .orElseThrow(() -> new ApiException("Pontuação da turma não encontrada", HttpStatus.NOT_FOUND));

        pc.adicionarPontos(pontos);
        pontuacaoColetivaRepository.save(pc);
    }

    // ── Alunos ──────────────────────────────────────────────────────────────

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

    /** NOVO — lista alunos de uma turma */
    @Transactional(readOnly = true)
    public List<AlunoResumo> listarAlunosDaTurma(Long turmaId) {
        turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ApiException("Turma não encontrada", HttpStatus.NOT_FOUND));
        return alunoRepository.findByTurmaId(turmaId).stream()
                .map(a -> new AlunoResumo(a.getId(), a.getNome(), a.getMatricula()))
                .toList();
    }
}
