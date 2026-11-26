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

/**
 * Mantém turmas, vínculos de alunos e o saldo coletivo nas operações do perfil administrativo.
 * O Spring Security aplica a permissão de perfil na entrada HTTP.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final TurmaRepository turmaRepository;
    private final PontuacaoColetivaRepository pontuacaoColetivaRepository;
    private final AlunoRepository alunoRepository;

    /**
     * Cria a turma com pontuação inicial de zero. A transação permite que turma e saldo
     * sejam gravados juntos ou revertidos em caso de falha.
     *
     * @param nome nome de exibição da turma
     * @param codigo código usado para identificar a turma nas consultas
     * @param anoLetivo ano do cadastro, distinto da série escolar
     * @return turma persistida com a pontuação vinculada
     */
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

    /** Soma pontos diretamente ao saldo da turma, sem registrar uma atividade coletiva. */
    @Transactional
    public void lancarPontosColetivos(Long turmaId, int pontos) {
        PontuacaoColetiva pc = pontuacaoColetivaRepository.bloquearPorTurma(turmaId)
                .orElseThrow(() -> new ApiException("Pontuação da turma não encontrada", HttpStatus.NOT_FOUND));

        pc.adicionarPontos(pontos);
        pontuacaoColetivaRepository.save(pc);
    }

    /** Define a turma atual do aluno, preservando seu cadastro e histórico de aprendizagem. */
    @Transactional
    public void vincularAlunoTurma(Long turmaId, Long alunoId) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ApiException("Turma não encontrada", HttpStatus.NOT_FOUND));
        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new ApiException("Aluno não encontrado", HttpStatus.NOT_FOUND));

        aluno.setTurma(turma);
        alunoRepository.save(aluno);
    }

    /** Remove o vínculo somente quando o aluno pertence à turma informada. */
    @Transactional
    public void desvincularAlunoTurma(Long turmaId, Long alunoId) {
        if (alunoRepository.desvincularDaTurma(alunoId, turmaId) == 0) {
            throw new ApiException("Vínculo do aluno com a turma não encontrado", HttpStatus.NOT_FOUND);
        }
    }
}
