package com.educria.service;

import com.educria.domain.entity.Aluno;
import com.educria.domain.entity.Professor;
import com.educria.dto.AssuntoSimples;
import com.educria.dto.PanoramaDaTurma;
import com.educria.dto.RankingsDaTurma;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.AssuntoRepository;
import com.educria.repository.DesempenhoPorAssuntoRepository;
import com.educria.repository.PanoramaRepository;
import com.educria.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PanoramaDaTurmaService {

    private final ProfessorRepository professorRepository;
    private final AssuntoRepository assuntoRepository;
    private final AlunoRepository alunoRepository;
    private final PanoramaRepository panoramaRepository;
    private final DesempenhoPorAssuntoRepository desempenhoRepository;
    private final Clock relogio;

    /** Alertas, indicadores por aluno e tendência de uma turma, nos assuntos da matéria do professor. */
    @Transactional(readOnly = true)
    public PanoramaDaTurma panorama(String email, Long turmaId) {
        Long materiaId = professorDaTurma(email, turmaId).getMateria().getId();
        LocalDate hoje = LocalDate.now(relogio);

        Map<Long, Integer> vencidas = new HashMap<>();
        for (Object[] linha : panoramaRepository.revisoesVencidas(turmaId, materiaId, hoje)) {
            vencidas.put(((Number) linha[0]).longValue(), ((Number) linha[1]).intValue());
        }
        return calcular(turmaId, materiaId, hoje, alunosDaTurma(turmaId), vencidas);
    }

    /** Classifica os alunos da turma autorizada por pontuação individual e por evolução na matéria. */
    @Transactional(readOnly = true)
    public RankingsDaTurma rankings(String email, Long turmaId) {
        var materia = professorDaTurma(email, turmaId).getMateria();
        var alunos = alunosDaTurma(turmaId);
        var referencia = new RankingsDaTurma.Materia(materia.getId(), materia.getNome());
        if (alunos.isEmpty()) {
            return new RankingsDaTurma(turmaId, referencia, CalculoDaTurma.JANELA_DIAS, List.of(), List.of());
        }

        Map<Long, Long> pontos = new HashMap<>();
        panoramaRepository.pontosDaTurma(turmaId, materia.getId())
                .forEach(linha -> pontos.put(linha.getAlunoId(), linha.getPontos()));
        Map<Long, Double> evolucoes = new HashMap<>();
        calcular(turmaId, materia.getId(), LocalDate.now(relogio), alunos, Map.of()).alunos()
                .forEach(indicador -> evolucoes.put(indicador.alunoId(), indicador.evolucao30d()));

        var rankingPontuacao = ClassificacaoDaTurma.ordenar(alunos, pontos).stream()
                .map(item -> new RankingsDaTurma.Pontuacao(item.posicao(), item.aluno().id(), item.aluno().nome(), item.valor()))
                .toList();
        var rankingEvolucao = ClassificacaoDaTurma.ordenar(alunos, evolucoes).stream()
                .map(item -> new RankingsDaTurma.Evolucao(item.posicao(), item.aluno().id(), item.aluno().nome(), item.valor()))
                .toList();
        return new RankingsDaTurma(turmaId, referencia, CalculoDaTurma.JANELA_DIAS, rankingPontuacao, rankingEvolucao);
    }

    private Professor professorDaTurma(String email, Long turmaId) {
        Professor professor = professorRepository.buscarComTurmas(email)
                .orElseThrow(() -> new ApiException("Professor não encontrado", HttpStatus.NOT_FOUND));
        professor.turmaPorId(turmaId)
                .orElseThrow(() -> new ApiException("Você não leciona nesta turma", HttpStatus.FORBIDDEN));
        return professor;
    }

    private List<CalculoDaTurma.AlunoNaTurma> alunosDaTurma(Long turmaId) {
        return alunoRepository.findByTurmaId(turmaId).stream()
                .sorted(Comparator.comparing(Aluno::getNome, String.CASE_INSENSITIVE_ORDER).thenComparing(Aluno::getId))
                .map(a -> new CalculoDaTurma.AlunoNaTurma(a.getId(), a.getNome()))
                .toList();
    }

    private PanoramaDaTurma calcular(Long turmaId, Long materiaId, LocalDate hoje,
                                    List<CalculoDaTurma.AlunoNaTurma> alunos, Map<Long, Integer> vencidas) {
        List<AssuntoSimples> assuntos = assuntoRepository.daMateria(materiaId).stream()
                .map(a -> new AssuntoSimples(a.getId(), a.getNome()))
                .toList();
        List<CalculoDoMapa.Registro> niveisAtuais = desempenhoRepository.daTurma(turmaId, materiaId).stream()
                .map(d -> new CalculoDoMapa.Registro(
                        d.getAluno().getId(),
                        d.getAssunto().getId(),
                        d.getNivelAtual(),
                        d.getTotalAcertos() + d.getTotalErros()))
                .toList();

        return CalculoDaTurma.calcular(turmaId, hoje, alunos, assuntos, niveisAtuais,
                panoramaRepository.respostasDaTurma(turmaId, materiaId), vencidas);
    }
}
