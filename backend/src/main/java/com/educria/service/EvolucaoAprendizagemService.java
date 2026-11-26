package com.educria.service;

import com.educria.dto.RelatorioEvolucao;
import com.educria.dto.RelatorioEvolucao.Assunto;
import com.educria.dto.RelatorioEvolucao.Dia;
import com.educria.dto.RelatorioEvolucao.Resumo;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.AssuntoRepository;
import com.educria.repository.EvolucaoAprendizagemRepository;
import com.educria.repository.EvolucaoAprendizagemRepository.Linha;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Monta o relatório de 30 dias com contagens diárias, indicadores do período e níveis observados por assunto. */
@Service
@RequiredArgsConstructor
public class EvolucaoAprendizagemService {
    private static final int DIAS = 30;

    private final AlunoRepository alunos;
    private final AssuntoRepository assuntos;
    private final EvolucaoAprendizagemRepository evolucao;
    private final Clock relogio;

    /** Consulta a evolução do titular identificado pelo e-mail autenticado, com filtro opcional por assunto. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public RelatorioEvolucao obter(String email, Long assuntoId) {
        var aluno = alunos.findByEmail(email)
                .orElseThrow(() -> new ApiException("Aluno não encontrado", HttpStatus.NOT_FOUND));
        return montar(aluno.getId(), assuntoId, null);
    }

    /** O chamador autoriza o aluno e determina a matéria a partir do professor. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public RelatorioEvolucao obterPorMateria(Long alunoId, long materiaId) {
        return montar(alunoId, null, materiaId);
    }

    private RelatorioEvolucao montar(Long alunoId, Long assuntoId, Long materiaId) {
        if (assuntoId != null && assuntos.findById(assuntoId).isEmpty()) {
            throw new ApiException("Assunto não encontrado", HttpStatus.NOT_FOUND);
        }

        // O intervalo inclui hoje e os 29 dias anteriores. O começo de amanhã serve de limite exclusivo.
        var hoje = LocalDate.now(relogio);
        var inicio = hoje.minusDays(DIAS - 1);
        var fimExclusivo = hoje.plusDays(1);
        var linhas = evolucao.agregar(alunoId, assuntoId, materiaId, inicio, fimExclusivo, relogio.getZone());
        var resumo = linhas.stream().filter(Linha::geral).findFirst()
                .map(geral -> new Resumo(geral.sessoesIniciadas(), geral.tentativas(), geral.acertos(),
                        geral.tentativasIncorretas(), geral.erros(), geral.questoesUnicas(), geral.tentativasReaprendizagem(), taxa(geral)))
                .orElseGet(() -> new Resumo(0, 0, 0, 0, 0, 0, 0, null));

        var porDia = linhas.stream().filter(linha -> linha.dia() != null)
                .collect(Collectors.toMap(Linha::dia, Function.identity()));
        // Completa os dias sem eventos com contagens zeradas. Os níveis observados continuam opcionais.
        var dias = inicio.datesUntil(fimExclusivo).map(data -> {
            var dia = porDia.get(data);
            return dia == null ? new Dia(data, 0, 0, 0, 0)
                    : new Dia(data, dia.tentativas(), dia.acertos(), dia.tentativasIncorretas(), dia.erros());
        }).toList();

        var porAssunto = linhas.stream().filter(linha -> linha.assuntoId() != null)
                .map(linha -> new Assunto(linha.assuntoId(), linha.nome(), linha.sessoesIniciadas(), linha.tentativas(),
                        linha.acertos(), linha.tentativasIncorretas(), linha.erros(), linha.questoesUnicas(), taxa(linha),
                        linha.nivelInicialObservado(), linha.nivelFinalObservado()))
                .toList();
        return new RelatorioEvolucao(inicio, hoje, resumo, dias, porAssunto);
    }

    private static BigDecimal taxa(Linha linha) {
        if (linha.primeirasTentativas() == 0) return null;
        return BigDecimal.valueOf(linha.primeirasCorretas()).movePointRight(2)
                .divide(BigDecimal.valueOf(linha.primeirasTentativas()), 2, RoundingMode.HALF_UP);
    }
}
