package com.educria.service;

import com.educria.domain.NivelAdaptativo;
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
import com.educria.repository.DesempenhoPorAssuntoRepository;
import com.educria.repository.PontuacaoColetivaRepository;
import com.educria.repository.RespostaAlunoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Fornece o catálogo, o desempenho privado do aluno e a pontuação coletiva da turma à área do estudante. */
@Service
@RequiredArgsConstructor
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final DesempenhoAdaptativoRepository desempenhoRepository;
    private final DesempenhoPorAssuntoRepository desempenhoPorAssunto;
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

    /** Combina o diagnóstico de assuntos praticados e a soma dos pontos já gravados nas tentativas do titular. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DesempenhoResponse getMeuDesempenho(Long alunoId) {
        var resumo = ResumoDoDesempenho.dosAssuntos(desempenhoPorAssunto.doAluno(alunoId));
        long pontos = respostas.pontosDoAluno(alunoId);
        if (resumo.nivelMedio() != null) {
            return DesempenhoResponse.builder()
                    .nivelAtual(resumo.nivelAtual())
                    .errosConsecutivos(resumo.errosConsecutivos())
                    .totalAcertos(resumo.acertos())
                    .totalErros(resumo.erros())
                    .nivelMaximo(resumo.nivelAtual() == NivelAdaptativo.MAXIMO)
                    .pontosIndividuais(pontos)
                    .build();
        }

        // Preserva a visão privada de alunos legados sem diagnóstico por assunto praticado.
        DesempenhoAdaptativo d = desempenhoRepository.findByAlunoId(alunoId)
                .orElseThrow(() -> new ApiException("Desempenho não encontrado", HttpStatus.NOT_FOUND));

        return DesempenhoResponse.builder()
                .nivelAtual(d.getNivelAtual())
                .errosConsecutivos(d.getErrosConsecutivos())
                .totalAcertos(d.getTotalAcertos())
                .totalErros(d.getTotalErros())
                .nivelMaximo(d.isNivelMaximo())
                .pontosIndividuais(pontos)
                .build();
    }

    /** Consulta a pontuação oficial da turma atual do aluno, preservando o resumo padrão quando não há vínculo. */
    @Transactional(readOnly = true)
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

        int pontos = pontuacaoColetivaRepository.findByTurmaId(turma.getId())
                .map(PontuacaoColetiva::getPontosTotais)
                .orElse(0);

        return PontuacaoColetivaResponse.builder()
                .turmaId(turma.getId())
                .nomeTurma(turma.getNome())
                .pontosTotais(pontos)
                .build();
    }
}
