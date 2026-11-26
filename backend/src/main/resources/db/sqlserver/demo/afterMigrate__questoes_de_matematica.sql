-- Callback do perfil dev. A ordem corresponde a q1–q80 no protótipo.
DECLARE @id_demo bigint = -1;
DECLARE @titulo nvarchar(200) = N'Demonstração de Matemática do EDUCRIA';
DECLARE @enunciado nvarchar(max) = N'Banco de questões do protótipo EDUCRIA (q1 a q80).';
DECLARE @transacao_propria bit = CASE WHEN @@TRANCOUNT = 0 THEN 1 ELSE 0 END;

-- A transação protege toda a carga, inclusive quando o Flyway chama o callback em autocommit.
IF @transacao_propria = 1 BEGIN TRANSACTION;
BEGIN TRY
    DECLARE @bloqueio int;
    EXEC @bloqueio = sys.sp_getapplock @Resource = N'educria.carga_demo_matematica',
        @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 15000;
    IF @bloqueio < 0 THROW 51042, N'Não foi possível obter o bloqueio da carga de demonstração.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.atividades WITH (UPDLOCK,HOLDLOCK) WHERE id = @id_demo)
    BEGIN
        SET IDENTITY_INSERT dbo.atividades ON;
        BEGIN TRY
            INSERT INTO dbo.atividades (id, titulo, enunciado) VALUES (@id_demo, @titulo, @enunciado);
            SET IDENTITY_INSERT dbo.atividades OFF;
        END TRY
        BEGIN CATCH
            SET IDENTITY_INSERT dbo.atividades OFF;
            THROW;
        END CATCH;
    END;

    IF NOT EXISTS (SELECT 1 FROM dbo.atividades WHERE id = @id_demo AND titulo = @titulo AND enunciado = @enunciado)
        THROW 51040, N'O ID de demonstração já pertence a outro conteúdo.', 1;

    DECLARE @itens TABLE (
        ordem int PRIMARY KEY, assunto nvarchar(100), dificuldade int, enunciado nvarchar(max),
        a nvarchar(500), b nvarchar(500), c nvarchar(500), d nvarchar(500),
        correta nvarchar(1), explicacao nvarchar(max)
    );

    INSERT INTO @itens VALUES
    (1, N'somar e subtrair', 1, N'Quanto é 7 + 8?', N'13', N'15', N'16', N'17', N'B', N'7 mais 8 é 15.'),
    (2, N'somar e subtrair', 1, N'Quanto é 12 + 9?', N'19', N'20', N'21', N'22', N'C', N'12 mais 8 é 20, mais 1 é 21.'),
    (3, N'somar e subtrair', 2, N'Quanto é 23 - 9?', N'12', N'13', N'14', N'16', N'C', N'Tire 3 para chegar em 20 e depois mais 6.'),
    (4, N'somar e subtrair', 2, N'Quanto é 40 + 35?', N'65', N'70', N'75', N'85', N'C', N'40 mais 30 é 70, mais 5 é 75.'),
    (5, N'somar e subtrair', 3, N'Quanto é 100 - 62?', N'28', N'38', N'42', N'48', N'B', N'De 62 até 100 faltam 38.'),
    (6, N'somar e subtrair', 3, N'Quanto é 128 + 46?', N'164', N'172', N'174', N'184', N'C', N'128 mais 40 é 168, mais 6 é 174.'),
    (7, N'somar e subtrair', 4, N'Quanto é 205 - 88?', N'107', N'113', N'117', N'123', N'C', N'205 menos 100 é 105. Devolva 12 e fica 117.'),
    (8, N'somar e subtrair', 4, N'Quanto é 347 + 265?', N'592', N'602', N'612', N'622', N'C', N'300 mais 200 é 500, 47 mais 65 é 112.'),
    (9, N'somar e subtrair', 5, N'Quanto é 1000 - 437?', N'537', N'553', N'563', N'573', N'C', N'De 437 até 1000 faltam 563.'),
    (10, N'somar e subtrair', 5, N'Quanto é 486 + 279 - 115?', N'630', N'640', N'650', N'660', N'C', N'486 mais 279 é 765, menos 115 é 650.'),
    (11, N'multiplicar e dividir', 1, N'Quanto é 7 × 8?', N'48', N'54', N'56', N'64', N'C', N'Sete vezes 8 é 56.'),
    (12, N'multiplicar e dividir', 1, N'Quanto é 9 × 6?', N'45', N'54', N'56', N'63', N'B', N'Nove vezes 6 é 54.'),
    (13, N'multiplicar e dividir', 2, N'Quanto é 72 ÷ 9?', N'6', N'7', N'8', N'9', N'C', N'9 vezes 8 dá 72.'),
    (14, N'multiplicar e dividir', 2, N'Quanto é 12 × 5?', N'50', N'55', N'60', N'65', N'C', N'12 vezes 10 é 120. Metade disso é 60.'),
    (15, N'multiplicar e dividir', 3, N'Quanto é 96 ÷ 8?', N'11', N'12', N'13', N'14', N'B', N'8 vezes 12 é 96.'),
    (16, N'multiplicar e dividir', 3, N'Quanto é 25 × 6?', N'125', N'130', N'150', N'155', N'C', N'25 vezes 6 é 100 mais 50.'),
    (17, N'multiplicar e dividir', 4, N'Quanto é 108 ÷ 4?', N'24', N'26', N'27', N'28', N'C', N'Metade de 108 é 54 e metade de 54 é 27.'),
    (18, N'multiplicar e dividir', 4, N'Quanto é 144 ÷ 12?', N'10', N'11', N'12', N'14', N'C', N'12 vezes 12 é 144.'),
    (19, N'multiplicar e dividir', 5, N'Quanto é 336 ÷ 14?', N'22', N'24', N'26', N'28', N'B', N'14 vezes 24 é 336.'),
    (20, N'multiplicar e dividir', 5, N'Quanto é 23 × 18?', N'394', N'404', N'414', N'424', N'C', N'23 vezes 20 é 460, tire 23 vezes 2.'),
    (21, N'frações', 1, N'Quanto é 1/2 de 48?', N'16', N'20', N'24', N'28', N'C', N'Metade de 48 é 24.'),
    (22, N'frações', 1, N'Quanto é 1/3 + 1/3?', N'1/6', N'2/3', N'2/6', N'1', N'B', N'Denominador igual, some só os numeradores.'),
    (23, N'frações', 2, N'Quanto é 3/4 de 100?', N'25', N'50', N'75', N'80', N'C', N'Cada quarto vale 25.'),
    (24, N'frações', 2, N'A fração 5/10 simplificada fica:', N'1/5', N'1/2', N'2/5', N'5/2', N'B', N'Divida os dois por 5.'),
    (25, N'frações', 3, N'Quanto é 2/5 de 40?', N'8', N'15', N'16', N'20', N'C', N'Um quinto de 40 é 8.'),
    (26, N'frações', 3, N'Quanto é 1/4 + 1/2?', N'2/6', N'3/4', N'1/3', N'2/4', N'B', N'1/2 vale 2/4. Somando com 1/4 dá 3/4.'),
    (27, N'frações', 4, N'Quanto é 3/8 de 64?', N'16', N'21', N'24', N'32', N'C', N'Um oitavo de 64 é 8.'),
    (28, N'frações', 4, N'Qual é maior, 2/3 ou 3/5?', N'2/3', N'3/5', N'são iguais', N'não dá para comparar', N'A', N'2/3 é cerca de 0,67 e 3/5 é 0,60.'),
    (29, N'frações', 5, N'Quanto é 2/3 - 1/4?', N'1/12', N'5/12', N'1/2', N'3/7', N'B', N'Iguale para doze avos: 8/12 menos 3/12.'),
    (30, N'frações', 5, N'Quanto é 3/5 × 10/9?', N'1/3', N'2/3', N'3/4', N'5/6', N'B', N'Multiplique em cima e embaixo e simplifique.'),
    (31, N'porcentagem', 1, N'Quanto é 50% de 36?', N'12', N'16', N'18', N'20', N'C', N'Metade de 36 é 18.'),
    (32, N'porcentagem', 1, N'Quanto é 10% de 250?', N'2,5', N'15', N'25', N'50', N'C', N'10% é dividir por 10.'),
    (33, N'porcentagem', 2, N'Quanto é 25% de 80?', N'16', N'20', N'25', N'30', N'B', N'25% é a quarta parte.'),
    (34, N'porcentagem', 2, N'Quanto é 30% de 200?', N'50', N'60', N'70', N'80', N'B', N'10% é 20 e três vezes isso é 60.'),
    (35, N'porcentagem', 3, N'Quanto é 15% de 400?', N'40', N'55', N'60', N'75', N'C', N'10% é 40 e 5% é 20.'),
    (36, N'porcentagem', 3, N'Um valor de 250 sofre aumento de 10%. Quanto passa a valer?', N'260', N'265', N'275', N'285', N'C', N'10% de 250 é 25.'),
    (37, N'porcentagem', 4, N'40% de um valor é 60. Qual é o valor?', N'100', N'120', N'150', N'180', N'C', N'Se 40% é 60, então 10% é 15.'),
    (38, N'porcentagem', 4, N'Um produto de 200 reais tem 30% de desconto. Qual o preço final?', N'130', N'140', N'150', N'170', N'B', N'30% de 200 é 60.'),
    (39, N'porcentagem', 5, N'Um salário de 1500 sobe 20% e depois cai 10% do novo valor. Quanto fica?', N'1500', N'1620', N'1650', N'1800', N'B', N'1500 vira 1800 e 10% de 1800 é 180.'),
    (40, N'porcentagem', 5, N'Um valor sobe de 80 para 100. Qual foi o aumento em porcentagem?', N'20%', N'25%', N'80%', N'125%', N'B', N'O aumento foi de 20 sobre 80.'),
    (41, N'proporção', 1, N'Três cadernos custam 18 reais. Quanto custam cinco cadernos?', N'24', N'28', N'30', N'36', N'C', N'Cada caderno custa 6 reais.'),
    (42, N'proporção', 1, N'Dois quilos de arroz custam 12 reais. Quanto custam cinco quilos?', N'24', N'28', N'30', N'32', N'C', N'Cada quilo custa 6 reais.'),
    (43, N'proporção', 2, N'Um carro faz 60 km em 1 hora. Quanto faz em 3 horas na mesma velocidade?', N'120', N'150', N'180', N'240', N'C', N'Três vezes 60.'),
    (44, N'proporção', 2, N'Seis canetas custam 9 reais. Quanto custam dez canetas?', N'12', N'13', N'15', N'18', N'C', N'Cada caneta custa 1,50.'),
    (45, N'proporção', 3, N'Uma receita para 4 pessoas usa 6 ovos. Quantos ovos para 6 pessoas?', N'7', N'8', N'9', N'10', N'C', N'Cada pessoa usa 1,5 ovo.'),
    (46, N'proporção', 3, N'Num mapa, 1 cm equivale a 5 km. Quantos km são 7 cm?', N'12', N'25', N'35', N'40', N'C', N'Sete vezes 5.'),
    (47, N'proporção', 4, N'Se 3/9 é igual a x/12, quanto vale x?', N'3', N'4', N'5', N'6', N'B', N'3/9 é 1/3, e um terço de 12 é 4.'),
    (48, N'proporção', 4, N'Quatro operários fazem uma obra em 8 dias. Oito operários fazem em quantos dias?', N'2', N'4', N'6', N'16', N'B', N'Mais gente, menos tempo, na mesma proporção.'),
    (49, N'proporção', 5, N'Seis máquinas fazem um serviço em 10 dias. Quatro máquinas fazem em quantos dias?', N'8', N'12', N'15', N'20', N'C', N'O total de trabalho é 60. Dividido por 4 dá 15.'),
    (50, N'proporção', 5, N'Cinco pedreiros levantam um muro em 12 dias. Quantos dias levam três pedreiros?', N'15', N'18', N'20', N'24', N'C', N'O total é 60. Dividido por 3 dá 20.'),
    (51, N'equações', 1, N'Se x + 12 = 30, quanto vale x?', N'12', N'16', N'18', N'22', N'C', N'Tire 12 dos dois lados.'),
    (52, N'equações', 1, N'Se 2x = 14, quanto vale x?', N'5', N'6', N'7', N'8', N'C', N'Divida os dois lados por 2.'),
    (53, N'equações', 2, N'Se x ÷ 3 = 9, quanto vale x?', N'12', N'21', N'27', N'33', N'C', N'Multiplique os dois lados por 3.'),
    (54, N'equações', 2, N'Se x - 7 = 15, quanto vale x?', N'8', N'20', N'22', N'25', N'C', N'Some 7 dos dois lados.'),
    (55, N'equações', 3, N'Se 2x + 5 = 21, quanto vale x?', N'6', N'7', N'8', N'9', N'C', N'Tire 5 e fica 2x igual a 16.'),
    (56, N'equações', 3, N'Se 5x - 8 = 12, quanto vale x?', N'3', N'4', N'5', N'6', N'B', N'Some 8 e fica 5x igual a 20.'),
    (57, N'equações', 4, N'Se 3(x - 2) = 15, quanto vale x?', N'5', N'6', N'7', N'9', N'C', N'Divida por 3 e fica x menos 2 igual a 5.'),
    (58, N'equações', 4, N'Se x/4 + 3 = 8, quanto vale x?', N'12', N'16', N'20', N'24', N'C', N'Tire 3 e fica x sobre 4 igual a 5.'),
    (59, N'equações', 5, N'Se 4x + 2 = 2x + 10, quanto vale x?', N'2', N'3', N'4', N'6', N'C', N'Junte os x de um lado e fica 2x igual a 8.'),
    (60, N'equações', 5, N'Se 2(x + 3) = 3(x - 1), quanto vale x?', N'3', N'6', N'9', N'12', N'C', N'Fica 2x mais 6 igual a 3x menos 3.'),
    (61, N'área e perímetro', 1, N'Qual é o perímetro de um quadrado de lado 9 cm?', N'18 cm', N'27 cm', N'36 cm', N'81 cm', N'C', N'Some os quatro lados.'),
    (62, N'área e perímetro', 1, N'Qual é a área de um quadrado de lado 6 cm?', N'12 cm²', N'24 cm²', N'36 cm²', N'42 cm²', N'C', N'Lado vezes lado.'),
    (63, N'área e perímetro', 2, N'Qual é a área de um retângulo de 12 cm por 7 cm?', N'19 cm²', N'74 cm²', N'84 cm²', N'96 cm²', N'C', N'Base vezes altura.'),
    (64, N'área e perímetro', 2, N'Qual é o perímetro de um retângulo de 8 cm por 5 cm?', N'13 cm', N'20 cm', N'26 cm', N'40 cm', N'C', N'Some os dois lados e multiplique por 2.'),
    (65, N'área e perímetro', 3, N'Qual é a área de um triângulo de base 10 cm e altura 6 cm?', N'16 cm²', N'30 cm²', N'60 cm²', N'120 cm²', N'B', N'Base vezes altura dividido por 2.'),
    (66, N'área e perímetro', 3, N'Qual é a área de um retângulo de 15 cm por 4 cm?', N'19 cm²', N'38 cm²', N'60 cm²', N'64 cm²', N'C', N'15 vezes 4 é 60.'),
    (67, N'área e perímetro', 4, N'Um quadrado tem área de 49 cm². Quanto mede o lado?', N'6 cm', N'7 cm', N'12 cm', N'14 cm', N'B', N'7 vezes 7 é 49.'),
    (68, N'área e perímetro', 4, N'Qual é o perímetro de um triângulo equilátero de lado 11 cm?', N'22 cm', N'33 cm', N'44 cm', N'121 cm', N'B', N'Três lados iguais de 11.'),
    (69, N'área e perímetro', 5, N'Um retângulo tem perímetro 30 cm e base 9 cm. Quanto mede a altura?', N'5 cm', N'6 cm', N'7 cm', N'9 cm', N'B', N'Base mais altura é 15, então a altura é 6.'),
    (70, N'área e perímetro', 5, N'Um terreno de 20 m por 15 m tem metade construída. Qual a área construída?', N'100 m²', N'150 m²', N'200 m²', N'300 m²', N'B', N'A área total é 300 e metade é 150.'),
    (71, N'várias etapas', 1, N'Duas pizzas são cortadas em 8 fatias cada. Cinco pessoas comem 3 fatias. Quantas sobram?', N'1', N'2', N'3', N'4', N'A', N'São 16 fatias e 15 foram comidas.'),
    (72, N'várias etapas', 1, N'Uma caixa tem 5 pacotes de 8 barras, divididos entre 10 crianças. Quantas cada uma recebe?', N'3', N'4', N'5', N'8', N'B', N'São 40 barras para 10 crianças.'),
    (73, N'várias etapas', 2, N'Uma turma tem 40 alunos e 60% são meninas. Quantos são meninos?', N'14', N'16', N'20', N'24', N'B', N'São 24 meninas, sobram 16.'),
    (74, N'várias etapas', 2, N'Você compra 4 camisas de 38 reais e paga com 200. Qual o troco?', N'42', N'48', N'52', N'58', N'B', N'O total é 152.'),
    (75, N'várias etapas', 3, N'Três cadernos custam 12 reais cada. Com 25% de desconto no total, quanto se paga?', N'27', N'30', N'33', N'36', N'A', N'O total é 36 e o desconto tira 9.'),
    (76, N'várias etapas', 3, N'Um ônibus já percorreu 3/5 de uma viagem de 250 km. Quantos km faltam?', N'50', N'100', N'150', N'200', N'B', N'Faltam 2/5 de 250.'),
    (77, N'várias etapas', 4, N'Uma loja vende 12 peças a 45 reais e dá 15% de desconto no total. Quanto recebe?', N'459', N'486', N'510', N'540', N'A', N'O total é 540 e o desconto tira 81.'),
    (78, N'várias etapas', 4, N'Um reservatório de 800 litros perde 25% pela manhã e 30% do resto à tarde. Quanto sobra?', N'400', N'420', N'450', N'480', N'B', N'Sobram 600 pela manhã e 30% disso são 180.'),
    (79, N'várias etapas', 5, N'Uma caixa com 24 lápis tem 1/3 doado e o resto dividido entre 4 alunos. Quantos cada um recebe?', N'3', N'4', N'5', N'6', N'B', N'Foram doados 8 e sobraram 16 para 4 alunos.'),
    (80, N'várias etapas', 5, N'Numa viagem de 480 km, 1/4 é feito de manhã e 40% do restante à tarde. Quantos km faltam?', N'180', N'216', N'240', N'260', N'B', N'Manhã 120, resta 360, tarde 144, faltam 216.');

    DECLARE @materia bigint = (SELECT id FROM dbo.materias WHERE nome = N'Matemática');
    IF EXISTS (
        SELECT 1 FROM @itens i
        WHERE NOT EXISTS (SELECT 1 FROM dbo.assuntos s WHERE s.materia_id = @materia AND s.nome = i.assunto)
    )
        THROW 51041, N'O catálogo de Matemática precisa conter os assuntos da demonstração.', 1;

    INSERT INTO dbo.questoes (atividade_id, ordem, assunto_id, dificuldade, enunciado,
                             alternativa_a, alternativa_b, alternativa_c, alternativa_d, resposta_correta, explicacao)
    SELECT @id_demo, i.ordem, s.id, i.dificuldade, i.enunciado, i.a, i.b, i.c, i.d, i.correta, i.explicacao
    FROM @itens i
    JOIN dbo.assuntos s ON s.materia_id = @materia AND s.nome = i.assunto
    WHERE NOT EXISTS (SELECT 1 FROM dbo.questoes q WHERE q.atividade_id = @id_demo AND q.ordem = i.ordem);

    IF @transacao_propria = 1 COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @transacao_propria = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
