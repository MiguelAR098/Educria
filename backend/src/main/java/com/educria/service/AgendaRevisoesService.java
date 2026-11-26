package com.educria.service;

import com.educria.domain.entity.Aluno;
import com.educria.domain.entity.Questao;
import com.educria.domain.entity.RepeticaoEspacada;
import com.educria.dto.AgendaRevisoes;
import com.educria.dto.RevisaoDoAluno;
import com.educria.repository.RepeticaoEspacadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AgendaRevisoesService {
    private final RepeticaoEspacadaRepository repeticoes;
    private final Clock relogio;

    // O chamador já bloqueou o aluno e validou a unicidade da resposta.
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Aluno aluno, Questao questao, boolean correta) {
        var existente = repeticoes.findByAlunoIdAndQuestaoId(aluno.getId(), questao.getId());
        if (!correta) {
            existente.ifPresent(repeticoes::delete);
            return;
        }
        var agenda = existente
                .orElseGet(() -> RepeticaoEspacada.builder().aluno(aluno).questao(questao).build());
        agenda.registrarAcerto(LocalDate.now(relogio));
        repeticoes.save(agenda);
    }

    @Transactional(readOnly = true)
    public AgendaRevisoes listar(Long alunoId) {
        var hoje = LocalDate.now(relogio);
        var pendentes = new ArrayList<RevisaoDoAluno>();
        var futuras = new ArrayList<RevisaoDoAluno>();
        for (var revisao : repeticoes.agenda(alunoId)) {
            if (revisao.proximaRevisao().isAfter(hoje)) futuras.add(revisao);
            else pendentes.add(revisao);
        }
        var virada = hoje.plusDays(1).atStartOfDay(relogio.getZone()).toInstant();
        return new AgendaRevisoes(hoje, virada, pendentes, futuras);
    }
}
