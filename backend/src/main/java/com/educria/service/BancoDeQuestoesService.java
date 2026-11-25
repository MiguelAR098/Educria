package com.educria.service;

import com.educria.domain.NivelAdaptativo;
import com.educria.domain.entity.Assunto;
import com.educria.domain.entity.Atividade;
import com.educria.domain.entity.Materia;
import com.educria.domain.entity.Professor;
import com.educria.domain.entity.Questao;
import com.educria.dto.AssuntoSimples;
import com.educria.dto.BancoDeQuestoes;
import com.educria.dto.CoberturaDoBanco;
import com.educria.dto.QuestaoDoProfessor;
import com.educria.dto.QuestaoExportada;
import com.educria.dto.QuestaoRequest;
import com.educria.dto.ResultadoImportacao;
import com.educria.exception.ApiException;
import com.educria.repository.AssuntoRepository;
import com.educria.repository.AtividadeRepository;
import com.educria.repository.BancoDeQuestoesRepository;
import com.educria.repository.ProfessorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Banco de questões da matéria do professor. Questões nunca são apagadas: apagar removeria
 * em cascata as respostas dos alunos. O professor desativa a questão e ela sai do sorteio.
 */
@Service
@RequiredArgsConstructor
public class BancoDeQuestoesService {

    private static final Set<String> LETRAS = Set.of("A", "B", "C", "D");
    static final int LIMITE_IMPORTACAO = 200;
    private static final int LIMITE_MENSAGENS = 20;
    private static final int QUANTIDADE_DIFICULDADES = NivelAdaptativo.MAXIMO - NivelAdaptativo.MINIMO + 1;

    private final ProfessorRepository professorRepository;
    private final AssuntoRepository assuntoRepository;
    private final BancoDeQuestoesRepository bancoRepository;
    private final AtividadeRepository atividadeRepository;

    @Transactional(readOnly = true)
    public BancoDeQuestoes listar(String email, Long assuntoId) {
        Professor professor = buscarProfessor(email);
        Materia materia = professor.getMateria();
        List<Assunto> assuntos = assuntoRepository.daMateria(materia.getId());

        List<Questao> questoes;
        if (assuntoId == null) {
            questoes = bancoRepository.daMateria(materia.getId());
        } else {
            assuntoDaMateria(assuntos, assuntoId);
            questoes = bancoRepository.doAssunto(assuntoId);
        }

        Map<Long, long[]> uso = estatisticas(questoes);
        return new BancoDeQuestoes(
                materia.getNome(),
                assuntos.stream().map(a -> new AssuntoSimples(a.getId(), a.getNome())).toList(),
                questoes.stream().map(q -> paraResposta(q, uso.get(q.getId()))).toList());
    }

    @Transactional(readOnly = true)
    public CoberturaDoBanco cobertura(String email) {
        Long materiaId = buscarProfessor(email).getMateria().getId();
        List<Assunto> assuntos = assuntoRepository.daMateria(materiaId);
        Map<Long, long[]> contagensPorAssunto = new HashMap<>();

        for (var contagem : bancoRepository.contarElegiveis(materiaId)) {
            long[] contagens = contagensPorAssunto.computeIfAbsent(contagem.getAssuntoId(),
                    id -> new long[QUANTIDADE_DIFICULDADES]);
            contagens[contagem.getDificuldade() - NivelAdaptativo.MINIMO] = contagem.getTotal();
        }

        long[] semQuestoes = new long[QUANTIDADE_DIFICULDADES];
        List<CoberturaDoBanco.Assunto> cobertura = assuntos.stream().map(assunto -> {
            long[] contagens = contagensPorAssunto.getOrDefault(assunto.getId(), semQuestoes);
            return new CoberturaDoBanco.Assunto(assunto.getId(), assunto.getNome(),
                    Arrays.stream(contagens).boxed().toList(), Arrays.stream(contagens).sum());
        }).toList();
        long total = cobertura.stream().mapToLong(CoberturaDoBanco.Assunto::total).sum();
        long vazias = cobertura.stream().flatMap(assunto -> assunto.contagens().stream())
                .filter(contagem -> contagem == 0).count();
        return new CoberturaDoBanco(cobertura, total, vazias);
    }

    @Transactional
    public QuestaoDoProfessor criar(String email, QuestaoRequest request) {
        validar(request);
        Professor professor = buscarProfessor(email);
        Assunto assunto = assuntoDaMateria(assuntoRepository.daMateria(professor.getMateria().getId()), request.assuntoId());

        Questao questao = Questao.builder()
                .atividade(atividadeDoBanco(professor.getMateria()))
                .autor(professor)
                .ativa(true)
                .build();
        preencher(questao, assunto, request);

        return paraResposta(bancoRepository.save(questao), null);
    }

