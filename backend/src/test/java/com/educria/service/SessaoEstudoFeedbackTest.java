package com.educria.service;

import com.educria.domain.entity.Aluno;
import com.educria.domain.entity.Questao;
import com.educria.domain.entity.RespostaAluno;
import com.educria.domain.entity.SessaoEstudo;
import com.educria.dto.FeedbackResposta.TipoOrientacao;
import com.educria.repository.AlunoRepository;
import com.educria.repository.RespostaAlunoRepository;
import com.educria.repository.SessaoEstudoRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessaoEstudoFeedbackTest {
    @Test
    void reenviaFeedbackDoPassoOriginalSemGravarOuAgendarOutraTentativa() {
        var alunos = mock(AlunoRepository.class);
        var sessoes = mock(SessaoEstudoRepository.class);
        var respostas = mock(RespostaAlunoRepository.class);
        var agenda = mock(AgendaRevisoesService.class);
        var servico = new SessaoEstudoService(alunos, null, null, sessoes, null, respostas, null, agenda, null);
        var aluno = Aluno.builder().id(5L).email("aluno@example.test").build();
        var questao = Questao.builder().id(12L).respostaCorreta("A").alternativaA("Resposta correta").build();
        var resposta = RespostaAluno.builder().aluno(aluno).questao(questao).passo(1).respostaDada("B").reaprender(true).build();
        when(alunos.bloquearPorEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(sessoes.buscarDoAluno(3L, aluno.getEmail())).thenReturn(Optional.of(mock(SessaoEstudo.class)));
        when(sessoes.questaoDaSessao(3L, 12L)).thenReturn(Optional.of(questao));
        when(respostas.findBySessaoIdAndPasso(3L, 1)).thenReturn(Optional.of(resposta));
        when(respostas.contarErrosAtePasso(5L, 3L, 12L, 1)).thenReturn(1L);

        var feedback = servico.responder(aluno.getEmail(), 3L, 12L, "B", 1);

        assertEquals(TipoOrientacao.DICA_INICIAL, feedback.tipoOrientacao());
        assertNull(feedback.alternativaCorreta());
        verify(respostas).contarErrosAtePasso(5L, 3L, 12L, 1);
        verify(respostas, never()).save(any());
        verifyNoInteractions(agenda);
    }
}
