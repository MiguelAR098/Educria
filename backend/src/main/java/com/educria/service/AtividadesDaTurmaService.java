package com.educria.service;

import com.educria.domain.entity.Aluno;
import com.educria.domain.entity.PontuacaoColetiva;
import com.educria.dto.AtividadeColetivaResponse;
import com.educria.dto.HistoricoDeAtividades;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.AtividadeColetivaRepository;
import com.educria.repository.PontuacaoColetivaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lado do aluno: o que a turma dele fez junto. Só a turma do próprio aluno é consultada. */
@Service
@RequiredArgsConstructor
public class AtividadesDaTurmaService {

    private static final int TAMANHO_HISTORICO = 20;

    private final AlunoRepository alunoRepository;
    private final PontuacaoColetivaRepository pontuacaoRepository;
    private final AtividadeColetivaRepository atividadeRepository;

    /** O cadastro do aluno autenticado define a turma da consulta. Sem turma, retorna saldo zero e lista vazia. */
    @Transactional(readOnly = true)
    public HistoricoDeAtividades daMinhaTurma(String emailAluno) {
        Aluno aluno = alunoRepository.findByEmail(emailAluno)
                .orElseThrow(() -> new ApiException("Aluno não encontrado", HttpStatus.NOT_FOUND));
        if (aluno.getTurma() == null) {
            return new HistoricoDeAtividades(0, List.of());
        }
        Long turmaId = aluno.getTurma().getId();

        int total = pontuacaoRepository.findByTurmaId(turmaId)
                .map(PontuacaoColetiva::getPontosTotais)
                .orElse(0);
        List<AtividadeColetivaResponse> atividades = atividadeRepository
                .historico(turmaId, PageRequest.of(0, TAMANHO_HISTORICO)).stream()
                .map(AtividadeColetivaService::paraResposta)
                .toList();

        return new HistoricoDeAtividades(total, atividades);
    }
}