    @Transactional
    public QuestaoDoProfessor atualizar(String email, Long questaoId, QuestaoRequest request) {
        validar(request);
        Professor professor = buscarProfessor(email);
        Questao questao = questaoDaMateria(professor, questaoId);
        Assunto assunto = assuntoDaMateria(assuntoRepository.daMateria(professor.getMateria().getId()), request.assuntoId());

        preencher(questao, assunto, request);
        return paraResposta(questao, estatisticas(List.of(questao)).get(questaoId));
    }

    @Transactional
    public QuestaoDoProfessor definirAtiva(String email, Long questaoId, boolean ativa) {
        Professor professor = buscarProfessor(email);
        Questao questao = questaoDaMateria(professor, questaoId);

        questao.setAtiva(ativa);
        return paraResposta(questao, estatisticas(List.of(questao)).get(questaoId));
    }

    /** Questões ativas da matéria no formato do JSON de importação (serve como cópia de segurança). */
    @Transactional(readOnly = true)
    public List<QuestaoExportada> exportar(String email) {
        Professor professor = buscarProfessor(email);
        return bancoRepository.daMateria(professor.getMateria().getId()).stream()
                .filter(q -> q.isAtiva() && q.getDificuldade() != null)
                .map(q -> new QuestaoExportada(
                        q.getAssunto().getNome(),
                        q.getDificuldade(),
                        q.getEnunciado(),
                        List.of(q.getAlternativaA(), q.getAlternativaB(), q.getAlternativaC(), q.getAlternativaD()),
                        "ABCD".indexOf(q.getRespostaCorreta()),
                        q.getExplicacao()))
                .toList();
    }

    /**
     * Cadastra várias questões de uma vez. Cada item é avaliado separadamente: os válidos entram,
     * os repetidos (mesmo enunciado já existente na matéria) são ignorados e os inválidos são
     * recusados com o motivo.
     */
    @Transactional
    public ResultadoImportacao importar(String email, JsonNode corpo) {
        if (corpo == null || !corpo.isArray()) {
            throw new ApiException("O JSON precisa ser uma lista entre colchetes", HttpStatus.BAD_REQUEST);
        }
        if (corpo.isEmpty()) {
            throw new ApiException("A lista está vazia", HttpStatus.BAD_REQUEST);
        }
        if (corpo.size() > LIMITE_IMPORTACAO) {
            throw new ApiException("Envie no máximo " + LIMITE_IMPORTACAO + " questões por vez", HttpStatus.BAD_REQUEST);
        }

        Professor professor = buscarProfessor(email);
        Materia materia = professor.getMateria();

        Map<String, Assunto> assuntos = new HashMap<>();
        assuntoRepository.daMateria(materia.getId())
                .forEach(a -> assuntos.put(LeitorDeQuestoes.normalizar(a.getNome()), a));
        Map<String, Long> idPorNome = new HashMap<>();
        Map<Long, Assunto> porId = new HashMap<>();
        assuntos.forEach((nome, assunto) -> {
            idPorNome.put(nome, assunto.getId());
            porId.put(assunto.getId(), assunto);
        });

        Set<String> existentes = new HashSet<>();
        bancoRepository.daMateria(materia.getId())
                .forEach(q -> existentes.add(LeitorDeQuestoes.normalizar(q.getEnunciado())));

        List<Questao> novas = new ArrayList<>();
        List<String> erros = new ArrayList<>();
        int repetidas = 0;
        int recusadas = 0;
        Atividade atividade = null;

        for (int i = 0; i < corpo.size(); i++) {
            try {
                QuestaoRequest pedido = LeitorDeQuestoes.ler(corpo.get(i), idPorNome);
                validar(pedido);
                if (!existentes.add(LeitorDeQuestoes.normalizar(pedido.enunciado()))) {
                    repetidas++;
                    continue;
                }
                if (atividade == null) {
                    atividade = atividadeDoBanco(materia);
                }
                Questao questao = Questao.builder().atividade(atividade).autor(professor).ativa(true).build();
                preencher(questao, porId.get(pedido.assuntoId()), pedido);
                novas.add(questao);
            } catch (IllegalArgumentException | ApiException erro) {
                recusadas++;
                if (erros.size() < LIMITE_MENSAGENS) {
                    erros.add("Item " + (i + 1) + ": " + erro.getMessage());
                }
            }
        }

        bancoRepository.saveAll(novas);
        return new ResultadoImportacao(novas.size(), repetidas, recusadas, erros);
    }

