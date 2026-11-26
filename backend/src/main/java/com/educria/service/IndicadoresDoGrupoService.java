package com.educria.service;

import com.educria.dto.IndicadoresDoGrupo;
import com.educria.repository.AssuntoRepository;
import com.educria.repository.IndicadoresDoGrupoRepository;
import com.educria.repository.IndicadoresDoGrupoRepository.AlunoDoRecorte;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compara indicadores agregados dos participantes com os dos demais alunos elegíveis no recorte.
 * A matéria do professor delimita os níveis e as respostas que alimentam a comparação.
 */
@Service
@RequiredArgsConstructor
public class IndicadoresDoGrupoService {
    private static final String ESCOPO = "GRUPOS_ATIVOS_DO_PROFESSOR_NAS_TURMAS_DO_GRUPO";

    private final GrupoPedagogicoService grupos;
    private final IndicadoresDoGrupoRepository indicadores;
    private final AssuntoRepository assuntos;
    private final Clock relogio;

    /**
     * Consulta o contexto autorizado, os vínculos e os dados de aprendizagem em uma transação de leitura.
     * A resposta contém médias, contagens e turmas de origem, sem identificação dos estudantes.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public IndicadoresDoGrupo indicadores(String email, Long id) {
        var contexto = grupos.contextoAutorizado(email, id);
        var janela = CalculoDaTurma.Janela.ate(LocalDate.now(relogio));
        var alunos = indicadores.alunos(id, contexto.professorId());
        Map<Long, CalculoDaTurma.MedidasDeAprendizagem> medidas = new HashMap<>();
        int totalAssuntos = assuntos.daMateria(contexto.materiaId()).size();
        Set<Long> respondeu = Set.of();
        if (!alunos.isEmpty() && totalAssuntos > 0) {
            indicadores.niveis(id, contexto.professorId(), contexto.materiaId(), janela.inicio()).forEach(nivel ->
                    medidas.computeIfAbsent(nivel.alunoId(), chave -> new CalculoDaTurma.MedidasDeAprendizagem())
                            .registrar(nivel.nivelAtual(), nivel.nivelInicial()));
            respondeu = new HashSet<>(indicadores.alunosComRespostaNaJanela(id, contexto.professorId(),
                    contexto.materiaId(), janela.inicio(), janela.fimExclusivo()));
        }

        var participantes = alunos.stream().filter(aluno -> aluno.origemId() != null).toList();
        var naoParticipantes = alunos.stream().filter(aluno -> aluno.origemId() == null).toList();
        var origens = participantes.stream().collect(Collectors.groupingBy(
                aluno -> new Origem(aluno.origemId(), aluno.origemNome()), Collectors.counting()))
                .entrySet().stream()
                .map(entrada -> new IndicadoresDoGrupo.Origem(entrada.getKey().id(), entrada.getKey().nome(), entrada.getValue()))
                .sorted(Comparator.comparing(IndicadoresDoGrupo.Origem::nome, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(IndicadoresDoGrupo.Origem::id))
                .toList();

        return new IndicadoresDoGrupo(id, contexto.nome(), contexto.tipo(),
                new IndicadoresDoGrupo.Materia(contexto.materiaId(), contexto.materiaNome()),
                new IndicadoresDoGrupo.Periodo(CalculoDaTurma.JANELA_DIAS, janela.inicio(), janela.fimExclusivo(),
                        relogio.getZone().getId()), ESCOPO, origens, resumir(participantes, medidas, respondeu, totalAssuntos),
                resumir(naoParticipantes, medidas, respondeu, totalAssuntos));
    }

    /** Cada aluno com a medida disponível tem o mesmo peso na média do recorte. */
    static IndicadoresDoGrupo.Recorte resumir(List<AlunoDoRecorte> alunos,
                                               Map<Long, CalculoDaTurma.MedidasDeAprendizagem> medidas,
                                               Set<Long> respondeu, int totalAssuntos) {
        var nivel = alunos.stream().map(aluno -> medidas.get(aluno.id())).filter(Objects::nonNull)
                .map(CalculoDaTurma.MedidasDeAprendizagem::nivelMedio).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).summaryStatistics();
        var evolucao = alunos.stream().filter(aluno -> medidas.containsKey(aluno.id()))
                .map(aluno -> medidas.get(aluno.id()).evolucao30d(totalAssuntos, respondeu.contains(aluno.id())))
                .filter(Objects::nonNull).mapToDouble(Double::doubleValue).summaryStatistics();
        return new IndicadoresDoGrupo.Recorte(alunos.size(), medida(nivel, alunos.size()), medida(evolucao, alunos.size()));
    }

    private static IndicadoresDoGrupo.Medida medida(DoubleSummaryStatistics valores, int total) {
        int comDados = Math.toIntExact(valores.getCount());
        Double media = comDados == 0 ? null : Math.round(valores.getAverage() * 100.0) / 100.0;
        return new IndicadoresDoGrupo.Medida(media, comDados, total - comDados);
    }

    private record Origem(Long id, String nome) {}
}
