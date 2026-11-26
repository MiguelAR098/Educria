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

/** Mantém a agenda por questão e consulta revisões pendentes ou futuras no fuso do relógio de estudo. */
@Service
@RequiredArgsConstructor
public class AgendaRevisoesService {
    private final RepeticaoEspacadaRepository repeticoes;
    private final Clock relogio;

    /**
     * Um acerto inicia o agendamento em um dia ou avança no intervalo existente. Um erro retira a questão
     * da agenda espaçada para que a sessão trate a reaprendizagem. O acerto que vier depois da remoção
     * reinicia a agenda em um dia. {@code MANDATORY} exige a transação do chamador, que já bloqueou o
     * aluno e validou a unicidade da tentativa.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Aluno aluno, Questao questao, boolean correta, LocalDate dataResposta) {
        var existente = repeticoes.findByAlunoIdAndQuestaoId(aluno.getId(), questao.getId());
        if (!correta) {
            existente.ifPresent(repeticoes::delete);
            return;
        }
        var agenda = existente
                .orElseGet(() -> RepeticaoEspacada.builder().aluno(aluno).questao(questao).build());
        agenda.registrarAcerto(dataResposta);
        repeticoes.save(agenda);
    }

    /** Uma revisão que vence hoje já está pendente. A próxima virada informa ao cliente quando atualizar a lista. */
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
