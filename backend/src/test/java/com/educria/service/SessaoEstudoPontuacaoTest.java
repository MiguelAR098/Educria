package com.educria.service;

import com.educria.domain.entity.Aluno;
import com.educria.domain.entity.Assunto;
import com.educria.domain.entity.DesempenhoPorAssunto;
import com.educria.domain.entity.Questao;
import com.educria.domain.entity.RespostaAluno;
import com.educria.domain.entity.SessaoEstudo;
import com.educria.dto.QuestaoParaAluno;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.DesempenhoPorAssuntoRepository;
import com.educria.repository.RespostaAlunoRepository;
import com.educria.repository.SessaoEstudoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessaoEstudoPontuacaoTest {
    private final AlunoRepository alunos = mock(AlunoRepository.class);
    private final SessaoEstudoRepository sessoes = mock(SessaoEstudoRepository.class);
    private final RespostaAlunoRepository respostas = mock(RespostaAlunoRepository.class);
    private final DesempenhoPorAssuntoRepository desempenhos = mock(DesempenhoPorAssuntoRepository.class);
    private final AgendaRevisoesService agenda = mock(AgendaRevisoesService.class);
    private final SessaoEstudoService servico = new SessaoEstudoService(
            alunos, null, desempenhos, sessoes, null, respostas, null, agenda, null);
    private final Aluno aluno = Aluno.builder().id(5L).email("aluno@example.test").build();
    private final Assunto assunto = new Assunto(null, "frações");
    private final Questao questao = Questao.builder().id(12L).assunto(assunto).dificuldade(5)
            .respostaCorreta("A").alternativaA("Certa").alternativaB("Errada").build();
    private final SessaoEstudo sessao = new SessaoEstudo(aluno, assunto, 12L, false);
    private final DesempenhoPorAssunto desempenho = new DesempenhoPorAssunto(aluno, assunto);

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(assunto, "id", 8L);
        ReflectionTestUtils.setField(sessao, "id", 7L);
        when(alunos.bloquearPorEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(sessoes.buscarDoAluno(7L, aluno.getEmail())).thenReturn(Optional.of(sessao));
        when(sessoes.questaoDaSessao(7L, 12L)).thenReturn(Optional.of(questao));
        when(desempenhos.findByAlunoIdAndAssuntoId(5L, 8L)).thenReturn(Optional.of(desempenho));
        when(respostas.save(any(RespostaAluno.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @ParameterizedTest(name = "dificuldade {0}, último acerto {1}, pontos {2}")
    @CsvSource(nullValues = "null", value = {
            "1, null, 10", "5, null, 50", "10, null, 100",
            "5, false, 30", "5, true, 15", "null, null, 0"
    })
    void gravaOPremioDoAcertoComHistoricoConsultadoPorAlunoEQuestao(
            Integer dificuldade, Boolean acertoAnterior, int pontos) {
        questao.setDificuldade(dificuldade);
        if (acertoAnterior != null) {
            when(respostas.findFirstByAlunoIdAndQuestaoIdOrderByRespondidoEmDescIdDesc(5L, 12L))
                    .thenReturn(Optional.of(() -> acertoAnterior));
        }

        var retorno = servico.responder(aluno.getEmail(), 7L, 12L, "A", 1);

        var gravada = ArgumentCaptor.forClass(RespostaAluno.class);
        var ordem = inOrder(alunos, respostas, agenda);
        ordem.verify(alunos).bloquearPorEmail(aluno.getEmail());
        ordem.verify(respostas).findBySessaoIdAndPasso(7L, 1);
        ordem.verify(respostas).findFirstByAlunoIdAndQuestaoIdOrderByRespondidoEmDescIdDesc(5L, 12L);
        ordem.verify(respostas).save(gravada.capture());
        ordem.verify(agenda).registrar(aluno, questao, true);
        assertEquals(pontos, gravada.getValue().getPontosIndividuais());
        assertEquals(pontos, retorno.pontosConcedidos());
    }

    @Test
    void erroERecuperacaoGeramDoisRegistrosSemAplicarNovoDiagnostico() {
        var erro = servico.responder(aluno.getEmail(), 7L, 12L, "B", 1);
        assertEquals(0, erro.pontosConcedidos());
        verify(respostas, never()).findFirstByAlunoIdAndQuestaoIdOrderByRespondidoEmDescIdDesc(anyLong(), anyLong());

        sessao.reapresentar(12L);
        when(respostas.findFirstByAlunoIdAndQuestaoIdOrderByRespondidoEmDescIdDesc(5L, 12L))
                .thenReturn(Optional.of(() -> false));
        var recuperacao = servico.responder(aluno.getEmail(), 7L, 12L, "A", 2);

        var tentativas = ArgumentCaptor.forClass(RespostaAluno.class);
        verify(respostas, times(2)).save(tentativas.capture());
        assertEquals(0, tentativas.getAllValues().getFirst().getPontosIndividuais());
        assertEquals(30, tentativas.getAllValues().getLast().getPontosIndividuais());
        assertEquals(30, recuperacao.pontosConcedidos());
        assertEquals(erro.nivelAtual(), recuperacao.nivelAtual());
        assertEquals(0, desempenho.getTotalAcertos());
        assertEquals(1, desempenho.getTotalErros());
        verify(desempenhos).save(desempenho);
        verify(agenda).registrar(aluno, questao, false);
        verify(agenda).registrar(aluno, questao, true);
    }

    @Test
    void reenvioEConsultaDevolvemPontosSalvosEReenvioConflitanteMantem409() {
        var gravada = RespostaAluno.builder().aluno(aluno).questao(questao)
                .passo(1).correta(true).respostaDada("A").pontosIndividuais(30).build();
        when(respostas.findBySessaoIdAndPasso(7L, 1)).thenReturn(Optional.of(gravada));
        when(respostas.findBySessaoIdAndPassoAndAlunoId(7L, 1, 5L)).thenReturn(Optional.of(gravada));
        when(respostas.pontosDaSessao(7L, 5L)).thenReturn(30L);
        when(sessoes.questaoApresentada(12L)).thenReturn(mock(QuestaoParaAluno.class));

        var reenvio = servico.responder(aluno.getEmail(), 7L, 12L, "A", 1);
        var reconsulta = servico.consultar(aluno.getEmail(), 7L);
        var conflito = assertThrows(ApiException.class, () -> servico.responder(aluno.getEmail(), 7L, 12L, "B", 1));

        assertEquals(30, reenvio.pontosConcedidos());
        assertEquals(reenvio, reconsulta.feedback());
        assertEquals(30, reconsulta.pontosSessao());
        assertEquals(HttpStatus.CONFLICT, conflito.getStatus());
        verify(respostas, never()).findFirstByAlunoIdAndQuestaoIdOrderByRespondidoEmDescIdDesc(anyLong(), anyLong());
        verify(respostas, never()).save(any());
        verifyNoInteractions(agenda, desempenhos);
    }

    @Test
    void reenvioDeTentativaLegadaNaoInventaPontos() {
        var legada = RespostaAluno.builder().aluno(aluno).questao(questao)
                .passo(1).correta(true).respostaDada("A").build();
        when(respostas.findBySessaoIdAndPasso(7L, 1)).thenReturn(Optional.of(legada));

        assertNull(servico.responder(aluno.getEmail(), 7L, 12L, "A", 1).pontosConcedidos());
        verify(respostas, never()).save(any());
        verifyNoInteractions(agenda);
    }
}
