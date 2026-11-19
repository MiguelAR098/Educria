package com.educria.service;

import com.educria.dto.QuestaoParaAluno;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SelecaoAdaptativaServiceTest {

    private static final long ASSUNTO = 12L;
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 19);
    private static final SelecaoAdaptativaService.Contexto INICIAL = contexto(Set.of(), Map.of(), 0);
    private List<QuestaoParaAluno> disponiveis = List.of();
    private final SelecaoAdaptativaService servico = new SelecaoAdaptativaService(assunto -> {
        assertEquals(ASSUNTO, assunto);
        return disponiveis;
    });

    private static SelecaoAdaptativaService.Contexto contexto(Set<Long> usadas, Map<Long, LocalDate> revisoes, int quantidade) {
        return new SelecaoAdaptativaService.Contexto(usadas, revisoes, quantidade, HOJE);
    }

    private static QuestaoParaAluno questao(long id, int dificuldade) {
        return new QuestaoParaAluno(id, ASSUNTO, dificuldade, "Quanto é 7 + 8?", "13", "15", "16", "17");
    }

    private void candidatas(QuestaoParaAluno... questoes) {
        disponiveis = List.of(questoes);
    }

    @Test
    void assuntoVazioRetornaVazioMesmoComRevisaoDeOutroAssunto() {
        candidatas();
        var pedido = contexto(Set.of(), Map.of(99L, HOJE), 0);
        assertTrue(servico.selecionar(ASSUNTO, 5, pedido).isEmpty());
    }

    @Test
    void assuntoEsgotadoNaoReapresentaUsadasNemAntecipaRevisoes() {
        candidatas(questao(1, 5), questao(2, 6));
        var pedido = contexto(Set.of(1L), Map.of(1L, HOJE, 2L, HOJE.plusDays(1)), 0);
        assertTrue(servico.selecionar(ASSUNTO, 5, pedido).isEmpty());
    }

    @Test
    void priorizaRevisaoElegivelHojeMesmoComQuestaoNovaNoNivelExato() {
        candidatas(questao(1, 5), questao(2, 2), questao(3, 5));
        var pedido = contexto(Set.of(), Map.of(2L, HOJE, 3L, HOJE.plusDays(1)), 2);
        assertEquals(2L, servico.selecionar(ASSUNTO, 5, pedido).orElseThrow().id());
    }

    @Test
    void revisaoAtrasadaJaUsadaCedeAVezParaQuestaoNova() {
        candidatas(questao(1, 4), questao(2, 4));
        var pedido = contexto(Set.of(1L), Map.of(1L, HOJE.minusDays(2)), 0);
        assertEquals(2L, servico.selecionar(ASSUNTO, 4, pedido).orElseThrow().id());
    }

    @Test
    void depoisDeTresRevisoesUsaApenasQuestoesNovas() {
        candidatas(questao(1, 4), questao(2, 9));
        var pedido = contexto(Set.of(), Map.of(1L, HOJE.minusDays(1)), 3);
        assertEquals(2L, servico.selecionar(ASSUNTO, 4, pedido).orElseThrow().id());
        var esgotado = contexto(Set.of(2L), pedido.proximasRevisoes(), 3);
        assertTrue(servico.selecionar(ASSUNTO, 4, esgotado).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10})
    void respeitaOsDoisExtremosDaEscala(int nivel) {
        candidatas(questao(10, 10), questao(5, 5), questao(1, 1));
        assertEquals(nivel, servico.selecionar(ASSUNTO, nivel, INICIAL).orElseThrow().dificuldade());
        candidatas(questao(2, 2), questao(4, 4), questao(5, 5));
        assertEquals(nivel == 1 ? 2 : 5, servico.selecionar(ASSUNTO, nivel, INICIAL).orElseThrow().dificuldade());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11})
    void rejeitaNivelInvalidoAntesDeConsultarBanco(int nivel) {
        var semConsulta = new SelecaoAdaptativaService(id -> fail("Consulta com nível inválido"));
        assertThrows(IllegalArgumentException.class, () -> semConsulta.selecionar(ASSUNTO, nivel, INICIAL));
    }

    @Test
    void empateFavoreceDificuldadeMenorEDepoisIdMenor() {
        candidatas(questao(1, 6), questao(7, 4), questao(3, 4));
        assertEquals(3L, servico.selecionar(ASSUNTO, 5, INICIAL).orElseThrow().id());
    }

    @Test
    void jsonDaSelecaoContemSomenteCamposExibiveis() throws Exception {
        candidatas(questao(1, 1));
        var dto = servico.selecionar(ASSUNTO, 1, INICIAL).orElseThrow();
        var mapper = new ObjectMapper();
        var json = mapper.readTree(mapper.writeValueAsBytes(dto));
        Set<String> campos = new HashSet<>();
        json.fieldNames().forEachRemaining(campos::add);
        assertEquals(Set.of("id", "assuntoId", "dificuldade", "enunciado",
                "alternativaA", "alternativaB", "alternativaC", "alternativaD"), campos);
        assertEquals("15", json.get("alternativaB").textValue());
    }
}
