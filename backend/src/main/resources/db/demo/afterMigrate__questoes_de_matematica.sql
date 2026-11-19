-- Callback exclusivo do perfil dev. A ordem corresponde a q1…q80 no protótipo.
DO $carga$
DECLARE
    id_demo CONSTANT bigint := -1;
    titulo_demo CONSTANT text := 'Demonstração de Matemática do EDUCRIA';
    texto_demo CONSTANT text := 'Banco de questões do protótipo EDUCRIA (q1 a q80).';
    atividade_demo atividades%ROWTYPE;
    id_assunto bigint;
    item record;
BEGIN
    -- Uma identidade estável fora da sequência BIGSERIAL evita confundir títulos iguais.
    INSERT INTO atividades (id, titulo, enunciado)
    VALUES (id_demo, titulo_demo, texto_demo)
    ON CONFLICT (id) DO NOTHING;

    SELECT * INTO STRICT atividade_demo FROM atividades WHERE id = id_demo FOR UPDATE;
    IF atividade_demo.titulo IS DISTINCT FROM titulo_demo OR
       atividade_demo.enunciado IS DISTINCT FROM texto_demo THEN
        RAISE EXCEPTION 'O id % de atividades já pertence a outro conteúdo. Carga de demonstração cancelada.', id_demo;
    END IF;

    FOR item IN
        SELECT * FROM (VALUES
            (1, 'somar e subtrair', 1, 'Quanto é 7 + 8?', '13', '15', '16', '17', 'B', '7 mais 8 é 15.'),
            (2, 'somar e subtrair', 1, 'Quanto é 12 + 9?', '19', '20', '21', '22', 'C', '12 mais 8 é 20, mais 1 é 21.'),
            (3, 'somar e subtrair', 2, 'Quanto é 23 - 9?', '12', '13', '14', '16', 'C', 'Tire 3 para chegar em 20 e depois mais 6.'),
            (4, 'somar e subtrair', 2, 'Quanto é 40 + 35?', '65', '70', '75', '85', 'C', '40 mais 30 é 70, mais 5 é 75.'),
            (5, 'somar e subtrair', 3, 'Quanto é 100 - 62?', '28', '38', '42', '48', 'B', 'De 62 até 100 faltam 38.'),
            (6, 'somar e subtrair', 3, 'Quanto é 128 + 46?', '164', '172', '174', '184', 'C', '128 mais 40 é 168, mais 6 é 174.'),
            (7, 'somar e subtrair', 4, 'Quanto é 205 - 88?', '107', '113', '117', '123', 'C', '205 menos 100 é 105. Devolva 12 e fica 117.'),
            (8, 'somar e subtrair', 4, 'Quanto é 347 + 265?', '592', '602', '612', '622', 'C', '300 mais 200 é 500, 47 mais 65 é 112.'),
            (9, 'somar e subtrair', 5, 'Quanto é 1000 - 437?', '537', '553', '563', '573', 'C', 'De 437 até 1000 faltam 563.'),
            (10, 'somar e subtrair', 5, 'Quanto é 486 + 279 - 115?', '630', '640', '650', '660', 'C', '486 mais 279 é 765, menos 115 é 650.'),
            (11, 'multiplicar e dividir', 1, 'Quanto é 7 × 8?', '48', '54', '56', '64', 'C', 'Sete vezes 8 é 56.'),
            (12, 'multiplicar e dividir', 1, 'Quanto é 9 × 6?', '45', '54', '56', '63', 'B', 'Nove vezes 6 é 54.'),
            (13, 'multiplicar e dividir', 2, 'Quanto é 72 ÷ 9?', '6', '7', '8', '9', 'C', '9 vezes 8 dá 72.'),
            (14, 'multiplicar e dividir', 2, 'Quanto é 12 × 5?', '50', '55', '60', '65', 'C', '12 vezes 10 é 120. Metade disso é 60.'),
            (15, 'multiplicar e dividir', 3, 'Quanto é 96 ÷ 8?', '11', '12', '13', '14', 'B', '8 vezes 12 é 96.'),
            (16, 'multiplicar e dividir', 3, 'Quanto é 25 × 6?', '125', '130', '150', '155', 'C', '25 vezes 6 é 100 mais 50.'),
            (17, 'multiplicar e dividir', 4, 'Quanto é 108 ÷ 4?', '24', '26', '27', '28', 'C', 'Metade de 108 é 54 e metade de 54 é 27.'),
            (18, 'multiplicar e dividir', 4, 'Quanto é 144 ÷ 12?', '10', '11', '12', '14', 'C', '12 vezes 12 é 144.'),
            (19, 'multiplicar e dividir', 5, 'Quanto é 336 ÷ 14?', '22', '24', '26', '28', 'B', '14 vezes 24 é 336.'),
            (20, 'multiplicar e dividir', 5, 'Quanto é 23 × 18?', '394', '404', '414', '424', 'C', '23 vezes 20 é 460, tire 23 vezes 2.'),
            (21, 'frações', 1, 'Quanto é 1/2 de 48?', '16', '20', '24', '28', 'C', 'Metade de 48 é 24.'),
            (22, 'frações', 1, 'Quanto é 1/3 + 1/3?', '1/6', '2/3', '2/6', '1', 'B', 'Denominador igual, some só os numeradores.'),
            (23, 'frações', 2, 'Quanto é 3/4 de 100?', '25', '50', '75', '80', 'C', 'Cada quarto vale 25.'),
            (24, 'frações', 2, 'A fração 5/10 simplificada fica:', '1/5', '1/2', '2/5', '5/2', 'B', 'Divida os dois por 5.'),
            (25, 'frações', 3, 'Quanto é 2/5 de 40?', '8', '15', '16', '20', 'C', 'Um quinto de 40 é 8.'),
            (26, 'frações', 3, 'Quanto é 1/4 + 1/2?', '2/6', '3/4', '1/3', '2/4', 'B', '1/2 vale 2/4. Somando com 1/4 dá 3/4.'),
            (27, 'frações', 4, 'Quanto é 3/8 de 64?', '16', '21', '24', '32', 'C', 'Um oitavo de 64 é 8.'),
            (28, 'frações', 4, 'Qual é maior, 2/3 ou 3/5?', '2/3', '3/5', 'são iguais', 'não dá para comparar', 'A', '2/3 é cerca de 0,67 e 3/5 é 0,60.'),
            (29, 'frações', 5, 'Quanto é 2/3 - 1/4?', '1/12', '5/12', '1/2', '3/7', 'B', 'Iguale para doze avos: 8/12 menos 3/12.'),
            (30, 'frações', 5, 'Quanto é 3/5 × 10/9?', '1/3', '2/3', '3/4', '5/6', 'B', 'Multiplique em cima e embaixo e simplifique.'),
            (31, 'porcentagem', 1, 'Quanto é 50% de 36?', '12', '16', '18', '20', 'C', 'Metade de 36 é 18.'),
            (32, 'porcentagem', 1, 'Quanto é 10% de 250?', '2,5', '15', '25', '50', 'C', '10% é dividir por 10.'),
            (33, 'porcentagem', 2, 'Quanto é 25% de 80?', '16', '20', '25', '30', 'B', '25% é a quarta parte.'),
            (34, 'porcentagem', 2, 'Quanto é 30% de 200?', '50', '60', '70', '80', 'B', '10% é 20 e três vezes isso é 60.'),
            (35, 'porcentagem', 3, 'Quanto é 15% de 400?', '40', '55', '60', '75', 'C', '10% é 40 e 5% é 20.'),
            (36, 'porcentagem', 3, 'Um valor de 250 sofre aumento de 10%. Quanto passa a valer?', '260', '265', '275', '285', 'C', '10% de 250 é 25.'),
            (37, 'porcentagem', 4, '40% de um valor é 60. Qual é o valor?', '100', '120', '150', '180', 'C', 'Se 40% é 60, então 10% é 15.'),
            (38, 'porcentagem', 4, 'Um produto de 200 reais tem 30% de desconto. Qual o preço final?', '130', '140', '150', '170', 'B', '30% de 200 é 60.'),
            (39, 'porcentagem', 5, 'Um salário de 1500 sobe 20% e depois cai 10% do novo valor. Quanto fica?', '1500', '1620', '1650', '1800', 'B', '1500 vira 1800 e 10% de 1800 é 180.'),
            (40, 'porcentagem', 5, 'Um valor sobe de 80 para 100. Qual foi o aumento em porcentagem?', '20%', '25%', '80%', '125%', 'B', 'O aumento foi de 20 sobre 80.'),
            (41, 'proporção', 1, 'Três cadernos custam 18 reais. Quanto custam cinco cadernos?', '24', '28', '30', '36', 'C', 'Cada caderno custa 6 reais.'),
            (42, 'proporção', 1, 'Dois quilos de arroz custam 12 reais. Quanto custam cinco quilos?', '24', '28', '30', '32', 'C', 'Cada quilo custa 6 reais.'),
            (43, 'proporção', 2, 'Um carro faz 60 km em 1 hora. Quanto faz em 3 horas na mesma velocidade?', '120', '150', '180', '240', 'C', 'Três vezes 60.'),
            (44, 'proporção', 2, 'Seis canetas custam 9 reais. Quanto custam dez canetas?', '12', '13', '15', '18', 'C', 'Cada caneta custa 1,50.'),
            (45, 'proporção', 3, 'Uma receita para 4 pessoas usa 6 ovos. Quantos ovos para 6 pessoas?', '7', '8', '9', '10', 'C', 'Cada pessoa usa 1,5 ovo.'),
            (46, 'proporção', 3, 'Num mapa, 1 cm equivale a 5 km. Quantos km são 7 cm?', '12', '25', '35', '40', 'C', 'Sete vezes 5.'),
            (47, 'proporção', 4, 'Se 3/9 é igual a x/12, quanto vale x?', '3', '4', '5', '6', 'B', '3/9 é 1/3, e um terço de 12 é 4.'),
            (48, 'proporção', 4, 'Quatro operários fazem uma obra em 8 dias. Oito operários fazem em quantos dias?', '2', '4', '6', '16', 'B', 'Mais gente, menos tempo, na mesma proporção.'),
            (49, 'proporção', 5, 'Seis máquinas fazem um serviço em 10 dias. Quatro máquinas fazem em quantos dias?', '8', '12', '15', '20', 'C', 'O total de trabalho é 60. Dividido por 4 dá 15.'),
            (50, 'proporção', 5, 'Cinco pedreiros levantam um muro em 12 dias. Quantos dias levam três pedreiros?', '15', '18', '20', '24', 'C', 'O total é 60. Dividido por 3 dá 20.'),
            (51, 'equações', 1, 'Se x + 12 = 30, quanto vale x?', '12', '16', '18', '22', 'C', 'Tire 12 dos dois lados.'),
            (52, 'equações', 1, 'Se 2x = 14, quanto vale x?', '5', '6', '7', '8', 'C', 'Divida os dois lados por 2.'),
            (53, 'equações', 2, 'Se x ÷ 3 = 9, quanto vale x?', '12', '21', '27', '33', 'C', 'Multiplique os dois lados por 3.'),
            (54, 'equações', 2, 'Se x - 7 = 15, quanto vale x?', '8', '20', '22', '25', 'C', 'Some 7 dos dois lados.'),
            (55, 'equações', 3, 'Se 2x + 5 = 21, quanto vale x?', '6', '7', '8', '9', 'C', 'Tire 5 e fica 2x igual a 16.'),
            (56, 'equações', 3, 'Se 5x - 8 = 12, quanto vale x?', '3', '4', '5', '6', 'B', 'Some 8 e fica 5x igual a 20.'),
            (57, 'equações', 4, 'Se 3(x - 2) = 15, quanto vale x?', '5', '6', '7', '9', 'C', 'Divida por 3 e fica x menos 2 igual a 5.'),
            (58, 'equações', 4, 'Se x/4 + 3 = 8, quanto vale x?', '12', '16', '20', '24', 'C', 'Tire 3 e fica x sobre 4 igual a 5.'),
            (59, 'equações', 5, 'Se 4x + 2 = 2x + 10, quanto vale x?', '2', '3', '4', '6', 'C', 'Junte os x de um lado e fica 2x igual a 8.'),
            (60, 'equações', 5, 'Se 2(x + 3) = 3(x - 1), quanto vale x?', '3', '6', '9', '12', 'C', 'Fica 2x mais 6 igual a 3x menos 3.'),
            (61, 'área e perímetro', 1, 'Qual é o perímetro de um quadrado de lado 9 cm?', '18 cm', '27 cm', '36 cm', '81 cm', 'C', 'Some os quatro lados.'),
            (62, 'área e perímetro', 1, 'Qual é a área de um quadrado de lado 6 cm?', '12 cm²', '24 cm²', '36 cm²', '42 cm²', 'C', 'Lado vezes lado.'),
            (63, 'área e perímetro', 2, 'Qual é a área de um retângulo de 12 cm por 7 cm?', '19 cm²', '74 cm²', '84 cm²', '96 cm²', 'C', 'Base vezes altura.'),
            (64, 'área e perímetro', 2, 'Qual é o perímetro de um retângulo de 8 cm por 5 cm?', '13 cm', '20 cm', '26 cm', '40 cm', 'C', 'Some os dois lados e multiplique por 2.'),
            (65, 'área e perímetro', 3, 'Qual é a área de um triângulo de base 10 cm e altura 6 cm?', '16 cm²', '30 cm²', '60 cm²', '120 cm²', 'B', 'Base vezes altura dividido por 2.'),
            (66, 'área e perímetro', 3, 'Qual é a área de um retângulo de 15 cm por 4 cm?', '19 cm²', '38 cm²', '60 cm²', '64 cm²', 'C', '15 vezes 4 é 60.'),
            (67, 'área e perímetro', 4, 'Um quadrado tem área de 49 cm². Quanto mede o lado?', '6 cm', '7 cm', '12 cm', '14 cm', 'B', '7 vezes 7 é 49.'),
            (68, 'área e perímetro', 4, 'Qual é o perímetro de um triângulo equilátero de lado 11 cm?', '22 cm', '33 cm', '44 cm', '121 cm', 'B', 'Três lados iguais de 11.'),
            (69, 'área e perímetro', 5, 'Um retângulo tem perímetro 30 cm e base 9 cm. Quanto mede a altura?', '5 cm', '6 cm', '7 cm', '9 cm', 'B', 'Base mais altura é 15, então a altura é 6.'),
            (70, 'área e perímetro', 5, 'Um terreno de 20 m por 15 m tem metade construída. Qual a área construída?', '100 m²', '150 m²', '200 m²', '300 m²', 'B', 'A área total é 300 e metade é 150.'),
            (71, 'várias etapas', 1, 'Duas pizzas são cortadas em 8 fatias cada. Cinco pessoas comem 3 fatias. Quantas sobram?', '1', '2', '3', '4', 'A', 'São 16 fatias e 15 foram comidas.'),
            (72, 'várias etapas', 1, 'Uma caixa tem 5 pacotes de 8 barras, divididos entre 10 crianças. Quantas cada uma recebe?', '3', '4', '5', '8', 'B', 'São 40 barras para 10 crianças.'),
            (73, 'várias etapas', 2, 'Uma turma tem 40 alunos e 60% são meninas. Quantos são meninos?', '14', '16', '20', '24', 'B', 'São 24 meninas, sobram 16.'),
            (74, 'várias etapas', 2, 'Você compra 4 camisas de 38 reais e paga com 200. Qual o troco?', '42', '48', '52', '58', 'B', 'O total é 152.'),
            (75, 'várias etapas', 3, 'Três cadernos custam 12 reais cada. Com 25% de desconto no total, quanto se paga?', '27', '30', '33', '36', 'A', 'O total é 36 e o desconto tira 9.'),
            (76, 'várias etapas', 3, 'Um ônibus já percorreu 3/5 de uma viagem de 250 km. Quantos km faltam?', '50', '100', '150', '200', 'B', 'Faltam 2/5 de 250.'),
            (77, 'várias etapas', 4, 'Uma loja vende 12 peças a 45 reais e dá 15% de desconto no total. Quanto recebe?', '459', '486', '510', '540', 'A', 'O total é 540 e o desconto tira 81.'),
            (78, 'várias etapas', 4, 'Um reservatório de 800 litros perde 25% pela manhã e 30% do resto à tarde. Quanto sobra?', '400', '420', '450', '480', 'B', 'Sobram 600 pela manhã e 30% disso são 180.'),
            (79, 'várias etapas', 5, 'Uma caixa com 24 lápis tem 1/3 doado e o resto dividido entre 4 alunos. Quantos cada um recebe?', '3', '4', '5', '6', 'B', 'Foram doados 8 e sobraram 16 para 4 alunos.'),
            (80, 'várias etapas', 5, 'Numa viagem de 480 km, 1/4 é feito de manhã e 40% do restante à tarde. Quantos km faltam?', '180', '216', '240', '260', 'B', 'Manhã 120, resta 360, tarde 144, faltam 216.')
        ) AS dados(ordem, assunto, dificuldade, enunciado, a, b, c, d, correta, explicacao)
    LOOP
        SELECT s.id INTO STRICT id_assunto
        FROM assuntos s JOIN materias m ON m.id = s.materia_id
        WHERE m.nome = 'Matemática' AND s.nome = item.assunto;

        INSERT INTO questoes (atividade_id, ordem, assunto_id, dificuldade, enunciado,
                              alternativa_a, alternativa_b, alternativa_c, alternativa_d,
                              resposta_correta, explicacao)
        SELECT id_demo, item.ordem, id_assunto, item.dificuldade, item.enunciado,
               item.a, item.b, item.c, item.d, item.correta, item.explicacao
        WHERE NOT EXISTS (
            SELECT 1 FROM questoes WHERE atividade_id = id_demo AND ordem = item.ordem
        );
    END LOOP;
END;
$carga$;
