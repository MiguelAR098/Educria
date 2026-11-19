package com.educria.service;

import com.educria.domain.NivelAdaptativo;
import com.educria.domain.entity.*;
import com.educria.domain.enums.SituacaoSessao;
import com.educria.dto.*;
import com.educria.exception.ApiException;
import com.educria.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SessaoEstudoService {
    private final AlunoRepository alunos;
    private final AssuntoRepository assuntos;
    private final DesempenhoPorAssuntoRepository desempenhos;
    private final SessaoEstudoRepository sessoes;
    private final SelecaoAdaptativaService selecao;

    private final RespostaAlunoRepository respostas;

    public record Inicio(EstadoSessao estado, boolean criada) {}

    @Transactional
    public Inicio iniciar(String email, Long assuntoId) {
        Aluno aluno = bloquearAluno(email);
        var aberta = sessoes.findByAlunoIdAndSituacaoNot(aluno.getId(), SituacaoSessao.ENCERRADA);
        if (aberta.isPresent()) {
            SessaoEstudo sessao = aberta.get();
            if (assuntoId != null && !assuntoId.equals(sessao.getAssunto().getId())) {
                throw new ApiException("Encerre a sessão aberta antes de mudar de assunto", HttpStatus.CONFLICT);
            }
            return new Inicio(estado(sessao), false);
        }

        Assunto assunto = assuntoId == null
                ? assuntos.recomendar(aluno.getId(), PageRequest.of(0, 1)).stream().findFirst()
                    .orElseThrow(SessaoEstudoService::semQuestoes)
                : assuntos.findById(assuntoId)
                    .orElseThrow(() -> new ApiException("Assunto não encontrado", HttpStatus.NOT_FOUND));

        var primeira = selecionar(aluno, assunto.getId(), Set.of());
        var sessao = sessoes.save(new SessaoEstudo(aluno, assunto, primeira.id()));
        return new Inicio(new EstadoSessao(sessao.resumo(), primeira), true);
    }

    @Transactional(readOnly = true)
    public List<ResumoSessao> listar(String email) {
        return sessoes.listarDoAluno(email);
    }

    @Transactional(readOnly = true)
    public EstadoSessao consultar(String email, Long id) {
        return estado(buscar(email, id));
    }

    @Transactional
    public EstadoSessao mudarSituacao(String email, Long id, SituacaoSessao destino) {
        bloquearAluno(email);
        var sessao = buscar(email, id);
        sessao.mudarSituacao(destino);
        return estado(sessao);
    }

    @Transactional
    public EstadoSessao apresentar(String email, Long id, int posicao) {
        Aluno aluno = bloquearAluno(email);
        var sessao = buscar(email, id);
        if (!sessao.precisaApresentar(posicao)) return estado(sessao);
        var proxima = selecionar(aluno, sessao.getAssunto().getId(), sessao.usadas());
        sessao.apresentar(proxima.id());
        return new EstadoSessao(sessao.resumo(), proxima);
    }

    @Transactional
    public FeedbackResposta responder(String email, Long id, Long questaoId, String alternativa) {
        Aluno aluno = bloquearAluno(email);
        SessaoEstudo sessao = buscar(email, id);
        Questao questao = sessoes.questaoDaSessao(id, questaoId)
                .orElseThrow(() -> new ApiException("Questão não apresentada nesta sessão", HttpStatus.NOT_FOUND));

        var anterior = respostas.feedback(id, questaoId);
        if (anterior.isPresent()) {
            var feedback = anterior.get();
            if (!feedback.alternativa().equals(alternativa)) {
                throw new ApiException("Esta questão já foi respondida com outra alternativa", HttpStatus.CONFLICT);
            }
            return feedback;
        }
        if (sessao.getSituacao() != SituacaoSessao.ATIVA || !questaoId.equals(sessao.questaoAtual())) {
            throw new ApiException("Responda à questão atual de uma sessão ativa", HttpStatus.CONFLICT);
        }
        String texto = switch (alternativa) {
            case "A" -> questao.getAlternativaA();
            case "B" -> questao.getAlternativaB();
            case "C" -> questao.getAlternativaC();
            case "D" -> questao.getAlternativaD();
            default -> null;
        };
        if (texto == null || texto.isBlank()) {
            throw new ApiException("Alternativa indisponível", HttpStatus.BAD_REQUEST);
        }
        boolean correta = questao.isRespostaCorreta(alternativa);
        respostas.save(RespostaAluno.builder().aluno(aluno).sessao(sessao).questao(questao)
                .respostaDada(alternativa).correta(correta).build());
        return new FeedbackResposta(alternativa, correta, questao.getExplicacao());
    }

    private Aluno bloquearAluno(String email) {
        // Todas as escritas do titular usam o mesmo bloqueio, inclusive quando ainda não há sessão.
        return alunos.bloquearPorEmail(email)
                .orElseThrow(() -> new ApiException("Aluno não encontrado", HttpStatus.NOT_FOUND));
    }

    private SessaoEstudo buscar(String email, Long id) {
        return sessoes.buscarDoAluno(id, email)
                .orElseThrow(() -> new ApiException("Sessão não encontrada", HttpStatus.NOT_FOUND));
    }

    private EstadoSessao estado(SessaoEstudo sessao) {
        var resumo = sessao.resumo();
        Long questaoId = sessao.questaoAtual();
        return new EstadoSessao(resumo, sessoes.questaoApresentada(questaoId),
                respostas.feedback(resumo.id(), questaoId).orElse(null));
    }

    private QuestaoParaAluno selecionar(Aluno aluno, Long assuntoId, Set<Long> usadas) {
        int nivel = desempenhos.findByAlunoIdAndAssuntoId(aluno.getId(), assuntoId)
                .map(DesempenhoPorAssunto::getNivelAtual).orElse(NivelAdaptativo.MINIMO);
        // O modelo atual agenda revisões por atividade, sem calendário individual por questão.
        var contexto = new SelecaoAdaptativaService.Contexto(usadas, Map.of(), 0, LocalDate.now());
        return selecao.selecionar(assuntoId, nivel, contexto).orElseThrow(SessaoEstudoService::semQuestoes);
    }

    private static ApiException semQuestoes() {
        return new ApiException("Nenhuma questão elegível disponível", HttpStatus.CONFLICT);
    }
}
