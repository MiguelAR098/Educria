package com.educria.service;

import com.educria.dto.AlertaDaTurma;
import com.educria.dto.AssuntoSimples;
import com.educria.dto.IndicadoresDoAluno;
import com.educria.dto.PanoramaDaTurma;
import com.educria.dto.PontoDaTendencia;
import com.educria.dto.RespostaNivel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Alertas, indicadores por aluno e tendência da turma, calculados a partir do histórico de respostas.
 * Não acessa o banco: recebe os dados prontos. Os limites abaixo são ajustáveis.
 */
final class CalculoDaTurma {

    static final int JANELA_DIAS = 30;
    /** Variação do nível médio (em níveis) a partir da qual se fala em perda ou em ausência de avanço. */
    static final double LIMITE_VARIACAO = 0.25;
    /** Nível médio a partir do qual o aluno conta como em nível alto. */
    static final int NIVEL_ALTO = 8;
    /** Alunos no nível baixo, no mesmo assunto, para o assunto contar como travado na turma. */
    static final int MINIMO_TRAVADOS = 3;
    static final int REVISOES_ACUMULADAS = 8;
    /** Respostas na janela para considerar que o aluno está ativo (e não apenas parado). */
    static final int MINIMO_RESPOSTAS_ATIVO = 5;
    static final int MINIMO_ERROS_PARA_APONTAR = 2;
    /** Dias, em relação a hoje, em que a tendência é medida: uma vez por semana. */
    static final int[] MARCOS_DA_TENDENCIA = {-28, -21, -14, -7, 0};

    record AlunoNaTurma(Long id, String nome) {}

    private CalculoDaTurma() {}

    /**
     * niveisAtuais é o nível de hoje de cada aluno em cada assunto (o mesmo dado do mapa da turma);
     * respostas traz o histórico, em ordem cronológica, e serve para a evolução e a tendência.
     */
    static PanoramaDaTurma calcular(Long turmaId, LocalDate hoje, List<AlunoNaTurma> alunos,
                                    List<AssuntoSimples> assuntos, List<CalculoDoMapa.Registro> niveisAtuais,
                                    List<RespostaNivel> respostas, Map<Long, Integer> revisoesVencidas) {
        LocalDateTime fimDeHoje = hoje.plusDays(1).atStartOfDay();
        LocalDateTime inicioDaJanela = hoje.minusDays(JANELA_DIAS).atStartOfDay();

        Map<String, CalculoDoMapa.Registro> atuais = new HashMap<>();
        niveisAtuais.forEach(r -> atuais.put(r.alunoId() + ":" + r.assuntoId(), r));

        // aluno -> assunto -> respostas em ordem cronológica
        Map<Long, Map<Long, List<RespostaNivel>>> celulas = new HashMap<>();
        for (RespostaNivel r : respostas) {
            celulas.computeIfAbsent(r.alunoId(), k -> new HashMap<>())
                    .computeIfAbsent(r.assuntoId(), k -> new ArrayList<>())
                    .add(r);
        }

        List<IndicadoresDoAluno> indicadores = new ArrayList<>();
        List<String> perderam = new ArrayList<>();
        List<String> semDesafio = new ArrayList<>();
        List<String> acumulando = new ArrayList<>();
        Map<Long, Integer> travados = new HashMap<>();

        for (AlunoNaTurma aluno : alunos) {
            Map<Long, List<RespostaNivel>> doAluno = celulas.getOrDefault(aluno.id(), Map.of());

            int naJanela = 0;
            double somaDasVariacoes = 0;
            double somaDosNiveis = 0;
            int assuntosPraticados = 0;
            int assuntosMedidos = 0;
            Map<String, Integer> erros = new HashMap<>();

            for (AssuntoSimples assunto : assuntos) {
                List<RespostaNivel> cel = doAluno.getOrDefault(assunto.id(), List.of());

                CalculoDoMapa.Registro atual = atuais.get(aluno.id() + ":" + assunto.id());
                Integer nivelFinal = atual != null && atual.respostas() > 0 ? atual.nivel() : null;
                if (nivelFinal != null) {
                    assuntosPraticados++;
                    somaDosNiveis += nivelFinal;
                    if (nivelFinal <= CalculoDoMapa.NIVEL_BAIXO) {
                        travados.merge(assunto.id(), 1, Integer::sum);
                    }
                }

                List<RespostaNivel> daJanela = cel.stream()
                        .filter(r -> !r.respondidoEm().isBefore(inicioDaJanela) && r.respondidoEm().isBefore(fimDeHoje))
                        .toList();
                naJanela += daJanela.size();
                daJanela.stream().filter(r -> !r.correta()).forEach(r -> erros.merge(assunto.nome(), 1, Integer::sum));

                Integer nivelInicial = nivelInicial(cel, inicioDaJanela);
                if (nivelFinal != null && nivelInicial != null) {
                    somaDasVariacoes += nivelFinal - nivelInicial;
                    assuntosMedidos++;
                }
            }

            Double evolucao = naJanela == 0 || assuntosMedidos == 0 ? null
                    : Math.round(somaDasVariacoes / assuntos.size() * 100.0) / 100.0;
            int vencidas = revisoesVencidas.getOrDefault(aluno.id(), 0);
            Double nivelMedio = assuntosPraticados == 0 ? null : somaDosNiveis / assuntosPraticados;

            String nome = primeiroNome(aluno.nome());
            if (evolucao != null && evolucao <= -LIMITE_VARIACAO) {
                perderam.add(nome);
            }
            if (vencidas >= REVISOES_ACUMULADAS) {
                acumulando.add(nome);
            }
            if (evolucao != null && Math.abs(evolucao) < LIMITE_VARIACAO && naJanela >= MINIMO_RESPOSTAS_ATIVO
                    && nivelMedio != null && nivelMedio >= NIVEL_ALTO) {
                semDesafio.add(nome);
            }

            indicadores.add(new IndicadoresDoAluno(aluno.id(), evolucao, vencidas, maisErrados(erros)));
        }

        return new PanoramaDaTurma(turmaId,
                alertas(perderam, travados, assuntos, acumulando, semDesafio),
                tendencia(hoje, assuntos, celulas, niveisAtuais),
                indicadores);
    }

