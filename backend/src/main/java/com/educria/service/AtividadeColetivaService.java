package com.educria.service;

import com.educria.domain.entity.AtividadeColetiva;
import com.educria.domain.entity.PontuacaoColetiva;
import com.educria.domain.entity.Professor;
import com.educria.domain.entity.Turma;
import com.educria.dto.AtividadeColetivaResponse;
import com.educria.dto.HistoricoDeAtividades;
import com.educria.dto.LancarAtividadeRequest;
import com.educria.exception.ApiException;
import com.educria.repository.AtividadeColetivaRepository;
import com.educria.repository.PontuacaoColetivaRepository;
import com.educria.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** Coordena o registro e a consulta das atividades nas turmas em que o professor leciona. */
@Service
@RequiredArgsConstructor
public class AtividadeColetivaService {

    static final Set<Integer> PONTOS_VALIDOS = Set.of(20, 40, 60, 80);
    private static final int TAMANHO_HISTORICO = 50;

    private final ProfessorRepository professorRepository;
    private final PontuacaoColetivaRepository pontuacaoRepository;
    private final AtividadeColetivaRepository atividadeRepository;

    /**
     * Persiste a atividade e soma seus pontos ao saldo da turma na mesma transação.
     * O bloqueio da pontuação existente serializa lançamentos concorrentes para a mesma turma.
     */
    @Transactional
    public AtividadeColetivaResponse lancar(String email, Long turmaId, LancarAtividadeRequest request) {
        if (!PONTOS_VALIDOS.contains(request.pontos())) {
            throw new ApiException("Os pontos devem ser 20, 40, 60 ou 80", HttpStatus.BAD_REQUEST);
        }
        Professor professor = buscarProfessor(email);
        Turma turma = turmaDoProfessor(professor, turmaId);

        PontuacaoColetiva pontuacao = pontuacaoRepository.bloquearPorTurma(turmaId)
                .orElseGet(() -> pontuacaoRepository.save(PontuacaoColetiva.builder().turma(turma).build()));
        pontuacao.adicionarPontos(request.pontos());
        pontuacaoRepository.save(pontuacao);

        AtividadeColetiva atividade = atividadeRepository.save(AtividadeColetiva.builder()
                .turma(turma)
                .professor(professor)
                .tipo(request.tipo())
                .titulo(request.titulo().trim())
                .pontos(request.pontos())
                .resumo(request.resumo() == null || request.resumo().isBlank() ? null : request.resumo().trim())
                .build());

        return paraResposta(atividade);
    }

    /**
     * Retorna o saldo acumulado e as até 50 atividades mais recentes da turma autorizada.
     * Lançamentos administrativos podem alterar o saldo sem aparecer nessa lista.
     */
    @Transactional(readOnly = true)
    public HistoricoDeAtividades historico(String email, Long turmaId) {
        Professor professor = buscarProfessor(email);
        turmaDoProfessor(professor, turmaId);

        int total = pontuacaoRepository.findByTurmaId(turmaId)
                .map(PontuacaoColetiva::getPontosTotais)
                .orElse(0);
        List<AtividadeColetivaResponse> atividades = atividadeRepository
                .historico(turmaId, PageRequest.of(0, TAMANHO_HISTORICO)).stream()
                .map(AtividadeColetivaService::paraResposta)
                .toList();

        return new HistoricoDeAtividades(total, atividades);
    }

    private Professor buscarProfessor(String email) {
        return professorRepository.buscarComTurmas(email)
                .orElseThrow(() -> new ApiException("Professor não encontrado", HttpStatus.NOT_FOUND));
    }

    private static Turma turmaDoProfessor(Professor professor, Long turmaId) {
        return professor.turmaPorId(turmaId)
                .orElseThrow(() -> new ApiException("Você não leciona nesta turma", HttpStatus.FORBIDDEN));
    }

    static AtividadeColetivaResponse paraResposta(AtividadeColetiva a) {
        return new AtividadeColetivaResponse(
                a.getId(),
                a.getTipo(),
                a.getTitulo(),
                a.getPontos(),
                a.getResumo(),
                a.getProfessor() != null ? a.getProfessor().getNome() : null,
                a.getCriadoEm());
    }
}
