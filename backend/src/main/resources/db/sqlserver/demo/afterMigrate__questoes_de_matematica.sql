-- Callback do perfil dev. As ordens 1–80 vêm do protótipo e 81–160 ampliam os níveis 6–10.
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
    (26, N'frações', 3, N'Quanto é 1/4 + 1/2?', N'2/6', N'3/4', N'1/6', N'2/4', N'B', N'1/2 vale 2/4. Somando com 1/4 dá 3/4.'),
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
    (71, N'várias etapas', 1, N'Duas pizzas são cortadas em 8 fatias cada. Cinco pessoas comem 3 fatias cada uma. Quantas sobram?', N'1', N'2', N'3', N'4', N'A', N'São 16 fatias. As cinco pessoas comem 5 × 3 = 15 fatias.'),
    (72, N'várias etapas', 1, N'Uma caixa tem 5 pacotes de 8 barras, divididos entre 10 crianças. Quantas cada uma recebe?', N'3', N'4', N'5', N'8', N'B', N'São 40 barras para 10 crianças.'),
    (73, N'várias etapas', 2, N'Uma turma tem 40 alunos e 60% são meninas. Quantos são meninos?', N'14', N'16', N'20', N'24', N'B', N'São 24 meninas, sobram 16.'),
    (74, N'várias etapas', 2, N'Você compra 4 camisas de 38 reais e paga com 200. Qual o troco?', N'42', N'48', N'52', N'58', N'B', N'O total é 152.'),
    (75, N'várias etapas', 3, N'Três cadernos custam 12 reais cada. Com 25% de desconto no total, quanto se paga?', N'27', N'30', N'33', N'36', N'A', N'O total é 36 e o desconto tira 9.'),
    (76, N'várias etapas', 3, N'Um ônibus já percorreu 3/5 de uma viagem de 250 km. Quantos km faltam?', N'50', N'100', N'150', N'200', N'B', N'Faltam 2/5 de 250.'),
    (77, N'várias etapas', 4, N'Uma loja vende 12 peças a 45 reais e dá 15% de desconto no total. Quanto recebe?', N'459', N'486', N'510', N'540', N'A', N'O total é 540 e o desconto tira 81.'),
    (78, N'várias etapas', 4, N'Um reservatório de 800 litros perde 25% pela manhã e 30% do resto à tarde. Quanto sobra?', N'400', N'420', N'450', N'480', N'B', N'Sobram 600 pela manhã e 30% disso são 180.'),
    (79, N'várias etapas', 5, N'Uma caixa com 24 lápis tem 1/3 doado e o resto dividido entre 4 alunos. Quantos cada um recebe?', N'3', N'4', N'5', N'6', N'B', N'Foram doados 8 e sobraram 16 para 4 alunos.'),
    (80, N'várias etapas', 5, N'Numa viagem de 480 km, 1/4 é feito de manhã e 40% do restante à tarde. Quantos km faltam?', N'180', N'216', N'240', N'260', N'B', N'Manhã 120, resta 360, tarde 144, faltam 216.');

    -- Ampliação nas dificuldades 6 a 10.
    INSERT INTO @itens VALUES
    -- Somar e subtrair
    (81, N'somar e subtrair', 6, N'Uma biblioteca tinha 248 livros. Recebeu 87 e emprestou 129. Com quantos livros ficou?', N'196', N'206', N'216', N'232', N'B', N'Some 248 e 87 para obter 335. Subtraia os 129 livros emprestados.'),
    (82, N'somar e subtrair', 6, N'Um mergulhador está a 18 m abaixo da superfície. Sobe 7 m e desce 12 m. A quantos metros abaixo da superfície ele termina?', N'23', N'13', N'17', N'37', N'A', N'Visto como profundidade, subir reduz 18 para 11 e descer aumenta 11 para 23.'),
    (83, N'somar e subtrair', 7, N'A temperatura de uma cidade passou de −7 °C para 6 °C. De quantos graus foi o aumento?', N'1', N'7', N'12', N'13', N'D', N'De −7 a 0 são 7 graus e de 0 a 6 são mais 6.'),
    (84, N'somar e subtrair', 7, N'Na entrada de um parque, havia 320 pessoas. Saíram 74, entraram 58 e saíram outras 39. Quantas pessoas ficaram?', N'226', N'281', N'265', N'287', N'C', N'Calcule 320 − 74 + 58 − 39. O saldo das movimentações é de 55 pessoas a menos.'),
    (85, N'somar e subtrair', 8, N'Dois jogadores começam com zero. Lara ganha 42 pontos, perde 18 e ganha 13. Ivo ganha 31, perde 7 e ganha 16. Por quantos pontos Ivo vence?', N'2', N'3', N'7', N'9', N'B', N'Lara soma 37 e Ivo soma 40. A diferença é 3.'),
    (86, N'somar e subtrair', 8, N'Um elevador sai do piso −3. Sobe 12 andares, desce 5 e sobe 2. Qual é o número do piso final?', N'6', N'2', N'8', N'12', N'A', N'Partindo de −3, os pisos intermediários são 9 e 4. Depois da última subida, o elevador chega ao piso 6.'),
    (87, N'somar e subtrair', 9, N'Uma conta estava com saldo de −34 reais. Um depósito de 95 reais foi seguido por duas compras de 28 e 41 reais. Quanto falta depositar para o saldo chegar a 20 reais?', N'8', N'12', N'20', N'28', N'D', N'O saldo fica em −8 reais. Para chegar a 20 reais, deposite mais 28.'),
    (88, N'somar e subtrair', 9, N'Uma equipe quer encerrar a campanha com 450 doações. Começou a semana com 297. Recebeu 63 e descobriu que 18 doações do total haviam sido contadas duas vezes. Quantas novas doações ainda precisa receber?', N'72', N'90', N'108', N'126', N'C', N'O total corrigido é 297 + 63 − 18 = 342. Faltam 108 para atingir 450.'),
    (89, N'somar e subtrair', 10, N'Uma trilha começa a 85 m de altitude. Os três primeiros trechos sobem 47 m, descem 96 m e sobem 18 m. O ponto final deve estar a 35 m de altitude. Quantos metros o último trecho precisa descer?', N'15', N'19', N'35', N'54', N'B', N'Após os três trechos, a altitude é 54 m. Para chegar a 35 m, desça 19 m.'),
    (90, N'somar e subtrair', 10, N'Três equipes juntaram 156, 123 e 98 latas. Um lote de 27 latas da primeira equipe já estava contado nas 123 da segunda. Depois de retirar mais 14 latas amassadas, quantas latas restam?', N'336', N'350', N'363', N'377', N'A', N'O total bruto é 377. Retire as 27 duplicadas e as 14 amassadas.'),
    -- Multiplicar e dividir
    (91, N'multiplicar e dividir', 6, N'Um evento tem 18 fileiras com 14 cadeiras em cada. Para o ensaio, retiram-se 4 cadeiras de cada fileira. Quantas cadeiras ficam?', N'180', N'176', N'212', N'248', N'A', N'Cada fileira fica com 10 cadeiras. Há 18 fileiras.'),
    (92, N'multiplicar e dividir', 6, N'Um posto precisa enviar 275 livros em caixas que comportam até 24 livros. Qual é o menor número de caixas necessário?', N'10', N'11', N'13', N'12', N'D', N'Onze caixas comportam 264 livros. Os 11 restantes precisam de mais uma caixa.'),
    (93, N'multiplicar e dividir', 7, N'Seis máquinas iguais produzem juntas 420 peças em 5 horas. Mantido o ritmo, quantas peças uma dessas máquinas produz em 8 horas?', N'70', N'84', N'112', N'140', N'C', N'Por máquina e por hora, a produção é 420 ÷ 30 = 14 peças. Em 8 horas, uma máquina faz 112.'),
    (94, N'multiplicar e dividir', 7, N'Uma cooperativa tem 18 caixas com 24 frutas em cada. Depois de separar 72 frutas, distribui o restante igualmente entre 15 escolas. Quantas frutas cada escola recebe?', N'18', N'24', N'26', N'30', N'B', N'São 432 frutas no início e 360 após a separação. Divida por 15.'),
    (95, N'multiplicar e dividir', 8, N'Com 250 mudas, uma escola quer montar canteiros de 18 mudas. Se reservar 16 mudas para reposição, quantos canteiros completos pode montar?', N'13', N'12', N'14', N'15', N'A', N'Sobram 234 mudas para o plantio. Divida 234 por 18.'),
    (96, N'multiplicar e dividir', 8, N'Um galpão guarda 16 caixas com 25 peças em cada. Para enviar todas as peças, elas serão reembaladas em pacotes de 8. Cada carro leva até 22 pacotes. Qual é o menor número de carros necessário?', N'2', N'4', N'5', N'3', N'D', N'As 400 peças formam 50 pacotes. Dois carros levam só 44; o terceiro leva os seis pacotes restantes.'),
    (97, N'multiplicar e dividir', 9, N'Uma escola levou 268 pessoas em ônibus. Há três ônibus com 48 lugares para passageiros e os demais têm 32. Qual é o menor total de ônibus para levar todos, usando os três ônibus maiores?', N'4', N'6', N'7', N'8', N'C', N'Os três maiores levam 144 pessoas. Para as 124 restantes, são necessários quatro ônibus de 32 lugares.'),
    (98, N'multiplicar e dividir', 9, N'Cada painel precisa de 14 parafusos. Uma caixa com 120 parafusos tem 8 defeituosos. Quantos painéis completos podem ser montados com três caixas que apresentam essa mesma perda?', N'22', N'24', N'25', N'26', N'B', N'Cada caixa fornece 112 parafusos utilizáveis, totalizando 336. Divida por 14.'),
    (99, N'multiplicar e dividir', 10, N'Um mercado precisa separar pelo menos 210 garrafas em caixas inteiras de 12 ou 18. Há no máximo cinco caixas de 18 disponíveis e caixas de 12 suficientes. Qual é o menor total de caixas necessário?', N'15', N'14', N'16', N'17', N'A', N'Catorze caixas levam no máximo 5 × 18 + 9 × 12 = 198. Com cinco de 18 e dez de 12, são 210.'),
    (100, N'multiplicar e dividir', 10, N'Dois depósitos tinham, cada um, 15 caixas de 24 livros. Um enviou 9 caixas completas e o outro repartiu todos os seus livros em 10 lotes iguais e enviou 4 lotes. Quantos livros não foram enviados pelos dois depósitos juntos?', N'324', N'384', N'400', N'360', N'D', N'No primeiro ficam 6 × 24 = 144 livros. No segundo ficam 6 lotes de 36, somando 216.'),
    -- Frações
    (101, N'frações', 6, N'Uma jarra de 240 mL estava cheia. Foram servidos 3/8 do líquido. Quantos mililitros ficaram?', N'90', N'120', N'180', N'150', N'D', N'Foram servidos 90 mL. Os outros 5/8 totalizam 150 mL.'),
    (102, N'frações', 6, N'Uma estudante leu 1/6 de um livro de manhã e 1/4 à tarde. O livro tem 144 páginas. Quantas páginas ela leu no total nesse dia?', N'36', N'48', N'60', N'84', N'C', N'As duas frações equivalem a 2/12 e 3/12. Cinco doze avos de 144 são 60.'),
    (103, N'frações', 7, N'De uma fita de 180 cm, usa-se primeiro 1/3. Depois, usa-se 1/4 do que sobrou. Quantos centímetros restam?', N'75', N'90', N'105', N'120', N'B', N'Após o primeiro corte, restam 120 cm. O segundo corta 30 cm.'),
    (104, N'frações', 7, N'Uma caixa tinha 96 fichas. Das fichas, 5/8 eram vermelhas. Se 1/3 das fichas vermelhas forem retiradas, quantas vermelhas ficam?', N'40', N'20', N'32', N'60', N'A', N'Das 60 fichas vermelhas, 20 são retiradas. Restam 40.'),
    (105, N'frações', 8, N'Um reservatório estava cheio. Foram usados 2/5 da capacidade na manhã e 1/6 da capacidade à tarde. Restaram 78 litros. Qual era a capacidade total, em litros?', N'156', N'168', N'195', N'180', N'D', N'Restaram 13/30 da capacidade. Se 13 partes valem 78 litros, 30 partes valem 180.'),
    (106, N'frações', 8, N'Um grupo percorreu 3/7 de uma rota e depois mais 2/3 do trecho restante. Ainda faltam 24 km. Qual é a extensão total da rota, em quilômetros?', N'84', N'105', N'126', N'168', N'C', N'Após a primeira etapa restam 4/7. A última parte é um terço desse resto, ou 4/21 da rota.'),
    (107, N'frações', 9, N'Duas turmas receberam 120 livros cada. A primeira doou 3/8 dos seus livros. A segunda doou 1/4 dos seus livros e depois 1/5 do restante. Quantos livros ficaram nas duas turmas ao todo?', N'141', N'147', N'150', N'155', N'B', N'Restam 75 livros na primeira turma e 72 na segunda. Some os dois valores.'),
    (108, N'frações', 9, N'Um tanque estava cheio. Perdeu 1/5 da capacidade e depois 1/4 da quantidade que ainda tinha. Foram repostos 18 litros, e então o tanque chegou a 3/4 da capacidade. Qual é a capacidade, em litros?', N'120', N'90', N'108', N'144', N'A', N'Antes da reposição, o tanque tinha 3/5 da capacidade. A diferença até 3/4 é 3/20, que vale 18 litros.'),
    (109, N'frações', 10, N'Uma turma leu 2/9 de um livro na segunda e 3/7 do que restava na terça. Na quarta leu metade do restante. Se ainda faltavam 40 páginas, quantas páginas tem o livro?', N'140', N'160', N'200', N'180', N'D', N'Restam sucessivamente 7/9, 4/9 e 2/9 do livro. Se 2/9 correspondem a 40 páginas, o livro tem 180.'),
    (110, N'frações', 10, N'Um pacote de figurinhas foi dividido em duas etapas. Ana recebeu 1/3 do total, e Bia recebeu 3/8 das que restaram. Depois disso, Caio ficou com as 35 restantes. Quantas figurinhas Bia recebeu?', N'14', N'28', N'21', N'35', N'C', N'Caio ficou com 5/8 do que Ana deixou. Se 5 partes valem 35, as 3 partes de Bia valem 21.'),
    -- Porcentagem
    (111, N'porcentagem', 6, N'Um fone de 240 reais recebeu desconto de 15%. Qual é o preço final em reais?', N'196', N'210', N'204', N'225', N'C', N'O desconto é 36 reais. Subtraia esse valor de 240.'),
    (112, N'porcentagem', 6, N'Uma feira vendeu 180 ingressos, dos quais 35% eram meias-entradas. Quantos eram ingressos inteiros?', N'63', N'117', N'108', N'126', N'B', N'Inteiros correspondem a 65% do total. Calcule 180 × 0,65.'),
    (113, N'porcentagem', 7, N'Após um desconto de 20%, o preço de um tênis ficou em 144 reais. Quanto custava antes do desconto?', N'180', N'160', N'172', N'192', N'A', N'Os 144 reais correspondem a 80% do preço. Divida 144 por 0,8.'),
    (114, N'porcentagem', 7, N'Uma turma tinha 25 participantes em abril e 35 em maio. Qual foi o aumento percentual em relação a abril? Responda em porcentagem.', N'25', N'29', N'60', N'40', N'D', N'O aumento de 10 representa 10 ÷ 25 = 0,4 ou 40%.'),
    (115, N'porcentagem', 8, N'Um aparelho de 500 reais recebe desconto de 20% e, depois, um desconto adicional de 15% sobre o preço reduzido. Qual é o preço final em reais?', N'325', N'350', N'340', N'360', N'C', N'O primeiro desconto deixa 400 reais. Tire 60 reais, que são 15% de 400.'),
    (116, N'porcentagem', 8, N'Um valor de 320 reais aumentou 25% e caiu 10% sobre o novo preço. Quantos reais ele aumentou em relação ao valor inicial?', N'32', N'40', N'48', N'80', N'B', N'O valor passa por 400 e termina em 360 reais. O aumento líquido é 40 reais.'),
    (117, N'porcentagem', 9, N'Uma escola tem 250 alunos. Deles, 60% frequentam oficinas. Entre os que frequentam oficinas, 30% fazem teatro. Quantos alunos da escola fazem teatro nessas oficinas?', N'45', N'30', N'75', N'90', N'A', N'São 150 alunos nas oficinas. Trinta por cento de 150 são 45.'),
    (118, N'porcentagem', 9, N'Num grupo de 120 pessoas, 30% preferem o turno da manhã. Chegam 30 pessoas e metade delas prefere a manhã. Qual fica sendo a porcentagem de pessoas que preferem a manhã?', N'30', N'35', N'40', N'34', N'D', N'Havia 36 pessoas e chegaram mais 15 com essa preferência. São 51 de 150, ou 34%.'),
    (119, N'porcentagem', 10, N'Uma loja vende um item por 360 reais depois de aplicar dois descontos sucessivos de 20% e 10%. Qual era o preço antes dos descontos, em reais?', N'450', N'480', N'500', N'520', N'C', N'Os fatores 0,8 e 0,9 deixam 72% do preço. Divida 360 por 0,72.'),
    (120, N'porcentagem', 10, N'Uma pesquisa tinha 200 respostas, com 55% favoráveis. As 20 respostas seguintes foram favoráveis. Se todas as próximas também forem favoráveis, qual é o menor número adicional de respostas para atingir pelo menos 65%?', N'13', N'38', N'20', N'37', N'B', N'Há 130 favoráveis de 220. Mais 37 dão 167 de 257, abaixo de 65%; mais 38 dão 168 de 258, acima de 65%.'),
    -- Proporção
    (121, N'proporção', 6, N'Para preparar 5 litros de tinta, misturam-se 2 litros de azul e 3 de branco. Mantendo a mistura, quantos litros de branco há em 35 litros de tinta pronta?', N'14', N'21', N'20', N'25', N'B', N'O branco corresponde a 3 de cada 5 litros. Multiplique 35 por 3/5.'),
    (122, N'proporção', 6, N'Em um mapa, 3 cm representam 12 km. Uma trilha está dividida em dois trechos de 6 cm e 4 cm nesse mapa. Qual é o comprimento total da trilha, em quilômetros?', N'40', N'24', N'32', N'48', N'A', N'Cada centímetro representa 4 km. São 10 cm ao todo.'),
    (123, N'proporção', 7, N'Em uma equipe, a razão entre adultos e jovens é 3 para 5. Há 48 pessoas e 6 jovens saem. Quantos jovens permanecem?', N'18', N'20', N'30', N'24', N'D', N'Oito partes somam 48, então cada parte tem 6. Havia 30 jovens, e agora há 24.'),
    (124, N'proporção', 7, N'Uma receita serve 8 pessoas com 600 g de farinha. Haverá 18 pessoas, mas já há 450 g de farinha na despensa. Quantos gramas ainda precisam ser comprados?', N'750', N'1050', N'900', N'1350', N'C', N'A receita usa 75 g por pessoa. Para 18 pessoas, usa 1.350 g. Desconte os 450 disponíveis.'),
    (125, N'proporção', 8, N'Oito pessoas realizam uma tarefa em 15 horas, todas trabalhando ao mesmo ritmo. Se 12 pessoas fizerem a mesma tarefa, quantas horas serão necessárias?', N'8', N'10', N'12', N'18', N'B', N'A tarefa exige 120 horas-pessoa. Divida por 12.'),
    (126, N'proporção', 8, N'Um carro percorre 156 km com 12 litros de combustível. Mantido o consumo, quantos quilômetros ainda pode percorrer após andar 65 km com um tanque inicialmente abastecido com 20 litros?', N'195', N'180', N'200', N'260', N'A', N'A autonomia inicial é 20 × 13 km. Subtraia os 65 km percorridos.'),
    (127, N'proporção', 9, N'Quatro impressoras iguais imprimem 600 páginas em 5 minutos. Duas ficam indisponíveis. Quantos minutos as outras duas levarão para imprimir 900 páginas?', N'10', N'12', N'18', N'15', N'D', N'Cada impressora faz 30 páginas por minuto. Duas fazem 60, e 900 ÷ 60 = 15.'),
    (128, N'proporção', 9, N'Uma maquete usa escala 1:250. No modelo, um corredor mede 8 cm e sua extensão mede 4 cm. Quantos metros mede o percurso completo real?', N'12', N'20', N'30', N'34', N'C', N'São 12 cm na maquete, equivalentes a 3.000 cm ou 30 m reais.'),
    (129, N'proporção', 10, N'Seis trabalhadores, todos com o mesmo ritmo, realizariam um serviço em 10 dias. Eles trabalham por 4 dias e depois três trabalhadores deixam a equipe. Quantos dias completos serão necessários desde o início?', N'12', N'16', N'14', N'18', N'B', N'O serviço requer 60 dias-pessoa. Após 24 dias-pessoa, faltam 36. Os três restantes precisam de 12 dias.'),
    (130, N'proporção', 10, N'Há 36 litros de solução com água e concentrado na razão 5 para 1. Quantos litros de concentrado puro precisam ser acrescentados para a razão água para concentrado passar a 3 para 1?', N'4', N'2', N'6', N'10', N'A', N'Há 30 litros de água e 6 de concentrado. Para a razão 3 para 1, é preciso ter 10 de concentrado.'),
    -- Equações
    (131, N'equações', 6, N'Dois ingressos de mesmo preço e uma taxa de 6 reais custam juntos 34 reais. Qual é o preço de um ingresso, em reais?', N'14', N'11', N'16', N'20', N'A', N'Retire a taxa de 34 e divida os 28 restantes por 2.'),
    (132, N'equações', 6, N'O triplo de um número, diminuído de 7, é igual ao dobro desse número, aumentado de 8. Qual é o número?', N'1', N'7', N'21', N'15', N'D', N'De 3x − 7 = 2x + 8, subtraia 2x e some 7 aos dois lados.'),
    (133, N'equações', 7, N'Numa caixa, há lápis e canetas. Há 6 canetas a mais que lápis e são 34 peças ao todo. Quantos lápis há?', N'12', N'17', N'14', N'20', N'C', N'Se x é o número de lápis, x + (x + 6) = 34. Logo, 2x = 28.'),
    (134, N'equações', 7, N'Resolva a equação 4(x − 3) = 2(x + 5). Qual é o valor de x?', N'8', N'11', N'10', N'12', N'B', N'Distribua para obter 4x − 12 = 2x + 10. Isso equivale a 2x = 22.'),
    (135, N'equações', 8, N'Uma assinatura A cobra 15 reais de taxa e 3 reais por aula. Outra, B, cobra 5 reais de taxa e 5 reais por aula. Com quantas aulas os preços são iguais?', N'5', N'2', N'4', N'10', N'A', N'Iguale 15 + 3x e 5 + 5x. A diferença de 10 reais nas taxas compensa 2 reais por aula.'),
    (136, N'equações', 8, N'Resolva a equação (x − 4)/3 + (x + 2)/2 = 8. Qual é o valor de x?', N'8', N'11', N'14', N'10', N'D', N'Multiplique toda a equação por 6. Então 2x − 8 + 3x + 6 = 48, ou 5x = 50.'),
    (137, N'equações', 9, N'Um preço desconhecido é reduzido em 20%. Em seguida, são somados 12 reais de frete, e o total é 84 reais. Qual era o preço inicial, em reais?', N'72', N'84', N'90', N'96', N'C', N'Da equação 0,8x + 12 = 84, obtemos 0,8x = 72.'),
    (138, N'equações', 9, N'Quatro números inteiros consecutivos têm soma de 86. Qual é o menor deles?', N'19', N'20', N'21', N'22', N'B', N'Use x, x + 1, x + 2 e x + 3. A soma é 4x + 6.'),
    (139, N'equações', 10, N'Um cofre contém oito moedas, todas de 1 ou 2 reais, que somam 13 reais. Quantas moedas são de 2 reais?', N'5', N'3', N'6', N'7', N'A', N'Com oito moedas de 1 real, teríamos 8 reais. Cada troca para uma moeda de 2 acrescenta 1 real.'),
    (140, N'equações', 10, N'Uma estudante gastou 1/3 do dinheiro e depois mais 18 reais. Ela guardou toda a sobra, que era metade da quantia inicial. Quantos reais possuía no início?', N'72', N'90', N'120', N'108', N'D', N'A diferença entre o dinheiro após o primeiro gasto, 2/3 do total, e o que guardou, 1/2, é 1/6 do total. Esse sexto vale 18.'),
    -- Área e perímetro
    (141, N'área e perímetro', 6, N'De um retângulo de 14 cm por 9 cm recorta-se um quadrado de lado 4 cm. Qual é a área restante, em centímetros quadrados?', N'94', N'112', N'122', N'110', N'D', N'A área original é 126 cm². O quadrado recortado tem 16 cm².'),
    (142, N'área e perímetro', 6, N'Dois quadrados de lado 5 cm são unidos por um lado completo, sem sobreposição. Qual é o perímetro da figura formada, em centímetros?', N'20', N'25', N'30', N'40', N'C', N'A figura é um retângulo de 10 cm por 5 cm. O lado compartilhado fica no interior.'),
    (143, N'área e perímetro', 7, N'Um jardim retangular de 16 m por 10 m tem uma faixa de 2 m de largura, por dentro, ao longo de toda a borda. Qual é a área da parte interna livre, em metros quadrados?', N'84', N'72', N'96', N'112', N'B', N'Retire 4 m de cada dimensão, pois a faixa aparece nas duas extremidades de cada lado. A parte livre mede 12 m por 6 m.'),
    (144, N'área e perímetro', 7, N'Um terreno triangular tem base de 18 m e altura de 12 m. Uma horta quadrada de lado 6 m ocupa parte dele. Qual é a área sem horta, em metros quadrados?', N'72', N'64', N'84', N'108', N'A', N'A área do triângulo é 108 m². A horta ocupa 36 m².'),
    (145, N'área e perímetro', 8, N'Um retângulo de 18 m por 12 m tem um canto recortado em forma de quadrado de lado 4 m. Qual é o perímetro da figura restante, em metros?', N'52', N'56', N'68', N'60', N'D', N'Os dois segmentos de 4 m retirados da borda externa são substituídos por dois segmentos internos de mesmo comprimento. O perímetro continua 2 × (18 + 12).'),
    (146, N'área e perímetro', 8, N'Um canteiro retangular de 12 m por 8 m está centrado em um terreno de 16 m por 12 m. Quantos metros quadrados do terreno ficam fora do canteiro?', N'80', N'104', N'96', N'112', N'C', N'O terreno tem 192 m² e o canteiro tem 96 m². Subtraia as áreas.'),
    (147, N'área e perímetro', 9, N'Um retângulo tem perímetro de 52 cm. O comprimento é 6 cm maior que a largura. Qual é a área, em centímetros quadrados?', N'144', N'160', N'156', N'168', N'B', N'A soma do comprimento com a largura é 26. As dimensões que diferem em 6 são 16 e 10.'),
    (148, N'área e perímetro', 9, N'Um pátio de 9 m por 7 m será coberto com placas quadradas de 50 cm de lado. Sem perda ou sobreposição, quantas placas são necessárias?', N'252', N'126', N'196', N'280', N'A', N'Cabem 18 placas no comprimento e 14 na largura. São 18 × 14 placas.'),
    (149, N'área e perímetro', 10, N'Uma moldura retangular externa de 18 cm por 14 cm tem 2 cm de largura em toda a volta. Qual é a área ocupada pela moldura, em centímetros quadrados?', N'96', N'104', N'140', N'112', N'D', N'A área externa é 252 cm² e a abertura mede 14 cm por 10 cm, com área de 140 cm².'),
    (150, N'área e perímetro', 10, N'Uma cerca cobre apenas três lados de um terreno retangular. O lado junto a um rio, sem cerca, mede 18 m. O terreno tem área de 216 m². Se é preciso deixar uma abertura de 3 m para um portão no lado oposto ao rio, quantos metros de cerca devem ser comprados?', N'36', N'42', N'39', N'45', N'C', N'Cada lado perpendicular ao rio mede 216 ÷ 18 = 12 m. O lado oposto precisa de 15 m de cerca.'),
    -- Várias etapas
    (151, N'várias etapas', 6, N'Uma turma compra 8 pacotes com 12 lápis cada. Separa 16 lápis para reposição e divide o resto entre 10 equipes. Quantos lápis recebe cada equipe?', N'7', N'9', N'8', N'10', N'C', N'As equipes repartem 96 − 16 = 80 lápis.'),
    (152, N'várias etapas', 6, N'Um passeio custa 18 reais por pessoa para 12 alunos. A escola contribui com 72 reais e divide o restante igualmente entre os 12 alunos. Quantos reais cada aluno paga?', N'10', N'12', N'14', N'16', N'B', N'O custo é 216 reais. Desconte a contribuição e divida 144 por 12.'),
    (153, N'várias etapas', 7, N'Uma feira arrecadou 750 reais. Gastou 20% desse valor com materiais e dividiu o restante igualmente entre três projetos. Quantos reais recebeu cada projeto?', N'200', N'150', N'180', N'250', N'A', N'Restam 600 reais após o gasto. Divida entre os três projetos.'),
    (154, N'várias etapas', 7, N'Uma escola tem 240 folhas. Usa 1/4 para cartazes e divide o restante em kits de 12 folhas. Se doa 4 kits, quantos kits ficam na escola?', N'9', N'12', N'15', N'11', N'D', N'As 180 folhas restantes formam 15 kits. Depois da doação ficam 11 kits.'),
    (155, N'várias etapas', 8, N'Um grupo tem 480 reais. Compra 6 ingressos de 52 reais, com desconto de 25% no total dos ingressos, e gasta 72 reais no transporte. Quanto sobra, em reais?', N'96', N'156', N'174', N'186', N'C', N'O preço dos ingressos cai de 312 para 234 reais. Com o transporte, o gasto chega a 306 reais.'),
    (156, N'várias etapas', 8, N'Uma associação tem 18 caixas com 20 kg de alimentos. Destina 2/5 do total a um abrigo e separa o restante em pacotes de 9 kg. Quantos pacotes obtém?', N'16', N'24', N'20', N'26', N'B', N'Dos 360 kg, restam 216 kg para os pacotes. Divida por 9.'),
    (157, N'várias etapas', 9, N'Um museu cobra 24 reais por adulto e 12 reais por criança. Um grupo de 10 adultos e 15 crianças tem desconto de 10% sobre a soma das entradas e paga mais 42 reais pelo transporte. Qual é o custo total, em reais?', N'420', N'378', N'402', N'462', N'A', N'As entradas somam 420 reais. Com desconto, custam 378. Some 42 de transporte.'),
    (158, N'várias etapas', 9, N'Um jardim de 12 m por 9 m reserva 1/3 da área para circulação. Em cada 6 m² da área restante é usado um saco de adubo. Cada saco custa 17 reais. Qual é o gasto em reais?', N'187', N'216', N'306', N'204', N'D', N'A área útil é 72 m², que requer 12 sacos. Multiplique por 17.'),
    (159, N'várias etapas', 10, N'Um clube separou 600 reais para uma atividade. Usou 1/5 em materiais e 25% do restante com transporte. O que sobrou paga igualmente as inscrições dos 12 participantes. Qual é o valor de cada inscrição, em reais?', N'25', N'35', N'30', N'40', N'C', N'Restam 480 reais após os materiais. O transporte custa 120, deixando 360 para as inscrições.'),
    (160, N'várias etapas', 10, N'Uma escola precisa de 96 cadernos. A loja A vende pacotes de 12 por 42 reais e dá 10% de desconto no total. A loja B vende pacotes de 16 por 49 reais e cobra 12 reais de entrega. Qual é o menor custo em reais, arredondado ao real mais próximo?', N'294', N'302', N'306', N'336', N'B', N'A loja A custa 8 × 42 × 0,90 = 302,40 reais. A loja B custa 6 × 49 + 12 = 306 reais. Arredonde o menor valor para 302.');

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
