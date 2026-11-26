package com.educria.service;

import com.educria.domain.entity.Questao;
import com.educria.domain.entity.RespostaAluno;
import com.educria.dto.FeedbackResposta;
import com.educria.dto.FeedbackResposta.TipoOrientacao;

import java.util.Locale;
import java.util.regex.Pattern;

/** Monta o retorno pedagógico de uma tentativa já registrada. */
final class FeedbackPedagogico {
    private static final Pattern PERCENTUAL_DE_UM_TOTAL =
            Pattern.compile("Quanto é [0-9]+(?:[,.][0-9]+)?% de [0-9]+(?:[,.][0-9]+)?\\?",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private FeedbackPedagogico() {}

    /**
     * Um acerto devolve a explicação disponível. O primeiro erro recebe uma dica inicial, o segundo recebe
     * uma orientação mais concreta e, a partir do terceiro, o retorno pode revelar a solução cadastrada.
     *
     * @param resposta tentativa já registrada, com o nível e os pontos daquele passo
     * @param erros total de erros do aluno para a questão na sessão até o passo informado
     * @return orientação compatível com o resultado e com os dados disponíveis na questão
     */
    static FeedbackResposta montar(RespostaAluno resposta, long erros) {
        Questao questao = resposta.getQuestao();
        if (resposta.isCorreta()) {
            return criar(resposta, TipoOrientacao.EXPLICACAO, questao.getExplicacao(), null, null, null);
        }

        Dicas dicas = dicas(questao);
        if (erros < 3) {
            return erros <= 1
                    ? criar(resposta, TipoOrientacao.DICA_INICIAL, dicas.inicial(), erros, null, null)
                    : criar(resposta, TipoOrientacao.DICA_APRIMORADA, dicas.aprimorada(), erros, null, null);
        }

        String correta = questao.getRespostaCorreta();
        if (correta != null) correta = correta.toUpperCase(Locale.ROOT);
        String texto = questao.textoAlternativa(correta);
        String explicacao = questao.getExplicacao();
        if (explicacao == null || explicacao.isBlank()) explicacao = dicas.aprimorada();
        if (texto == null || texto.isBlank()) {
            return criar(resposta, TipoOrientacao.RESOLUCAO, explicacao, erros, null, null);
        }
        return criar(resposta, TipoOrientacao.RESOLUCAO, explicacao, erros, correta, texto);
    }

    private static FeedbackResposta criar(RespostaAluno resposta, TipoOrientacao tipo, String texto,
                                          Long erros, String letraCorreta, String textoCorreto) {
        return new FeedbackResposta(resposta.getRespostaDada(), resposta.isCorreta(), texto,
                resposta.getNivelAnterior(), resposta.getNivelAtual(), resposta.isReaprender(),
                tipo, erros, letraCorreta, textoCorreto, resposta.getPontosIndividuais());
    }

    private record Dicas(String inicial, String aprimorada) {}

    private static Dicas dicas(Questao questao) {
        String assunto = questao.getAssunto() == null ? "" : questao.getAssunto().getNome().toLowerCase(Locale.ROOT);
        String enunciado = questao.getEnunciado();
        if (assunto.equals("porcentagem") && enunciado != null && PERCENTUAL_DE_UM_TOTAL.matcher(enunciado.strip()).matches()) {
            return new Dicas(
                    "O valor que aparece depois de «de» é o total. A pergunta pede uma parte percentual desse total.",
                    "Divida o número junto ao sinal de porcentagem por 100. Multiplique a fração obtida pelo total indicado depois de «de».");
        }
        return switch (assunto) {
            case "somar e subtrair" -> new Dicas(
                    "A pergunta envolve juntar quantidades ou descobrir o que sobra ou falta? Isso ajuda a escolher a operação.",
                    "Alinhe unidades com unidades e dezenas com dezenas. Calcule da direita para a esquerda e confira o resultado com a operação inversa.");
            case "multiplicar e dividir" -> new Dicas(
                    "Identifique os grupos e a quantidade em cada grupo. Qual desses valores a pergunta pede?",
                    "Se souber o número de grupos e a quantidade em cada um, multiplique para achar o total. Se souber o total e um desses fatores, divida para achar o outro.");
            case "frações" -> new Dicas(
                    "Identifique o todo. O denominador indica em quantas partes iguais ele foi dividido.",
                    "Para comparar, somar ou subtrair frações, escreva-as com o mesmo denominador. Para calcular uma fração de uma quantidade, divida o total pelo denominador e multiplique pelo numerador.");
            case "porcentagem" -> new Dicas(
                    "Identifique qual quantidade representa 100% e qual porcentagem a pergunta quer usar ou descobrir.",
                    "Escreva a porcentagem como uma fração de denominador 100. Se a pergunta der a porcentagem, aplique a fração ao valor de referência. Em um aumento ou desconto, some ou subtraia a variação.");
            case "proporção" -> new Dicas(
                    "Identifique as duas grandezas. Quando uma aumenta, a outra também aumenta ou diminui?",
                    "Coloque os valores correspondentes em duas linhas. Se a relação for direta, iguale as razões. Se for inversa, compare os produtos dos valores correspondentes.");
            case "equações" -> new Dicas(
                    "Localize a incógnita e identifique quais operações foram feitas com ela.",
                    "Se houver parênteses, simplifique primeiro. Use operações inversas nos dois lados para isolar a incógnita e confira substituindo o valor na equação original.");
            case "área e perímetro" -> new Dicas(
                    "A pergunta pede a medida do contorno ou da superfície? Veja quais medidas da figura foram dadas.",
                    "Para o contorno, some os comprimentos dos lados. Para a superfície, identifique a figura e use a fórmula de área correspondente. Confira se a unidade da resposta é linear ou quadrada.");
            case "várias etapas" -> new Dicas(
                    "Comece pela parte da pergunta que pode ser resolvida com os dados já disponíveis.",
                    "Anote o resultado de cada etapa com sua unidade e confira em qual etapa seguinte ele será usado. Ao final, volte à pergunta para conferir se obteve a quantidade pedida.");
            default -> new Dicas(
                    "Identifique o que a pergunta pede e destaque o dado do enunciado que ajuda a começar.",
                    "Escreva o que você sabe e o que ainda precisa descobrir. Explique como um dado leva ao próximo passo e compare o resultado com as condições da pergunta.");
        };
    }
}
