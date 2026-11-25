package com.educria.service;

import com.educria.domain.entity.Aluno;
import com.educria.domain.entity.DesempenhoAdaptativo;
import com.educria.domain.entity.PontuacaoColetiva;
import com.educria.domain.entity.Turma;
import com.educria.dto.AssuntoDoAluno;
import com.educria.dto.DesempenhoResponse;
import com.educria.dto.MateriaResumo;
import com.educria.dto.PontuacaoColetivaResponse;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.MateriaRepository;
import com.educria.repository.DesempenhoAdaptativoRepository;
import com.educria.repository.PontuacaoColetivaRepository;
import com.educria.repository.RespostaAlunoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final DesempenhoAdaptativoRepository desempenhoRepository;
    private final PontuacaoColetivaRepository pontuacaoColetivaRepository;

    private final MateriaRepository materiaRepository;
    private final RespostaAlunoRepository respostas;

    @Transactional(readOnly = true)
    public List<MateriaResumo> listarMaterias(Long alunoId) {
        return materiaRepository.listarCatalogo(alunoId);
    }

    @Transactional(readOnly = true)
    public List<AssuntoDoAluno> listarAssuntos(Long materiaId, Long alunoId) {
        if (!materiaRepository.existsById(materiaId)) {
            throw new ApiException("Matéria não encontrada", HttpStatus.NOT_FOUND);
        }
        return materiaRepository.listarAssuntos(materiaId, alunoId);
    }

    @Transactional(readOnly = true)
    public DesempenhoResponse getMeuDesempenho(Long alunoId) {
        DesempenhoAdaptativo d = desempenhoRepository.findByAlunoId(alunoId)
                .orElseThrow(() -> new ApiException("Desempenho não encontrado", HttpStatus.NOT_FOUND));

        return DesempenhoResponse.builder()
                .nivelAtual(d.getNivelAtual())
                .errosConsecutivos(d.getErrosConsecutivos())
                .totalAcertos(d.getTotalAcertos())
                .totalErros(d.getTotalErros())
                .nivelMaximo(d.isNivelMaximo())
                .pontosIndividuais(respostas.pontosDoAluno(alunoId))
                .build();
    }

    public PontuacaoColetivaResponse getPontuacaoDaMinhaTurma(Long alunoId) {
        Aluno aluno = alunoRepository.findByIdWithTurma(alunoId)
                .orElseThrow(() -> new ApiException("Aluno não encontrado", HttpStatus.NOT_FOUND));

        Turma turma = aluno.getTurma();
        if (turma == null) {
            return PontuacaoColetivaResponse.builder()
                    .turmaId(null)
                    .nomeTurma("Sem turma vinculada")
                    .pontosTotais(0)
                    .build();
        }

        PontuacaoColetiva pc = pontuacaoColetivaRepository.findByTurmaId(turma.getId())
                .orElseGet(() -> {
                    PontuacaoColetiva nova = PontuacaoColetiva.builder()
                            .turma(turma)
                            .pontosTotais(0)
                            .build();
                    return pontuacaoColetivaRepository.save(nova);
                });

        return PontuacaoColetivaResponse.builder()
                .turmaId(turma.getId())
                .nomeTurma(turma.getNome())
                .pontosTotais(pc.getPontosTotais())
                .build();
    }

    @Transactional
    public void registrarRespostaEAtualizarDesempenho(Long alunoId, boolean acertou) {
        DesempenhoAdaptativo desempenho = desempenhoRepository.findByAlunoId(alunoId)
                .orElseThrow(() -> new ApiException("Desempenho não encontrado", HttpStatus.NOT_FOUND));

        desempenho.registrarResposta(acertou);
        desempenhoRepository.save(desempenho);
    }
}
