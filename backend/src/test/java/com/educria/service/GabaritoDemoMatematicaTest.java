package com.educria.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class GabaritoDemoMatematicaTest {
    private static final String LITERAL = "N'((?:''|[^'])*)'";
    private static final Pattern QUESTAO = Pattern.compile("(?m)^    \\((\\d+), " + LITERAL
            + ", (\\d+), " + (LITERAL + ", ").repeat(6) + LITERAL + "\\)[,;]$");
    private static final Pattern VALOR = Pattern.compile("(\\d+(?:[.,]\\d+)?)(?:/(\\d+))? *(%|cm²|cm|m²)?");

    private static final Object[] RESULTADOS_ORIGINAIS = {
            // Somar e subtrair (1 a 10)
            7 + 8, 12 + 9, 23 - 9, 40 + 35, 100 - 62, 128 + 46, 205 - 88, 347 + 265, 1000 - 437,
            486 + 279 - 115,
            // Multiplicar e dividir (11 a 20)
            7 * 8, 9 * 6, 72 / 9, 12 * 5, 96 / 8, 25 * 6, 108 / 4, 144 / 12, 336 / 14, 23 * 18,
            // Frações (21 a 30)
            48 / 2, "2/3", 100 * 3 / 4, "1/2", 40 * 2 / 5, "3/4", 64 * 3 / 8, "2/3", "5/12", "2/3",
            // Porcentagem (31 a 40)
            36 / 2, 250 / 10, 80 / 4, 200 * 30 / 100, 400 * 15 / 100, 250 * 110 / 100,
            60 * 100 / 40, 200 * 70 / 100, 1500 * 120 / 100 * 90 / 100, (100 - 80) * 100 / 80 + "%",
            // Proporção (41 a 50)
            18 / 3 * 5, 12 / 2 * 5, 60 * 3, 9 * 10 / 6, 6 * 6 / 4, 5 * 7, 12 * 3 / 9, 4 * 8 / 8,
            6 * 10 / 4, 5 * 12 / 3,
            // Equações (51 a 60)
            30 - 12, 14 / 2, 9 * 3, 15 + 7, (21 - 5) / 2, (12 + 8) / 5, 15 / 3 + 2, (8 - 3) * 4,
            (10 - 2) / (4 - 2), (2 * 3 + 3) / (3 - 2),
            // Área e perímetro (61 a 70)
            4 * 9 + " cm", 6 * 6 + " cm²", 12 * 7 + " cm²", 2 * (8 + 5) + " cm",
            10 * 6 / 2 + " cm²", 15 * 4 + " cm²", (int) Math.sqrt(49) + " cm", 3 * 11 + " cm",
            (30 / 2 - 9) + " cm", 20 * 15 / 2 + " m²",
            // Várias etapas (71 a 80)
            2 * 8 - 5 * 3, 5 * 8 / 10, 40 * 40 / 100, 200 - 4 * 38, 3 * 12 * 75 / 100, 250 * 2 / 5,
            12 * 45 * 85 / 100, 800 * 75 / 100 * 70 / 100, 24 * 2 / 3 / 4, 480 * 3 / 4 * 60 / 100,
    };

    // Cálculos a partir dos enunciados, independentes das alternativas e letras do SQL.
    private static final int[] RESPOSTAS_CALCULADAS = {
            // Somar e subtrair (81 a 90)
            248+87-129,
            18-7+12,
            6-(-7),
            320-74+58-39,
            (31-7+16)-(42-18+13),
            -3+12-5+2,
            20-(-34+95-28-41),
            450-(297+63-18),
            85+47-96+18-35,
            156+123+98-27-14,
            // Multiplicar e dividir (91 a 100)
            18*(14-4),
            (275+23)/24,
            420/(6*5)*8,
            (18*24-72)/15,
            (250-16)/18,
            (16*25/8+21)/22,
            3+(268-3*48+31)/32,
            3*(120-8)/14,
            5+(210-5*18+11)/12,
            (15-9)*24+6*(15*24/10),
            // Frações (101 a 110)
            240*5/8,
            144/6+144/4,
            180*2/3*3/4,
            96*5/8*2/3,
            78*30/(30-12-5),
            24*21/4,
            120*5/8+120*3/4*4/5,
            18*20/3,
            40*9/2,
            35*3/5,
            // Porcentagem (111 a 120)
            240*85/100,
            180*65/100,
            144*100/80,
            (35-25)*100/25,
            500*80/100*85/100,
            320*125/100*90/100-320,
            250*60/100*30/100,
            (120*30/100+30/2)*100/150,
            360*10000/(80*90),
            (220*65-(200*55+20*100)+34)/35,
            // Proporção (121 a 130)
            35*3/5,
            (6+4)*(12/3),
            48*5/8-6,
            600/8*18-450,
            8*15/12,
            20*(156/12)-65,
            900/(2*(600/(4*5))),
            (8+4)*250/100,
            4+(6*10-6*4)/3,
            (36*5/6)/3-36/6,
            // Equações (131 a 140)
            (34-6)/2,
            (8+7)/(3-2),
            (34-6)/2,
            (4*3+2*5)/(4-2),
            (15-5)/(5-3),
            (8*6+2*4-3*2)/(2+3),
            (84-12)*100/80,
            (86-1-2-3)/4,
            13-8,
            18*6,
            // Área e perímetro (141 a 150)
            14*9-4*4,
            2*(2*5+5),
            (16-4)*(10-4),
            18*12/2-6*6,
            2*(18+12)-2*4+2*4,
            16*12-12*8,
            ((52/2+6)/2)*((52/2-6)/2),
            (900/50)*(700/50),
            18*14-(18-4)*(14-4),
            2*(216/18)+18-3,
            // Várias etapas (151 a 160)
            (8*12-16)/10,
            (18*12-72)/12,
            750*80/100/3,
            (240*3/4)/12-4,
            480-6*52*75/100-72,
            18*20*3/5/9,
            (10*24+15*12)*90/100+42,
            12*9*2/3/6*17,
            600*4/5*75/100/12,
            (96/12*42*90+50)/100,
    };

    @Test
    void confereCalculosAlternativasEEnunciadosDasCentoESessentaQuestoes() throws Exception {
        var recurso = new ClassPathResource("db/sqlserver/demo/afterMigrate__questoes_de_matematica.sql");
        var linhas = QUESTAO.matcher(recurso.getContentAsString(StandardCharsets.UTF_8));
        Set<String> enunciados = new HashSet<>();
        Set<Character> letras = new HashSet<>();
        int quantidade = 0;
        while (linhas.find()) {
            int ordem = Integer.parseInt(linhas.group(1));
            assertEquals(++quantidade, ordem);
            assertTrue(enunciados.add(linhas.group(4)), "Enunciado repetido na ordem " + ordem);
            var alternativas = List.of(linhas.group(5), linhas.group(6), linhas.group(7), linhas.group(8))
                    .stream().map(GabaritoDemoMatematicaTest::normalizarValor).toList();
            assertEquals(4, new HashSet<>(alternativas).size(), "Alternativas equivalentes na ordem " + ordem);
            char letra = linhas.group(9).charAt(0);
            assertTrue(letra >= 'A' && letra <= 'D', "Letra inválida na ordem " + ordem);
            letras.add(letra);
            Object esperado = ordem <= 80 ? RESULTADOS_ORIGINAIS[ordem - 1] : RESPOSTAS_CALCULADAS[ordem - 81];
            assertEquals(normalizarValor(esperado.toString()), alternativas.get(letra - 'A'),
                    "Resultado calculado para a ordem " + ordem);
            assertFalse(linhas.group(10).isBlank(), "Explicação na ordem " + ordem);
        }
        assertEquals(160, quantidade);
        assertEquals(Set.of('A', 'B', 'C', 'D'), letras);
    }

    @Test
    void explicitaAsCondicoesDasQuestoesDePizzaEDinheiroGuardado() throws Exception {
        var recurso = new ClassPathResource("db/sqlserver/demo/afterMigrate__questoes_de_matematica.sql");
        var linhas = QUESTAO.matcher(recurso.getContentAsString(StandardCharsets.UTF_8));
        var condicoes = new java.util.HashMap<>(java.util.Map.of(71, "3 fatias cada uma", 140, "guardou toda a sobra"));
        while (linhas.find()) {
            int ordem = Integer.parseInt(linhas.group(1));
            String condicao = condicoes.remove(ordem);
            if (condicao != null) assertTrue(linhas.group(4).contains(condicao), "Condição ausente na ordem " + ordem);
        }
        assertTrue(condicoes.isEmpty(), "Questões ausentes da carga");
    }

    private static String normalizarValor(String texto) {
        String normalizado = texto.strip().toLowerCase(Locale.ROOT);
        var partes = VALOR.matcher(normalizado);
        if (!partes.matches()) return normalizado;

        var numero = new BigDecimal(partes.group(1).replace(',', '.'));
        if (partes.group(2) != null) {
            numero = numero.divide(new BigDecimal(partes.group(2)), MathContext.DECIMAL128);
        }
        String unidade = partes.group(3);
        return numero.stripTrailingZeros().toPlainString() + (unidade == null ? "" : unidade);
    }
}
