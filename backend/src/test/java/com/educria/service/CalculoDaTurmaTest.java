package com.educria.service;

import com.educria.dto.AlertaDaTurma;
import com.educria.dto.AssuntoSimples;
import com.educria.dto.IndicadoresDoAluno;
import com.educria.dto.PanoramaDaTurma;
import com.educria.dto.RespostaNivel;
import com.educria.service.CalculoDaTurma.AlunoNaTurma;
import com.educria.service.CalculoDoMapa.Registro;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculoDaTurmaTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 23);
    private static final AssuntoSimples FRACOES = new AssuntoSimples(1L, "frações");
    private static final AssuntoSimples PORCENTAGEM = new AssuntoSimples(2L, "porcentagem");
    private static final List<AssuntoSimples> ASSUNTOS = List.of(FRACOES, PORCENTAGEM);

    private static final AlunoNaTurma ANA = new AlunoNaTurma(10L, "Ana Lima");
    private static final AlunoNaTurma BRUNO = new AlunoNaTurma(11L, "Bruno Reis");
    private static final AlunoNaTurma CARLA = new AlunoNaTurma(12L, "Carla Souza");

    private static RespostaNivel resposta(AlunoNaTurma aluno, AssuntoSimples assunto, int anterior, int atual,
                                          boolean correta, int diasAtras) {
        return new RespostaNivel(aluno.id(), assunto.id(), assunto.nome(), anterior, atual, correta,
                HOJE.minusDays(diasAtras).atTime(10, 0));
    }

    private static Registro nivel(AlunoNaTurma aluno, AssuntoSimples assunto, int nivel, int respostas) {
        return new Registro(aluno.id(), assunto.id(), nivel, respostas);
    }

    private static PanoramaDaTurma calcular(List<AlunoNaTurma> alunos, Map<Long, Integer> revisoes,
                                            List<Registro> niveisAtuais, RespostaNivel... respostas) {
        List<RespostaNivel> ordenadas = Stream.of(respostas)
                .sorted(Comparator.comparing(RespostaNivel::respondidoEm)).toList();
        return CalculoDaTurma.calcular(5L, HOJE, alunos, ASSUNTOS, niveisAtuais, ordenadas, revisoes);
    }

    private static IndicadoresDoAluno de(PanoramaDaTurma panorama, AlunoNaTurma aluno) {
        return panorama.alunos().stream().filter(i -> i.alunoId().equals(aluno.id())).findFirst().orElseThrow();
    }

    private static AlertaDaTurma alerta(PanoramaDaTurma panorama, String titulo) {
        return panorama.alertas().stream().filter(a -> a.titulo().equals(titulo)).findFirst().orElse(null);
    }

    @Test
    void evolucaoEhAVariacaoDoNivelMedioNosAssuntosDaMateria() {
        PanoramaDaTurma panorama = calcular(List.of(ANA), Map.of(),
                List.of(nivel(ANA, FRACOES, 5, 3), nivel(ANA, PORCENTAGEM, 2, 1)),
                resposta(ANA, FRACOES, 1, 3, true, 40),
                resposta(ANA, FRACOES, 3, 5, true, 10),
                resposta(ANA, PORCENTAGEM, 1, 2, true, 5));

        // frações 3 -> 5 (+2) e porcentagem 1 -> 2 (+1), dividido pelos 2 assuntos da matéria
        assertEquals(1.5, de(panorama, ANA).evolucao30d());
    }

    @Test
    void alunoSemRespostaNaJanelaNaoTemEvolucao() {
        PanoramaDaTurma panorama = calcular(List.of(ANA), Map.of(), List.of(nivel(ANA, FRACOES, 3, 1)),
                resposta(ANA, FRACOES, 1, 3, true, 60));

        assertNull(de(panorama, ANA).evolucao30d());
    }

    @Test
    void respostasAntigasSemNivelNaoViramEvolucaoZero() {
        RespostaNivel antiga = new RespostaNivel(ANA.id(), FRACOES.id(), FRACOES.nome(), null, null, false,
                HOJE.minusDays(3).atTime(10, 0));

        PanoramaDaTurma panorama = calcular(List.of(ANA), Map.of(), List.of(nivel(ANA, FRACOES, 3, 1)), antiga);

        assertNull(de(panorama, ANA).evolucao30d());
    }

    @Test
    void perdaDeNivelGeraAlerta() {
        PanoramaDaTurma panorama = calcular(List.of(ANA, BRUNO), Map.of(), List.of(nivel(BRUNO, FRACOES, 3, 2)),
                resposta(BRUNO, FRACOES, 1, 5, true, 50),
                resposta(BRUNO, FRACOES, 5, 3, false, 3));

        assertEquals(-1.0, de(panorama, BRUNO).evolucao30d());
        AlertaDaTurma alerta = alerta(panorama, "Perda de nível");
        assertTrue(alerta.texto().startsWith("Bruno perdeu"));
    }

    @Test
    void assuntoSoTravaATurmaCom3AlunosNoNivelBaixo() {
        Registro ana = nivel(ANA, FRACOES, 2, 3);
        Registro bruno = nivel(BRUNO, FRACOES, 1, 2);
        Registro carla = nivel(CARLA, FRACOES, 2, 4);

        assertNull(alerta(calcular(List.of(ANA, BRUNO, CARLA), Map.of(), List.of(ana, bruno)),
                "Assunto travado na turma"));

        AlertaDaTurma alerta = alerta(calcular(List.of(ANA, BRUNO, CARLA), Map.of(), List.of(ana, bruno, carla)),
                "Assunto travado na turma");
        assertTrue(alerta.texto().startsWith("3 alunos"));
        assertTrue(alerta.texto().contains("frações"));
    }

    @Test
    void revisoesAcumuladasGeramAlertaSoAPartirDoLimite() {
        assertNull(alerta(calcular(List.of(ANA), Map.of(10L, 7), List.of()), "Revisões acumuladas"));

        AlertaDaTurma alerta = alerta(calcular(List.of(ANA), Map.of(10L, 8), List.of()), "Revisões acumuladas");
        assertTrue(alerta.texto().startsWith("Ana tem"));
    }

    @Test
    void alunoAtivoEmNivelAltoESemAvancoFicaSemDesafio() {
        PanoramaDaTurma panorama = calcular(List.of(ANA), Map.of(), List.of(nivel(ANA, FRACOES, 9, 5)),
                resposta(ANA, FRACOES, 9, 9, true, 9),
                resposta(ANA, FRACOES, 9, 9, true, 8),
                resposta(ANA, FRACOES, 9, 9, true, 7),
                resposta(ANA, FRACOES, 9, 9, true, 6),
                resposta(ANA, FRACOES, 9, 9, true, 5));

        assertEquals(0.0, de(panorama, ANA).evolucao30d());
        assertTrue(alerta(panorama, "Sem desafio").texto().startsWith("Ana está"));
    }

    @Test
    void erraMaisEmApontaSoAssuntosComDoisOuMaisErros() {
        PanoramaDaTurma panorama = calcular(List.of(ANA), Map.of(), List.of(),
                resposta(ANA, FRACOES, 3, 2, false, 6),
                resposta(ANA, FRACOES, 2, 1, false, 5),
                resposta(ANA, FRACOES, 1, 1, false, 4),
                resposta(ANA, PORCENTAGEM, 1, 1, false, 3));

        assertEquals(List.of("frações"), de(panorama, ANA).erraMaisEm());
    }

    @Test
    void tendenciaTemUmPontoPorSemanaENuloAntesDeHaverPratica() {
        PanoramaDaTurma panorama = calcular(List.of(ANA), Map.of(),
                List.of(nivel(ANA, FRACOES, 5, 3), nivel(ANA, PORCENTAGEM, 2, 1)),
                resposta(ANA, FRACOES, 1, 3, true, 40),
                resposta(ANA, FRACOES, 3, 5, true, 10),
                resposta(ANA, PORCENTAGEM, 1, 2, true, 5));

        var pontos = panorama.tendencia();
        assertEquals(5, pontos.size());
        assertEquals(HOJE.minusDays(28), pontos.get(0).dia());
        assertEquals(List.of(3.0, 3.0, 3.0, 5.0, 3.5), pontos.stream().map(p -> p.nivelMedio()).toList());

        assertNull(calcular(List.of(ANA), Map.of(), List.of()).tendencia().get(0).nivelMedio());
    }

    @Test
    void juntarNomesComVirgulaEE() {
        assertEquals("Ana", CalculoDaTurma.juntar(List.of("Ana")));
        assertEquals("Ana e Bruno", CalculoDaTurma.juntar(List.of("Ana", "Bruno")));
        assertEquals("Ana, Bruno e Carla", CalculoDaTurma.juntar(List.of("Ana", "Bruno", "Carla")));
    }
}
