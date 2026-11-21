package com.educria.service;

import com.educria.domain.entity.Assunto;
import com.educria.domain.entity.Questao;
import com.educria.domain.entity.RespostaAluno;
import com.educria.dto.FeedbackResposta.TipoOrientacao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackPedagogicoTest {
    private static final String EXPLICACAO = "Divida 18 por 3 e multiplique o preço de um caderno por 5. O resultado é 30 reais.";
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void distingueDicasESoSerializaGabaritoDepoisDoTerceiroErro() {
        var resposta = resposta(false, EXPLICACAO);
        var primeira = FeedbackPedagogico.montar(resposta, 1);
        var segunda = FeedbackPedagogico.montar(resposta, 2);
        assertEquals(TipoOrientacao.DICA_INICIAL, primeira.tipoOrientacao());
        assertTrue(primeira.explicacao().contains("grandezas"));
        assertEquals(TipoOrientacao.DICA_APRIMORADA, segunda.tipoOrientacao());
        assertTrue(segunda.explicacao().contains("iguale as razões"));
        for (var dica : List.of(primeira, segunda)) {
            var publica = json.valueToTree(dica);
            assertFalse(publica.has("alternativaCorreta"));
            assertFalse(publica.has("textoAlternativaCorreta"));
            assertFalse(dica.explicacao().contains("30 reais"));
        }
        for (long erros : new long[]{3, 4}) {
            var resolucao = FeedbackPedagogico.montar(resposta, erros);
            assertEquals(TipoOrientacao.RESOLUCAO, resolucao.tipoOrientacao());
            assertEquals(erros, resolucao.errosDaQuestao());
            assertEquals("C", resolucao.alternativaCorreta());
            assertEquals("30 reais", resolucao.textoAlternativaCorreta());
            assertEquals(EXPLICACAO, resolucao.explicacao());
            assertFalse(resolucao.correta());
            assertTrue(resolucao.reaprender());
            assertEquals(6, resolucao.nivelAtual());
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t "})
    void orientaPeloAssuntoQuandoFaltaExplicacaoCadastrada(String explicacao) {
        var feedback = FeedbackPedagogico.montar(resposta(false, explicacao), 3);

        assertEquals(TipoOrientacao.RESOLUCAO, feedback.tipoOrientacao());
        assertEquals("30 reais", feedback.textoAlternativaCorreta());
        assertTrue(feedback.explicacao().contains("iguale as razões"));
    }

    @Test
    void aproveitaEnunciadoDePorcentagemDiretaSemCalcularOResultado() {
        var resposta = resposta(false, "10% de 250 é 25.");
        ReflectionTestUtils.setField(resposta.getQuestao().getAssunto(), "nome", "porcentagem");
        resposta.getQuestao().setEnunciado("Quanto é 10% de 250?");

        var feedback = FeedbackPedagogico.montar(resposta, 2);

        assertTrue(feedback.explicacao().contains("depois de «de»"));
        assertFalse(feedback.explicacao().contains("25"));
        assertNull(feedback.alternativaCorreta());
    }

    @Test
    void acertoPreservaExplicacaoSemRevelacaoDeErro() {
        var feedback = FeedbackPedagogico.montar(resposta(true, EXPLICACAO), 1);

        assertEquals(TipoOrientacao.EXPLICACAO, feedback.tipoOrientacao());
        assertEquals(EXPLICACAO, feedback.explicacao());
        assertTrue(feedback.correta());
        assertFalse(feedback.reaprender());
        assertNull(feedback.errosDaQuestao());
        assertNull(feedback.alternativaCorreta());
        assertNull(feedback.textoAlternativaCorreta());
    }

    @Test
    void erroLegadoUsaAPoliticaDeDicasSemAlterarAPendencia() {
        var resposta = resposta(false, EXPLICACAO);
        resposta.setReaprender(false);

        var feedback = FeedbackPedagogico.montar(resposta, 1);

        assertFalse(feedback.reaprender());
        assertEquals(TipoOrientacao.DICA_INICIAL, feedback.tipoOrientacao());
        assertNotEquals(EXPLICACAO, feedback.explicacao());
        assertNull(feedback.alternativaCorreta());
    }

    private RespostaAluno resposta(boolean correta, String explicacao) {
        var assunto = new Assunto();
        ReflectionTestUtils.setField(assunto, "nome", "proporção");
        var questao = Questao.builder().assunto(assunto).enunciado("Três cadernos custam 18 reais. Quanto custam cinco cadernos?")
                .respostaCorreta("c").alternativaC("30 reais").explicacao(explicacao).build();
        return RespostaAluno.builder().questao(questao).correta(correta).reaprender(!correta)
                .respostaDada(correta ? "C" : "A").nivelAnterior(6).nivelAtual(6).build();
    }
}
