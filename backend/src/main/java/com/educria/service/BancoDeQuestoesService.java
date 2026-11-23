package com.educria.service;

import com.educria.domain.entity.Assunto;
import com.educria.domain.entity.Atividade;
import com.educria.domain.entity.Materia;
import com.educria.domain.entity.Professor;
import com.educria.domain.entity.Questao;
import com.educria.dto.AssuntoSimples;
import com.educria.dto.BancoDeQuestoes;
import com.educria.dto.QuestaoDoProfessor;
import com.educria.dto.QuestaoRequest;
import com.educria.exception.ApiException;
import com.educria.repository.AssuntoRepository;
import com.educria.repository.AtividadeRepository;
import com.educria.repository.BancoDeQuestoesRepository;
import com.educria.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
