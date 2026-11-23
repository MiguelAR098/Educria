package com.educria.service;

import com.educria.dto.AssuntoDaTurma;
import com.educria.dto.AssuntoSimples;
import com.educria.dto.CelulaDoMapa;
import com.educria.dto.LinhaDoMapa;
import com.educria.dto.MapaDaTurma;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Monta o mapa da turma a partir de dados simples, sem acesso ao banco. */
final class CalculoDoMapa {

    /** Nível a partir do qual o assunto conta como "travado" para o aluno. */
    static final int NIVEL_BAIXO = 2;
    static final int LIMITE_FRAGEIS = 4;

    record AlunoNoMapa(Long id, String nome) {}

    record Registro(Long alunoId, Long assuntoId, int nivel, int respostas) {}

    private CalculoDoMapa() {}

    static MapaDaTurma calcular(Long turmaId, String turmaNome, String materia, List<AssuntoSimples> assuntos,
                                List<AlunoNoMapa> alunos, List<Registro> registros) {
        Map<String, Registro> registroDe = new HashMap<>();
        registros.forEach(r -> registroDe.put(r.alunoId() + ":" + r.assuntoId(), r));

        List<LinhaDoMapa> linhas = alunos.stream()
                .map(aluno -> new LinhaDoMapa(aluno.id(), aluno.nome(), assuntos.stream()
                        .map(assunto -> celula(registroDe.get(aluno.id() + ":" + assunto.id())))
                        .toList()))
                .toList();

        List<AssuntoDaTurma> medias = new ArrayList<>();
        for (int i = 0; i < assuntos.size(); i++) {
            int coluna = i;
            List<CelulaDoMapa> praticadas = linhas.stream()
                    .map(linha -> linha.celulas().get(coluna))
                    .filter(CelulaDoMapa::praticou)
                    .toList();
            Double media = praticadas.isEmpty() ? null
                    : Math.round(praticadas.stream().mapToInt(CelulaDoMapa::nivel).average().orElse(0) * 10.0) / 10.0;
            medias.add(new AssuntoDaTurma(
                    assuntos.get(i).id(),
                    assuntos.get(i).nome(),
                    media,
                    (int) praticadas.stream().filter(c -> c.nivel() <= NIVEL_BAIXO).count(),
                    praticadas.size()));
        }

        List<AssuntoDaTurma> maisFrageis = medias.stream()
                .filter(a -> a.nivelMedio() != null)
                .sorted(Comparator.comparing(AssuntoDaTurma::nivelMedio))
                .limit(LIMITE_FRAGEIS)
                .toList();

        return new MapaDaTurma(turmaId, turmaNome, materia, assuntos, linhas, medias, maisFrageis);
    }

    private static CelulaDoMapa celula(Registro registro) {
        return registro == null
                ? new CelulaDoMapa(1, false)
                : new CelulaDoMapa(registro.nivel(), registro.respostas() > 0);
    }
}
