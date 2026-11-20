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
import java.util.Optional;
import java.util.Locale;
import java.util.HashMap;
import java.time.Clock;
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

    private final Clock relogio;
    private final AgendaRevisoesService agenda;
    private final RepeticaoEspacadaRepository repeticoes;

    private static final int QUESTOES_ENTRE_TENTATIVAS = 2;

    private record Escolha(QuestaoParaAluno questao, boolean revisao) {}

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

        var hoje = LocalDate.now(relogio);
        Assunto assunto = assuntoId == null
                ? assuntos.recomendar(aluno.getId(), hoje, PageRequest.of(0, 1)).stream().findFirst()
                    .orElseThrow(SessaoEstudoService::semQuestoes)
                : assuntos.findById(assuntoId)
                    .orElseThrow(() -> new ApiException("Assunto não encontrado", HttpStatus.NOT_FOUND));

        var primeira = selecionar(aluno, assunto.getId(), Set.of(), 0, hoje).orElseThrow(SessaoEstudoService::semQuestoes);
        var sessao = sessoes.save(new SessaoEstudo(aluno, assunto, primeira.questao().id(), primeira.revisao()));
        return new Inicio(new EstadoSessao(sessao.resumo(), primeira.questao(), null, 1, false, 0), true);
    }

    @Transactional(readOnly = true)
    public List<ResumoSessao> listar(String email) {
        return sessoes.listarDoAluno(email);
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public EstadoSessao consultar(String email, Long id) {
        return estado(buscar(email, id));
    }

    @Transactional
    public EstadoSessao mudarSituacao(String email, Long id, SituacaoSessao destino) {
        bloquearAluno(email);
        var sessao = buscar(email, id);
        if (destino == SituacaoSessao.ENCERRADA && !respostas.fila(id).isEmpty()) {
            throw new ApiException("Ainda há questões para reaprender. Você pode pausar a sessão.", HttpStatus.CONFLICT);
        }
        sessao.mudarSituacao(destino);
        return estado(sessao);
    }

    @Transactional
    public EstadoSessao apresentar(String email, Long id, int posicao) {
        Aluno aluno = bloquearAluno(email);
        var sessao = buscar(email, id);
        if (!sessao.precisaApresentar(posicao)) return estado(sessao);
        if (sessao.repetindo() && respostas.findBySessaoIdAndPasso(id, sessao.getPasso()).isEmpty()) {
            throw new ApiException("Responda à tentativa atual antes de avançar", HttpStatus.CONFLICT);
        }
        var fila = respostas.fila(id);
        var pendente = fila.isEmpty() ? null : fila.getFirst();
        boolean intercalou = pendente != null && sessao.getPasso() - pendente.getUltimoPasso() >= QUESTOES_ENTRE_TENTATIVAS;
        if (!intercalou && sessao.usadas().size() < SessaoEstudo.LIMITE) {
            var proxima = selecionar(aluno, sessao.getAssunto().getId(), sessao.usadas(),
                    sessao.getRevisoesApresentadas(), LocalDate.now(relogio));
            if (proxima.isPresent()) {
                var escolha = proxima.get();
                sessao.apresentar(escolha.questao().id(), escolha.revisao());
                return estado(sessao);
            }
        }
        if (pendente == null) throw semQuestoes();
        sessao.reapresentar(pendente.getQuestao());
        return estado(sessao);
    }

    @Transactional
    public FeedbackResposta responder(String email, Long id, Long questaoId, String alternativa, Integer passo) {
        Aluno aluno = bloquearAluno(email);
        SessaoEstudo sessao = buscar(email, id);
        Questao questao = sessoes.questaoDaSessao(id, questaoId)
                .orElseThrow(() -> new ApiException("Questão não apresentada nesta sessão", HttpStatus.NOT_FOUND));

        // Um cliente antigo só pode enviar/repetir a primeira resposta àquela questão.
        var anterior = passo == null ? respostas.findFirstBySessaoIdAndQuestaoIdOrderByPasso(id, questaoId)
                : respostas.findBySessaoIdAndPasso(id, passo);
        if (anterior.isPresent()) {
            var salva = anterior.get();
            if (!salva.getQuestao().getId().equals(questaoId) || !salva.getRespostaDada().equals(alternativa)) {
                throw new ApiException("Esta tentativa já tem outra resposta", HttpStatus.CONFLICT);
            }
            return feedback(salva);
        }
        if ((passo != null && passo != sessao.getPasso()) || sessao.getSituacao() != SituacaoSessao.ATIVA
                || !questaoId.equals(sessao.questaoAtual())) {
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
        Assunto assunto = sessao.getAssunto();
        var desempenho = desempenhos.findByAlunoIdAndAssuntoId(aluno.getId(), assunto.getId())
                .orElseGet(() -> new DesempenhoPorAssunto(aluno, assunto));
        int nivelAnterior = desempenho.getNivelAtual();
        if (!sessao.repetindo()) {
            desempenho.registrarResposta(correta);
            desempenhos.save(desempenho);
        }
        int nivelAtual = desempenho.getNivelAtual();

        var salva = respostas.save(RespostaAluno.builder().passo(sessao.getPasso()).reaprender(!correta).aluno(aluno).sessao(sessao).questao(questao)
                .respostaDada(alternativa).correta(correta)
                .nivelAnterior(nivelAnterior).nivelAtual(nivelAtual).build());
        agenda.registrar(aluno, questao, correta);
        return feedback(salva);
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
                respostas.findBySessaoIdAndPasso(resumo.id(), sessao.getPasso()).map(this::feedback).orElse(null),
                sessao.getPasso(), sessao.repetindo(), respostas.fila(resumo.id()).size());
    }

    private FeedbackResposta feedback(RespostaAluno resposta) {
        var questao = resposta.getQuestao();
        String explicacao = resposta.isReaprender() ? orientacao(questao.getAssunto().getNome()) : questao.getExplicacao();
        return new FeedbackResposta(resposta.getRespostaDada(), resposta.isCorreta(), explicacao,
                resposta.getNivelAnterior(), resposta.getNivelAtual(), resposta.isReaprender());
    }

    private String orientacao(String assunto) {
        return switch (assunto.toLowerCase(Locale.ROOT)) {
            case "somar e subtrair" -> "Separe dezenas e unidades. Confira se a operação deve aumentar ou diminuir o valor inicial.";
            case "multiplicar e dividir" -> "Organize os valores em grupos iguais e confira o cálculo com a operação inversa.";
            case "frações" -> "Identifique o todo e o que cada parte representa. Faça um desenho para conferir.";
            case "porcentagem" -> "Identifique qual quantidade representa 100% antes de calcular a parte pedida.";
            case "proporção" -> "Organize os pares de valores e verifique se variam no mesmo sentido ou em sentidos opostos.";
            case "equações" -> "Faça a mesma operação nos dois lados da igualdade. Teste o valor encontrado.";
            case "área e perímetro" -> "Confira se a pergunta pede a superfície ou o contorno e observe a unidade de medida.";
            case "várias etapas" -> "Separe o problema em etapas e anote cada resultado intermediário.";
            default -> "Identifique o que a pergunta pede e refaça o raciocínio antes de comparar as alternativas.";
        };
    }

    private Optional<Escolha> selecionar(Aluno aluno, Long assuntoId, Set<Long> usadas, int revisoesUsadas, LocalDate hoje) {
        int nivel = desempenhos.findByAlunoIdAndAssuntoId(aluno.getId(), assuntoId)
                .map(DesempenhoPorAssunto::getNivelAtual).orElse(NivelAdaptativo.MINIMO);
        var datas = new HashMap<Long, LocalDate>();
        for (var item : repeticoes.datasPorAssunto(aluno.getId(), assuntoId)) {
            datas.put(item.getQuestaoId(), item.getData());
        }
        var contexto = new SelecaoAdaptativaService.Contexto(usadas, datas, revisoesUsadas, hoje);
        return selecao.selecionar(assuntoId, nivel, contexto)
                .map(questao -> new Escolha(questao, datas.containsKey(questao.id())));
    }

    private static ApiException semQuestoes() {
        return new ApiException("Nenhuma questão elegível disponível", HttpStatus.CONFLICT);
    }
}
