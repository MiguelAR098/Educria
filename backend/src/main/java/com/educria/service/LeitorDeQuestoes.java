package com.educria.service;

import com.educria.dto.QuestaoRequest;
import com.fasterxml.jackson.databind.JsonNode;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Lê um item do JSON de importação, no formato do protótipo do hackathon:
 * {"conceito":"porcentagem","dificuldade":2,"enunciado":"...","alts":["a","b","c","d"],"correta":1,"explicacao":"..."}
 * "correta" aceita o índice de 0 a 3 ou a letra A a D. Erros viram IllegalArgumentException
 * com uma mensagem que o professor consegue corrigir.
 */
final class LeitorDeQuestoes {

    private static final String LETRAS = "ABCD";

    private LeitorDeQuestoes() {}

    static QuestaoRequest ler(JsonNode item, Map<String, Long> assuntoPorNome) {
        if (item == null || !item.isObject()) {
            throw new IllegalArgumentException("o item precisa ser um objeto entre chaves");
        }

        String conceito = texto(item.get("conceito"));
        Long assuntoId = assuntoPorNome.get(normalizar(conceito));
        if (assuntoId == null) {
            throw new IllegalArgumentException(conceito.isEmpty()
                    ? "falta o campo conceito"
                    : "o conceito \"" + conceito + "\" não é um assunto da sua matéria");
        }

        JsonNode dificuldade = item.get("dificuldade");
        if (dificuldade == null || !dificuldade.isIntegralNumber() || !dificuldade.canConvertToInt()
                || dificuldade.asInt() < 1 || dificuldade.asInt() > 10) {
            throw new IllegalArgumentException("dificuldade precisa ser um número inteiro de 1 a 10");
        }

        String enunciado = texto(item.get("enunciado"));
        if (enunciado.isEmpty() || enunciado.length() > 2000) {
            throw new IllegalArgumentException("enunciado é obrigatório e deve ter até 2000 caracteres");
        }

        JsonNode alts = item.get("alts");
        if (alts == null || !alts.isArray() || alts.size() != 4) {
            throw new IllegalArgumentException("alts precisa ter exatamente 4 textos");
        }
        String[] alternativas = new String[4];
        for (int i = 0; i < 4; i++) {
            alternativas[i] = texto(alts.get(i));
            if (alternativas[i].isEmpty() || alternativas[i].length() > 500) {
                throw new IllegalArgumentException("cada alternativa precisa ter de 1 a 500 caracteres");
            }
        }

        String explicacao = texto(item.get("explicacao"));
        if (explicacao.length() > 1000) {
            throw new IllegalArgumentException("explicacao deve ter até 1000 caracteres");
        }

        return new QuestaoRequest(assuntoId, dificuldade.asInt(), enunciado,
                alternativas[0], alternativas[1], alternativas[2], alternativas[3],
                letraCorreta(item.get("correta")), explicacao.isEmpty() ? null : explicacao);
    }

    /** Minúsculas, sem acentos e com espaços simples: "Frações " e "fracoes" são o mesmo conceito. */
    static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String letraCorreta(JsonNode correta) {
        if (correta != null && correta.isIntegralNumber() && correta.canConvertToInt()
                && correta.asInt() >= 0 && correta.asInt() <= 3) {
            return String.valueOf(LETRAS.charAt(correta.asInt()));
        }
        if (correta != null && correta.isTextual()) {
            String letra = correta.asText().trim().toUpperCase(Locale.ROOT);
            if (letra.length() == 1 && LETRAS.contains(letra)) {
                return letra;
            }
        }
        throw new IllegalArgumentException("correta precisa ser um número de 0 a 3 ou uma letra de A a D");
    }

    private static String texto(JsonNode no) {
        return no != null && (no.isTextual() || no.isNumber()) ? no.asText().trim() : "";
    }
}