    private static List<AlertaDaTurma> alertas(List<String> perderam, Map<Long, Integer> travados,
                                               List<AssuntoSimples> assuntos, List<String> acumulando,
                                               List<String> semDesafio) {
        List<AlertaDaTurma> alertas = new ArrayList<>();

        if (!perderam.isEmpty()) {
            alertas.add(new AlertaDaTurma("Perda de nível",
                    juntar(perderam) + (perderam.size() > 1 ? " perderam" : " perdeu")
                            + " nível médio nos últimos 30 dias. Vale uma conversa individual antes de qualquer atividade nova."));
        }

        assuntos.stream()
                .filter(a -> travados.getOrDefault(a.id(), 0) >= MINIMO_TRAVADOS)
                .sorted(Comparator.comparing((AssuntoSimples a) -> travados.get(a.id())).reversed())
                .limit(2)
                .forEach(a -> alertas.add(new AlertaDaTurma("Assunto travado na turma",
                        travados.get(a.id()) + " alunos estão no nível " + CalculoDoMapa.NIVEL_BAIXO
                                + " ou abaixo em " + a.nome()
                                + ". É conteúdo para retomar com a turma inteira, não para resolver aluno por aluno.")));

        if (!acumulando.isEmpty()) {
            alertas.add(new AlertaDaTurma("Revisões acumuladas",
                    juntar(acumulando) + (acumulando.size() > 1 ? " têm" : " tem")
                            + " " + REVISOES_ACUMULADAS + " ou mais revisões vencidas. Costuma ser sinal de que a pessoa"
                            + " parou de entrar, não de que não sabe."));
        }

        if (!semDesafio.isEmpty()) {
            alertas.add(new AlertaDaTurma("Sem desafio",
                    juntar(semDesafio) + (semDesafio.size() > 1 ? " estão" : " está")
                            + " em nível alto e sem avanço. Falta questão difícil no banco ou proposta fora do sistema."));
        }
        return alertas;
    }

    private static List<PontoDaTendencia> tendencia(LocalDate hoje, List<AssuntoSimples> assuntos,
                                                    Map<Long, Map<Long, List<RespostaNivel>>> celulas,
                                                    List<CalculoDoMapa.Registro> niveisAtuais) {
        List<PontoDaTendencia> pontos = new ArrayList<>();
        for (int marco : MARCOS_DA_TENDENCIA) {
            LocalDate dia = hoje.plusDays(marco);
            LocalDateTime limite = dia.plusDays(1).atStartOfDay();
            double soma = 0;
            int contadas = 0;
            if (marco == 0) {
                // Hoje vale o nível atual de cada assunto, igual ao mapa da turma.
                for (CalculoDoMapa.Registro registro : niveisAtuais) {
                    if (registro.respostas() > 0) {
                        soma += registro.nivel();
                        contadas++;
                    }
                }
            } else {
                for (Map<Long, List<RespostaNivel>> doAluno : celulas.values()) {
                    for (AssuntoSimples assunto : assuntos) {
                        Integer nivel = nivelAte(doAluno.getOrDefault(assunto.id(), List.of()), limite);
                        if (nivel != null) {
                            soma += nivel;
                            contadas++;
                        }
                    }
                }
            }
            pontos.add(new PontoDaTendencia(dia, contadas == 0 ? null : Math.round(soma / contadas * 10.0) / 10.0));
        }
        return pontos;
    }

    /** Nível do assunto ao fim do período (exclusivo); nulo se o aluno ainda não tinha respondido até então. */
    private static Integer nivelAte(List<RespostaNivel> cel, LocalDateTime limiteExclusivo) {
        Integer nivel = null;
        for (RespostaNivel r : cel) {
            if (!r.respondidoEm().isBefore(limiteExclusivo)) {
                break;
            }
            if (r.nivelAtual() != null) {
                nivel = r.nivelAtual();
            }
        }
        return nivel;
    }

    /**
     * Nível no começo da janela: o nível depois da última resposta anterior a ela ou, se o aluno só começou
     * dentro da janela, o nível em que estava antes da primeira resposta.
     */
    private static Integer nivelInicial(List<RespostaNivel> cel, LocalDateTime inicioDaJanela) {
        Integer antes = nivelAte(cel, inicioDaJanela);
        if (antes != null) {
            return antes;
        }
        Predicate<RespostaNivel> comNivel = r -> r.nivelAnterior() != null;
        Optional<RespostaNivel> primeira = cel.stream().filter(comNivel).findFirst();
        return primeira.map(RespostaNivel::nivelAnterior).orElse(null);
    }

    private static List<String> maisErrados(Map<String, Integer> erros) {
        return erros.entrySet().stream()
                .filter(e -> e.getValue() >= MINIMO_ERROS_PARA_APONTAR)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(2)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static String primeiroNome(String nome) {
        String[] partes = nome.trim().split("\\s+");
        return partes.length == 0 ? nome : partes[0];
    }

    /** "Ana", "Ana e Bruno", "Ana, Bruno e Carla". */
    static String juntar(List<String> nomes) {
        if (nomes.size() <= 1) {
            return String.join("", nomes);
        }
        return String.join(", ", nomes.subList(0, nomes.size() - 1)) + " e " + nomes.get(nomes.size() - 1);
    }
}