    /** Regras que a validação por anotação não cobre. As alternativas já chegam não vazias. */
    static void validar(QuestaoRequest request) {
        String letra = request.respostaCorreta().trim().toUpperCase();
        if (!LETRAS.contains(letra)) {
            throw new ApiException("A resposta correta deve ser A, B, C ou D", HttpStatus.BAD_REQUEST);
        }
        Set<String> distintas = new HashSet<>(Stream.of(
                        request.alternativaA(), request.alternativaB(), request.alternativaC(), request.alternativaD())
                .map(a -> a.trim().toLowerCase())
                .toList());
        if (distintas.size() < 4) {
            throw new ApiException("As alternativas devem ser diferentes entre si", HttpStatus.BAD_REQUEST);
        }
    }

    private static void preencher(Questao questao, Assunto assunto, QuestaoRequest r) {
        questao.setAssunto(assunto);
        questao.setDificuldade(r.dificuldade());
        questao.setEnunciado(r.enunciado().trim());
        questao.setAlternativaA(r.alternativaA().trim());
        questao.setAlternativaB(r.alternativaB().trim());
        questao.setAlternativaC(r.alternativaC().trim());
        questao.setAlternativaD(r.alternativaD().trim());
        questao.setRespostaCorreta(r.respostaCorreta().trim().toUpperCase());
        questao.setExplicacao(r.explicacao() == null || r.explicacao().isBlank() ? null : r.explicacao().trim());
    }

    private Professor buscarProfessor(String email) {
        return professorRepository.buscarComTurmas(email)
                .orElseThrow(() -> new ApiException("Professor não encontrado", HttpStatus.NOT_FOUND));
    }

    private static Assunto assuntoDaMateria(List<Assunto> assuntosDaMateria, Long assuntoId) {
        return assuntosDaMateria.stream()
                .filter(a -> a.getId().equals(assuntoId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Este assunto não pertence à sua matéria", HttpStatus.FORBIDDEN));
    }

    private Questao questaoDaMateria(Professor professor, Long questaoId) {
        Questao questao = bancoRepository.buscar(questaoId)
                .orElseThrow(() -> new ApiException("Questão não encontrada", HttpStatus.NOT_FOUND));
        if (!questao.getAssunto().getMateria().getId().equals(professor.getMateria().getId())) {
            throw new ApiException("Esta questão não é da sua matéria", HttpStatus.FORBIDDEN);
        }
        return questao;
    }

    /** Container exigido pelo modelo: uma atividade por matéria agrupa as questões dos professores. */
    private Atividade atividadeDoBanco(Materia materia) {
        String titulo = "Banco de questões: " + materia.getNome();
        return atividadeRepository.findFirstByTituloOrderByIdAsc(titulo)
                .orElseGet(() -> atividadeRepository.save(Atividade.builder()
                        .titulo(titulo)
                        .enunciado("Questões cadastradas por professores da matéria.")
                        .build()));
    }

    /** Para cada questão: {total de respostas, total de acertos}. */
    private Map<Long, long[]> estatisticas(List<Questao> questoes) {
        Map<Long, long[]> uso = new HashMap<>();
        if (questoes.isEmpty()) {
            return uso;
        }
        for (Object[] linha : bancoRepository.estatisticas(questoes.stream().map(Questao::getId).toList())) {
            long total = ((Number) linha[1]).longValue();
            long acertos = linha[2] == null ? 0 : ((Number) linha[2]).longValue();
            uso.put(((Number) linha[0]).longValue(), new long[]{total, acertos});
        }
        return uso;
    }

    private static QuestaoDoProfessor paraResposta(Questao q, long[] uso) {
        long total = uso != null ? uso[0] : 0;
        Double taxa = total > 0 ? Math.round(uso[1] * 1000.0 / total) / 10.0 : null;

        return new QuestaoDoProfessor(
                q.getId(),
                q.getAssunto().getId(),
                q.getAssunto().getNome(),
                q.getDificuldade(),
                q.getEnunciado(),
                q.getAlternativaA(),
                q.getAlternativaB(),
                q.getAlternativaC(),
                q.getAlternativaD(),
                q.getRespostaCorreta(),
                q.getExplicacao(),
                q.isAtiva(),
                q.getAutor() != null ? q.getAutor().getNome() : null,
                total,
                taxa);
    }
}
