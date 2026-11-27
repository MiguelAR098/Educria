-- Callback do perfil dev. As ordens 1–80 vêm do protótipo e 81–160 ampliam os níveis 6–10.
DECLARE @id_demo bigint = -1;
DECLARE @titulo nvarchar(200) = N'Demonstração de Matemática do EDUCRIA';
DECLARE @enunciado_legado nvarchar(max) = N'Banco de questões do protótipo EDUCRIA (q1 a q80).';
DECLARE @enunciado nvarchar(max) = N'Banco demonstrativo de Matemática do EDUCRIA (questões 1 a 160).';
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

    IF NOT EXISTS (
        SELECT 1 FROM dbo.atividades
        WHERE id = @id_demo
          AND CONVERT(varbinary(max), titulo) = CONVERT(varbinary(max), @titulo)
          AND CONVERT(varbinary(max), enunciado) IN (
              CONVERT(varbinary(max), @enunciado_legado), CONVERT(varbinary(max), @enunciado)
          )
    )
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
    (71, N'várias etapas', 1, N'Duas pizzas são cortadas em 8 fatias cada. Cinco pessoas comem 3 fatias cada uma. Quantas sobram?', N'1', N'2', N'3', N'4', N'A', N'São 16 fatias e 15 foram comidas.'),
    (72, N'várias etapas', 1, N'Uma caixa tem 5 pacotes de 8 barras, divididos entre 10 crianças. Quantas cada uma recebe?', N'3', N'4', N'5', N'8', N'B', N'São 40 barras para 10 crianças.'),
    (73, N'várias etapas', 2, N'Uma turma tem 40 alunos e 60% são meninas. Quantos são meninos?', N'14', N'16', N'20', N'24', N'B', N'São 24 meninas, sobram 16.'),
    (74, N'várias etapas', 2, N'Você compra 4 camisas de 38 reais e paga com 200. Qual o troco?', N'42', N'48', N'52', N'58', N'B', N'O total é 152.'),
    (75, N'várias etapas', 3, N'Três cadernos custam 12 reais cada. Com 25% de desconto no total, quanto se paga?', N'27', N'30', N'33', N'36', N'A', N'O total é 36 e o desconto tira 9.'),
    (76, N'várias etapas', 3, N'Um ônibus já percorreu 3/5 de uma viagem de 250 km. Quantos km faltam?', N'50', N'100', N'150', N'200', N'B', N'Faltam 2/5 de 250.'),
    (77, N'várias etapas', 4, N'Uma loja vende 12 peças a 45 reais e dá 15% de desconto no total. Quanto recebe?', N'459', N'486', N'510', N'540', N'A', N'O total é 540 e o desconto tira 81.'),
    (78, N'várias etapas', 4, N'Um reservatório de 800 litros perde 25% pela manhã e 30% do resto à tarde. Quanto sobra?', N'400', N'420', N'450', N'480', N'B', N'Sobram 600 pela manhã e 30% disso são 180.'),
    (79, N'várias etapas', 5, N'Uma caixa com 24 lápis tem 1/3 doado e o resto dividido entre 4 alunos. Quantos cada um recebe?', N'3', N'4', N'5', N'6', N'B', N'Foram doados 8 e sobraram 16 para 4 alunos.'),
    (80, N'várias etapas', 5, N'Numa viagem de 480 km, 1/4 é feito de manhã e 40% do restante à tarde. Quantos km faltam?', N'180', N'216', N'240', N'260', N'B', N'Manhã 120, resta 360, tarde 144, faltam 216.');

    -- Ampliação nas dificuldades 6–10, com dois exercícios por assunto e nível.
    INSERT INTO @itens VALUES
    (81, N'somar e subtrair', 6, N'A biblioteca tinha 248 livros. Recebeu mais 87 e enviou 129 para outras escolas. Quantos livros ficaram?', N'206', N'216', N'294', N'335', N'A', N'O acervo chegou a 335 livros. Depois da saída de 129, ficaram 206.'),
    (82, N'somar e subtrair', 6, N'O elevador estava no 18º andar. Desceu 7 andares e depois subiu 12. Em que andar parou?', N'13', N'21', N'23', N'37', N'C', N'A primeira parada foi no 11º andar. Subindo mais 12, ele chegou ao 23º.'),
    (83, N'somar e subtrair', 7, N'Ao amanhecer, a temperatura era de −7 graus. À tarde, chegou a 6 graus. De quantos graus foi o aumento?', N'1', N'7', N'12', N'13', N'D', N'Do valor −7 até zero são 7 graus. De zero até 6 são mais 6 graus, totalizando 13.'),
    (84, N'somar e subtrair', 7, N'Um almoxarifado começou com 320 caixas. Entregou 74 pela manhã, recebeu 58 e entregou outras 39 à tarde. Quantas caixas ficaram?', N'246', N'265', N'285', N'304', N'B', N'320 − 74 = 246. Depois, 246 + 58 = 304 e 304 − 39 = 265.'),
    (85, N'somar e subtrair', 8, N'Duas equipes começaram com 31 e 42 pontos. A primeira perdeu 7 e ganhou 16. A segunda perdeu 18 e ganhou 13. Por quantos pontos a primeira terminou à frente?', N'3', N'5', N'7', N'9', N'A', N'A primeira terminou com 40 pontos e a segunda com 37. A diferença foi de 3 pontos.'),
    (86, N'somar e subtrair', 8, N'Pela manhã, um sensor marcava −3 graus. A temperatura subiu 12 graus, caiu 5 e voltou a subir 2. Qual foi a última leitura, em graus?', N'3', N'5', N'6', N'10', N'C', N'A sequência de temperaturas foi −3, 9, 4 e 6 graus.'),
    (87, N'somar e subtrair', 9, N'Uma jogadora terminou com 20 pontos. Outra começou com −34, ganhou 95 e perdeu 28 em uma rodada e 41 em outra. Por quantos pontos a primeira terminou à frente?', N'8', N'12', N'20', N'28', N'D', N'A segunda terminou com −34 + 95 − 28 − 41 = −8. A primeira ficou 20 − (−8) = 28 pontos à frente.'),
    (88, N'somar e subtrair', 9, N'Há 450 vagas para uma feira. Das 297 reservas da manhã e 63 da tarde, 18 foram canceladas. Quantas vagas ainda podem ser reservadas?', N'90', N'108', N'126', N'144', N'B', N'Foram confirmadas 297 + 63 − 18 = 342 reservas. Logo, 450 − 342 = 108 vagas ficaram livres.'),
    (89, N'somar e subtrair', 10, N'Uma conta de créditos tinha saldo 85. Recebeu 47, pagou 96, recebeu 18 e pagou 35. Qual foi o saldo final?', N'1', N'17', N'19', N'36', N'C', N'Os saldos após cada movimento foram 132, 36, 54 e 19.'),
    (90, N'somar e subtrair', 10, N'Três salas doaram 156, 123 e 98 livros. Na triagem, 27 foram devolvidos e outros 14 seguiram direto para restauração. Quantos foram aceitos para empréstimo imediato?', N'319', N'350', N'377', N'336', N'D', N'Chegaram 377 livros. Entre devolvidos e em restauração, saíram 41, restando 336 para empréstimo.'),
    (91, N'multiplicar e dividir', 6, N'Um ginásio tem 18 fileiras com 14 cadeiras em cada uma. Quatro cadeiras de cada fileira estão reservadas. Quantas ficam disponíveis ao público geral?', N'180', N'162', N'216', N'252', N'A', N'Ficam livres 14 − 4 = 10 cadeiras por fileira. Nas 18 fileiras, são 180 cadeiras.'),
    (92, N'multiplicar e dividir', 6, N'Uma serigrafia fez 275 adesivos pela manhã e mais 23 à tarde. Quantos envelopes completos de 24 adesivos ela pode montar?', N'10', N'11', N'12', N'13', N'C', N'São 298 adesivos. Doze envelopes usam 288, sobrando 10 adesivos.'),
    (93, N'multiplicar e dividir', 7, N'Uma feira distribuiu 420 pulseiras entre seis grupos de cinco monitores, com a mesma quantidade para cada monitor. Mantendo essa cota, quantas pulseiras são necessárias para oito monitores?', N'70', N'105', N'120', N'112', N'D', N'São 30 monitores na primeira distribuição. Cada um recebeu 14 pulseiras, então oito precisarão de 112.'),
    (94, N'multiplicar e dividir', 7, N'Dezoito bandejas traziam 24 mudas cada. A equipe plantou 72 e repartiu as demais igualmente entre 15 hortas. Quantas mudas cada horta recebeu?', N'23', N'24', N'25', N'28', N'B', N'As bandejas trouxeram 432 mudas. Após o plantio, sobraram 360 e a divisão por 15 resulta em 24.'),
    (95, N'multiplicar e dividir', 8, N'Uma marcenaria tem 250 peças. Na inspeção, separou 16 com defeito. Quantos conjuntos de 18 peças boas consegue montar?', N'12', N'14', N'15', N'13', N'D', N'Sobram 234 peças boas. Como 234 ÷ 18 = 13, podem ser montados 13 conjuntos.'),
    (96, N'multiplicar e dividir', 8, N'Em uma oficina, 16 rolos de fio de 25 metros são unidos sem perda para cortar trechos de 8 metros. Outro setor entrega mais 21 trechos já cortados. Quantos pacotes completos de 22 trechos serão montados?', N'3', N'2', N'4', N'8', N'A', N'Os 400 metros produzem 50 trechos. Com mais 21, há 71 trechos para montar 3 pacotes completos. Sobram 5.'),
    (97, N'multiplicar e dividir', 9, N'Um centro tem 268 itens para montar 3 caixas grandes de 48 itens cada. Depois disso chegam mais 31 itens, e o que sobrar será guardado em caixas pequenas de 32. Quantas caixas completas serão montadas ao todo?', N'4', N'6', N'7', N'8', N'C', N'As grandes usam 144 itens. Restam 268 − 144 + 31 = 155, que enchem 4 caixas pequenas e deixam 27 itens fora. Ao todo são 7 caixas.'),
    (98, N'multiplicar e dividir', 9, N'Cada um dos três grupos de uma gincana recebeu 120 fichas e usou 8 para inscrições. Após juntar as fichas restantes, quantos envelopes de 14 fichas podem ser completados?', N'8', N'24', N'25', N'26', N'B', N'Cada grupo tem 112 fichas restantes. Reunidas, as 336 fichas enchem 336 ÷ 14 = 24 envelopes.'),
    (99, N'multiplicar e dividir', 10, N'Uma exposição já tem 5 painéis completos de 12 cartões. Separadamente, há 210 cartões novos. Cinco oficinas retiram 18 cartões cada, e chegam mais 11 cartões. Quantos painéis completos a exposição poderá exibir ao todo?', N'10', N'14', N'16', N'15', N'D', N'Para os painéis novos, restam 210 − 90 + 11 = 131 cartões. Eles completam 10 painéis, além dos 5 prontos. Ao todo são 15.'),
    (100, N'multiplicar e dividir', 10, N'Cada uma de duas bibliotecas recebeu 15 caixas de 24 livros. A primeira enviou 9 caixas a outras escolas. A segunda dividiu seu lote por igual entre 10 turmas. Somando os livros que ficaram na primeira aos entregues a 6 dessas turmas, quantos livros são?', N'324', N'360', N'384', N'504', N'B', N'A primeira ficou com 6 caixas, ou 144 livros. Da segunda, seis turmas receberam 6 × 36 = 216 livros. A soma é 360.'),
    (101, N'frações', 6, N'Uma rádio separou 5/8 dos seus 240 minutos de programação para música. Quantos minutos foram separados?', N'90', N'120', N'150', N'192', N'C', N'Um oitavo de 240 minutos são 30 minutos. Cinco oitavos são 150 minutos.'),
    (102, N'frações', 6, N'Uma instituição repartiu um lote de 144 pares de meias. Reservou 1/6 do lote para um abrigo e 1/4 do lote para outro. Quantos pares os dois abrigos receberão juntos?', N'24', N'36', N'48', N'60', N'D', N'O primeiro receberá 144 ÷ 6 = 24 pares e o segundo, 144 ÷ 4 = 36. Juntos receberão 60.'),
    (103, N'frações', 7, N'Dos 180 ingressos de uma sessão, 2/3 foram reservados. Até a véspera, os clientes tinham retirado 3/4 dos ingressos reservados. Quantos ingressos foram retirados?', N'90', N'105', N'120', N'135', N'A', N'Foram reservados 120 ingressos. Três quartos de 120 são 90 ingressos retirados.'),
    (104, N'frações', 7, N'De 96 mudas, 5/8 são de plantas medicinais. Dois terços dessas mudas medicinais serão entregues a uma escola. Quantas mudas a escola receberá?', N'36', N'40', N'48', N'60', N'B', N'Cinco oitavos de 96 são 60 mudas. Dois terços de 60 são 40.'),
    (105, N'frações', 8, N'Um reservatório perdeu 2/5 da água original no primeiro dia e 1/6 da água original no segundo. Restaram 78 litros. Quantos litros havia no início?', N'130', N'156', N'180', N'195', N'C', N'As perdas somam 12/30 + 5/30 = 17/30. Os 13/30 restantes valem 78 litros. Cada trigésimo vale 6 litros, então havia 180.'),
    (106, N'frações', 8, N'Uma faixa requer 5 metros e 1/4 de metro de tecido. A escola fará 24 faixas iguais. Quantos metros de tecido serão necessários?', N'114', N'120', N'132', N'126', N'D', N'Cada faixa exige 21 quartos de metro. Para 24 faixas, são 504 quartos, ou 126 metros.'),
    (107, N'frações', 9, N'Duas turmas receberam 120 fichas cada uma. A primeira registrou 5/8 delas. A segunda revisou 3/4 de suas fichas e registrou 4/5 das que revisou. Quantas fichas as duas turmas registraram juntas?', N'144', N'147', N'150', N'165', N'B', N'A primeira turma registrou 75. A segunda revisou 90 e registrou 72. A soma é 147 fichas.'),
    (108, N'frações', 9, N'Um ciclista parou depois de percorrer 18 km, que correspondiam a 3/20 do circuito. Quantos quilômetros tem o circuito inteiro?', N'60', N'90', N'120', N'140', N'C', N'Se 3/20 do circuito são 18 km, 1/20 são 6 km. As vinte partes totalizam 120 km.'),
    (109, N'frações', 10, N'De uma coleção de gravuras, 1/3 foi guardado no arquivo. Um terço das gravuras restantes foi exposto, totalizando 40. Quantas gravuras havia na coleção completa?', N'90', N'120', N'160', N'180', N'D', N'Após o arquivamento restaram 2/3 do total. Um terço desse restante é 2/9, que valem 40 gravuras. Assim, 1/9 vale 20 e a coleção tem 180.'),
    (110, N'frações', 10, N'Um rolo de fita tem 35 metros. Um quinto do rolo foi reservado. Da parte que sobrou, 3/4 serão usados para enfeitar o palco. Quantos metros serão usados no palco?', N'21', N'24', N'28', N'30', N'A', N'Foram reservados 7 metros, restando 28. Três quartos de 28 são 21 metros.'),
    (111, N'porcentagem', 6, N'Um ingresso custa 240 reais e tem 15% de desconto antecipado. Qual é o preço antecipado, em reais?', N'168', N'180', N'204', N'216', N'C', N'O desconto é de 36 reais. O novo preço é 240 − 36 = 204 reais.'),
    (112, N'porcentagem', 6, N'Dos 180 participantes de uma oficina, 35% solicitaram material impresso. Os demais escolheram a versão digital. Quantos escolheram a digital?', N'63', N'90', N'108', N'117', N'D', N'A versão digital foi escolhida por 65% dos participantes. Isso corresponde a 117 pessoas.'),
    (113, N'porcentagem', 7, N'Um formulário recebeu 144 respostas, correspondentes a 80% do público convidado. Quantas pessoas foram convidadas?', N'160', N'180', N'200', N'224', N'B', N'Como 80% correspondem a 144, cada 10% correspondem a 18 pessoas. O total é de 180.'),
    (114, N'porcentagem', 7, N'O número de participantes de um clube cresceu de 25 para 35. Qual foi o percentual de crescimento em relação ao número inicial?', N'40', N'29', N'35', N'60', N'A', N'O aumento foi de 10 pessoas. Em relação às 25 iniciais, 10 ÷ 25 = 0,4, isto é, 40%.'),
    (115, N'porcentagem', 8, N'Uma cooperativa oferece dois descontos sucessivos sobre uma compra de 500 reais. O primeiro é de 20%, e o segundo é de 15% sobre o preço reduzido. Quanto se paga, em reais?', N'325', N'335', N'340', N'350', N'C', N'O primeiro desconto deixa o preço em 400 reais. O segundo tira 60 reais desse valor, resultando em 340.'),
    (116, N'porcentagem', 8, N'Um aparelho custava 320 reais. O preço subiu 25% e, depois, caiu 10% sobre o novo valor. Qual foi o aumento em reais em relação ao preço inicial?', N'32', N'40', N'48', N'80', N'B', N'O aumento levou o preço a 400 reais. Com o desconto de 40 reais, ficou em 360. A diferença para os 320 iniciais é de 40 reais.'),
    (117, N'porcentagem', 9, N'Uma feira colocou 250 ingressos à venda e vendeu 60% deles. Das vendas, 30% foram pela internet. Quantos ingressos foram vendidos pela internet?', N'75', N'105', N'150', N'45', N'D', N'Foram vendidos 150 ingressos. Trinta por cento de 150 são 45 ingressos vendidos pela internet.'),
    (118, N'porcentagem', 9, N'Em uma escola, 30% dos 120 alunos do turno da manhã e metade dos 30 alunos do turno da tarde participam de esportes. Qual percentual dos 150 alunos pratica esportes?', N'30', N'32', N'34', N'40', N'C', N'Participam 36 alunos da manhã e 15 da tarde. As 51 participações correspondem a 34% de 150 alunos.'),
    (119, N'porcentagem', 10, N'Na primeira triagem foram aprovadas 80% das peças fabricadas. Na segunda triagem foram aprovadas 90% das peças que passaram na primeira. Se 360 peças passaram pelas duas triagens, quantas peças foram fabricadas?', N'450', N'480', N'500', N'540', N'C', N'As duas etapas aprovaram 0,8 × 0,9 = 0,72 do lote. Logo, 360 ÷ 0,72 = 500 peças.'),
    (120, N'porcentagem', 10, N'Das 200 primeiras avaliações de um evento, 55% foram positivas. As 20 seguintes foram todas positivas. Se as próximas também forem positivas, quantas novas avaliações são necessárias, no mínimo, para atingir pelo menos 65% de avaliações positivas no total?', N'37', N'38', N'43', N'65', N'B', N'Há 130 avaliações positivas em 220. Com x novas positivas, 130 + x deve ser pelo menos 0,65 × (220 + x). Logo, 0,35x deve ser pelo menos 13, e o menor inteiro possível é 38.'),
    (121, N'proporção', 6, N'Uma torneira enche um balde com 35 litros em 5 minutos. Mantendo o mesmo fluxo, quantos litros a torneira fornece em 3 minutos?', N'15', N'20', N'21', N'25', N'C', N'A vazão é de 35 ÷ 5 = 7 litros por minuto. Em 3 minutos, passam 21 litros.'),
    (122, N'proporção', 6, N'Um painel usou 12 azulejos para montar 3 desenhos iguais. Outro painel terá 6 desses desenhos na primeira faixa e 4 na segunda. Quantos azulejos serão usados no outro painel?', N'30', N'36', N'44', N'40', N'D', N'Cada desenho ocupa 4 azulejos. As duas faixas somam 10 desenhos, que usam 40 azulejos.'),
    (123, N'proporção', 7, N'Uma receita para 8 pessoas usa 48 morangos. Para manter a proporção, uma cozinheira adaptou a receita para 5 pessoas. Se ela já tem 6 morangos, quantos ainda precisa comprar?', N'24', N'25', N'30', N'36', N'A', N'São 6 morangos por pessoa, ou 30 para 5 pessoas. Como ela já tem 6, precisa comprar 24.'),
    (124, N'proporção', 7, N'Em 8 minutos, um equipamento processa 600 páginas. Há um pedido de 18 minutos de produção na mesma velocidade. Se 450 páginas desse pedido já foram processadas, quantas ainda faltam?', N'750', N'900', N'1000', N'1350', N'B', N'O equipamento processa 75 páginas por minuto. O pedido é de 75 × 18 = 1350 páginas, e faltam 900.'),
    (125, N'proporção', 8, N'Em um mapa na mesma escala, um trajeto de 12 km é desenhado com 8 cm. Outro trajeto mede 15 km. Qual é o comprimento do seu desenho, em centímetros?', N'9', N'10', N'11', N'12', N'B', N'A razão é de 8 cm para 12 km, ou 2/3 cm por quilômetro. Em 15 km, isso dá 10 cm.'),
    (126, N'proporção', 8, N'Doze máquinas iguais embalam 156 produtos em uma hora. Em outro turno, funcionam 20 máquinas no mesmo ritmo. Se 65 produtos da primeira hora desse novo turno forem separados para inspeção, quantos produtos dessa hora estarão disponíveis para envio?', N'130', N'175', N'195', N'260', N'C', N'Cada máquina embala 13 produtos em uma hora. As 20 máquinas embalam 260. Separadas 65, sobram 195 para envio.'),
    (127, N'proporção', 9, N'Quatro bombas idênticas retiram 600 litros em 5 horas. Trabalhando no mesmo ritmo, quantas horas duas delas levarão para retirar 900 litros?', N'12', N'20', N'30', N'15', N'D', N'Cada bomba retira 600 ÷ (4 × 5) = 30 litros por hora. Duas retiram 60 por hora, então 900 litros levam 15 horas.'),
    (128, N'proporção', 9, N'Um caminho com dois trechos retos mede 8 cm e 4 cm em uma planta na escala 1:250. Qual é o comprimento real do caminho, em metros?', N'30', N'25', N'32', N'48', N'A', N'A soma no desenho é 12 cm. Na escala, isso representa 3000 cm, equivalentes a 30 metros.'),
    (129, N'proporção', 10, N'Seis trabalhadores, todos com o mesmo ritmo, concluiriam um serviço em 10 horas. Depois de 4 horas com a equipe completa, três saíram. Quantas horas, desde o início, o serviço levou?', N'12', N'14', N'16', N'20', N'C', N'O serviço requer 60 horas de trabalho somadas. A equipe já fez 24, e os 3 restantes precisam de 12 horas para completar as 36 restantes. O tempo total é 16 horas.'),
    (130, N'proporção', 10, N'Um ateliê produz 36 peças com 6 sacos de argila e doa 1/6 das peças. Na semana seguinte, usa só 5 sacos, com a mesma proporção, e doa 1/3 da nova produção. Quantas peças a mais são doadas na segunda semana?', N'3', N'4', N'6', N'10', N'B', N'Na primeira semana, foram doadas 6 peças. Na segunda, 5 sacos renderam 30 peças, das quais 10 foram doadas. A diferença foi de 4 peças.'),
    (131, N'equações', 6, N'Dois marcadores iguais, junto com uma borracha de 6 reais, custaram 34 reais. Quanto custa um marcador, em reais?', N'14', N'17', N'20', N'28', N'A', N'Se x é o preço do marcador, 2x + 6 = 34. Então 2x = 28 e cada marcador custa 14 reais.'),
    (132, N'equações', 6, N'Qual valor de x torna 3x − 7 igual a 2x + 8?', N'8', N'12', N'15', N'21', N'C', N'Subtraindo 2x dos dois lados, fica x − 7 = 8. Assim, x = 15.'),
    (133, N'equações', 7, N'Qual valor de x satisfaz a equação 2(x − 5) + 16 = 34?', N'9', N'10', N'12', N'14', N'D', N'Subtraindo 16 dos dois lados, temos 2(x − 5) = 18. Logo, x − 5 = 9 e x = 14.'),
    (134, N'equações', 7, N'Quatro sacos com x − 3 sementes cada têm o mesmo total de sementes que dois sacos com x + 5 cada. Qual é o valor de x?', N'8', N'11', N'13', N'16', N'B', N'A igualdade é 4(x − 3) = 2(x + 5). Expandindo, 4x − 12 = 2x + 10. Portanto, 2x = 22 e x = 11.'),
    (135, N'equações', 8, N'Um estacionamento cobra 5 reais de entrada mais 5 reais por hora. Outro cobra 15 reais de entrada mais 3 reais por hora. Após quantas horas as cobranças são iguais?', N'5', N'7', N'10', N'15', N'A', N'Igualando os valores, 5 + 5x = 15 + 3x. Assim, 2x = 10 e os valores coincidem em 5 horas.'),
    (136, N'equações', 8, N'Um laboratório registrou o mesmo total em dois relatórios. No primeiro, eram 8 lotes de 6 amostras. No segundo, eram 2 lotes de x − 4 amostras e 3 de x + 2 amostras. Qual é o valor de x?', N'6', N'8', N'10', N'12', N'C', N'O total é 48. Assim, 2(x − 4) + 3(x + 2) = 48, ou 5x − 2 = 48. Portanto, x = 10.'),
    (137, N'equações', 9, N'Uma mochila recebeu 20% de desconto sobre o preço original. Com mais 12 reais de entrega, o pedido custou 84 reais. Qual era o preço original da mochila, em reais?', N'72', N'84', N'96', N'90', N'D', N'Se p é o preço original, 0,8p + 12 = 84. Logo, 0,8p = 72 e p = 90 reais.'),
    (138, N'equações', 9, N'Quatro equipes marcaram, respectivamente, x, x + 1, x + 2 e x + 3 pontos. Se a soma foi 86, quantos pontos marcou a primeira equipe?', N'18', N'20', N'21', N'22', N'B', N'Somando, 4x + 6 = 86. Assim, 4x = 80 e a primeira equipe marcou 20 pontos.'),
    (139, N'equações', 10, N'Em um jogo, o placar soma 8 pontos aos que a equipe conquistou, triplica o resultado e depois desconta 4 por atraso. Se o placar final foi 35, quantos pontos a equipe conquistou antes desses ajustes?', N'5', N'8', N'9', N'13', N'A', N'Se x é a pontuação inicial, 3(x + 8) − 4 = 35. Somando 4 e dividindo por 3, chega-se a x + 8 = 13. Logo, x = 5.'),
    (140, N'equações', 10, N'Lia usou metade do dinheiro em livros e um terço do valor inicial em materiais. Depois, guardou toda a sobra, que era de 18 reais. Quantos reais Lia tinha antes das compras?', N'90', N'96', N'108', N'120', N'C', N'Os gastos somam 1/2 + 1/3 = 5/6 do valor inicial. A sobra de 18 reais vale 1/6, então o valor inicial era 108 reais.'),
    (141, N'área e perímetro', 6, N'De um cartão retangular de 14 cm por 9 cm foi recortado um quadrado de 4 cm de lado. Qual é a área do cartão após o recorte, em centímetros quadrados?', N'96', N'106', N'126', N'110', N'D', N'A área original era 126 cm². O quadrado retirou 16 cm², restando 110 cm².'),
    (142, N'área e perímetro', 6, N'A largura de um cartaz retangular é 5 cm e o comprimento mede o dobro da largura. Qual é o perímetro do cartaz, em centímetros?', N'20', N'25', N'30', N'50', N'C', N'O comprimento é de 10 cm. Os dois lados de 10 e os dois de 5 somam 30 cm.'),
    (143, N'área e perímetro', 7, N'Uma moldura ocupa uma faixa interna de 2 cm de largura em cada lado de uma placa de 16 cm por 10 cm. Qual é a área da parte central, em centímetros quadrados?', N'66', N'72', N'84', N'96', N'B', N'As dimensões da parte central são 16 − 4 = 12 cm e 10 − 4 = 6 cm. A área é 72 cm².'),
    (144, N'área e perímetro', 7, N'De um retalho triangular com base de 18 cm e altura de 12 cm, foi retirado um quadrado de 6 cm de lado totalmente contido no retalho. Qual é a área que restou, em centímetros quadrados?', N'72', N'84', N'108', N'180', N'A', N'A área inicial é 18 × 12 ÷ 2 = 108 cm². Tirando os 36 cm² do quadrado, restam 72 cm².'),
    (145, N'área e perímetro', 8, N'Uma placa de 18 cm por 12 cm teve um quadrado de 4 cm de lado retirado de um dos cantos. Qual é o comprimento do contorno da nova peça, em centímetros?', N'52', N'60', N'68', N'76', N'B', N'O perímetro inicial é 60 cm. Saem dois trechos externos de 4 cm e entram dois novos trechos de 4 cm no recorte. O total permanece 60 cm.'),
    (146, N'área e perímetro', 8, N'Em um piso retangular de 16 m por 12 m, uma área retangular de 12 m por 8 m já foi pintada. Quantos metros quadrados faltam pintar?', N'88', N'100', N'192', N'96', N'D', N'O piso tem 192 m². Como 96 m² já foram pintados, faltam outros 96 m².'),
    (147, N'área e perímetro', 9, N'O perímetro de um terreno retangular é 52 m. Seu comprimento é 6 m maior que a largura. Qual é a área, em metros quadrados?', N'130', N'156', N'160', N'169', N'C', N'Comprimento mais largura somam 26 m. Com diferença de 6 m, medem 16 e 10 m. A área é 160 m².'),
    (148, N'área e perímetro', 9, N'Um salão mede 900 cm de comprimento e 700 cm de largura. Será coberto com placas quadradas de lado 50 cm, sem cortes nem espaços. Quantas placas são necessárias?', N'252', N'224', N'246', N'270', N'A', N'Há 18 placas em uma direção e 14 na outra. A malha completa contém 18 × 14 = 252 placas.'),
    (149, N'área e perímetro', 10, N'Um jardim retangular de 18 m por 14 m receberá um caminho interno de 2 m de largura ao longo de todos os lados. Quantos metros quadrados serão ocupados pelo caminho?', N'104', N'112', N'120', N'140', N'B', N'O jardim inteiro tem 252 m². Sem o caminho, o retângulo interno mede 14 m por 10 m e tem 140 m². O caminho ocupa 112 m².'),
    (150, N'área e perímetro', 10, N'Um pátio retangular de 216 m² tem um lado de 18 m encostado a uma parede que já fecha esse lado. Os outros três lados serão cercados, com uma abertura de 3 m para o portão. Quantos metros de cerca serão usados?', N'36', N'42', N'57', N'39', N'D', N'A largura é 216 ÷ 18 = 12 m. Os três lados a cercar somam 42 m. Descontando o portão, são 39 m de cerca.'),
    (151, N'várias etapas', 6, N'Um grupo trouxe 8 caixas de 12 gizes de cera cada. Depois de usar 16 gizes, guardará os restantes em estojos de 10 gizes. Quantos estojos completos serão feitos?', N'7', N'8', N'9', N'10', N'B', N'Havia 96 gizes. Após o uso, sobraram 80 e cada estojo recebe 10. São 8 estojos completos.'),
    (152, N'várias etapas', 6, N'Em uma partida, cada um dos 18 participantes recebeu 12 fichas. Durante o jogo, 72 fichas foram retiradas de circulação. Ao final, as demais foram recolhidas em conjuntos de 12. Quantos conjuntos foram recolhidos?', N'10', N'12', N'14', N'18', N'B', N'Ao todo foram distribuídas 216 fichas. Restaram 144 em circulação, suficientes para 144 ÷ 12 = 12 conjuntos.'),
    (153, N'várias etapas', 7, N'Um tanque contém 750 litros. A equipe vai transferir 80% dessa água, repartindo-a igualmente entre 3 recipientes. Quantos litros vão para cada recipiente?', N'150', N'180', N'200', N'250', N'C', N'O volume transferido é de 600 litros. Em três partes iguais, são 200 litros por recipiente.'),
    (154, N'várias etapas', 7, N'De 240 livros recebidos, 3/4 serão enviados em caixas com 12 livros cada. Quatro caixas irão ao posto A e as demais ao posto B. Quantas caixas irão ao posto B?', N'8', N'12', N'15', N'11', N'D', N'Serão enviados 180 livros, que ocupam 15 caixas. Depois de separar 4 para o posto A, restam 11 para o posto B.'),
    (155, N'várias etapas', 8, N'A comissão de uma festa tem 480 reais. Comprará 6 jogos de 52 reais cada, com 25% de desconto nesses jogos, e pagará 72 reais pelo transporte. Quanto sobrará, em reais?', N'174', N'168', N'210', N'246', N'A', N'Sem desconto, os jogos custam 312 reais. Com desconto, custam 234. Somando o transporte, gasta-se 306, e sobram 174 reais.'),
    (156, N'várias etapas', 8, N'Uma cooperativa testou 18 bandejas de 20 mudas cada. Aprovou 3/5 das mudas e dividiu as aprovadas igualmente entre 9 escolas. Quantas mudas cada escola recebeu?', N'18', N'20', N'24', N'40', N'C', N'O total testado foi 360 mudas. Foram aprovadas 216. Dividindo por 9, são 24 mudas para cada escola.'),
    (157, N'várias etapas', 9, N'Uma encomenda tem 10 pacotes a 24 reais cada e 15 pacotes a 12 reais cada. Um cupom desconta 10% do preço dos pacotes e o frete custa 42 reais. Qual é o valor final, em reais?', N'378', N'400', N'420', N'462', N'C', N'Os pacotes custam 240 + 180 = 420 reais antes do desconto. Após o cupom, ficam por 378. Com o frete, são 420 reais.'),
    (158, N'várias etapas', 9, N'Uma oficina catalogou 12 caixas com 9 figurinhas cada. Dois terços dessas figurinhas serão agrupados em conjuntos de 6 para vender por 17 reais cada conjunto. Quanto renderá a venda de todos esses conjuntos, em reais?', N'153', N'187', N'221', N'204', N'D', N'São 108 figurinhas, das quais 72 serão vendidas. Elas formam 12 conjuntos a 17 reais, totalizando 204 reais.'),
    (159, N'várias etapas', 10, N'Uma escola recebeu 600 mudas e reservou 1/5 para doação. Das restantes, 25% vão para o pátio. O que sobrar será dividido igualmente entre 12 canteiros da praça. Quantas mudas irão para cada canteiro?', N'25', N'30', N'36', N'40', N'B', N'Após a reserva, restam 480 mudas. O pátio recebe 120 e as 360 restantes são divididas entre 12 canteiros, com 30 por canteiro.'),
    (160, N'várias etapas', 10, N'Uma feira monta kits de 12 peças com um lote de 96 peças. Cada kit tem preço de 42 reais, e todos serão vendidos com 10% de desconto. Qual será a receita total arredondada ao real mais próximo?', N'302', N'303', N'336', N'378', N'A', N'O lote forma 8 kits. O total sem desconto é 336 reais, e com 10% de desconto fica em 302,40 reais. O real mais próximo é 302.');

    DECLARE @materia bigint = (SELECT id FROM dbo.materias WHERE nome = N'Matemática');
    IF EXISTS (
        SELECT 1 FROM @itens i
        WHERE NOT EXISTS (SELECT 1 FROM dbo.assuntos s WHERE s.materia_id = @materia AND s.nome = i.assunto)
    )
        THROW 51041, N'O catálogo de Matemática precisa conter os assuntos da demonstração.', 1;

    -- Compara também os campos intactos para preservar questões editadas ou desativadas.
    DECLARE @enunciado_71_legado nvarchar(max) =
        N'Duas pizzas são cortadas em 8 fatias cada. Cinco pessoas comem 3 fatias. Quantas sobram?';

    UPDATE q
    SET alternativa_c = i.c, enunciado = i.enunciado
    FROM dbo.questoes q
    JOIN @itens i ON i.ordem = q.ordem
    JOIN dbo.assuntos s ON s.id = q.assunto_id AND s.materia_id = @materia AND s.nome = i.assunto
    WHERE q.atividade_id = @id_demo AND q.ordem IN (26, 71)
      AND q.autor_id IS NULL AND q.ativa = 1 AND q.dificuldade = i.dificuldade
      AND CONVERT(varbinary(max), q.enunciado) =
          CONVERT(varbinary(max), CASE WHEN i.ordem = 71 THEN @enunciado_71_legado ELSE i.enunciado END)
      AND CONVERT(varbinary(max), q.alternativa_a) = CONVERT(varbinary(max), i.a)
      AND CONVERT(varbinary(max), q.alternativa_b) = CONVERT(varbinary(max), i.b)
      AND CONVERT(varbinary(max), q.alternativa_c) =
          CONVERT(varbinary(max), CASE WHEN i.ordem = 26 THEN N'1/3' ELSE i.c END)
      AND CONVERT(varbinary(max), q.alternativa_d) = CONVERT(varbinary(max), i.d)
      AND CONVERT(varbinary(max), q.resposta_correta) = CONVERT(varbinary(max), i.correta)
      AND CONVERT(varbinary(max), q.explicacao) = CONVERT(varbinary(max), i.explicacao);

    INSERT INTO dbo.questoes (atividade_id, ordem, assunto_id, dificuldade, enunciado,
                             alternativa_a, alternativa_b, alternativa_c, alternativa_d, resposta_correta, explicacao)
    SELECT @id_demo, i.ordem, s.id, i.dificuldade, i.enunciado, i.a, i.b, i.c, i.d, i.correta, i.explicacao
    FROM @itens i
    JOIN dbo.assuntos s ON s.materia_id = @materia AND s.nome = i.assunto
    WHERE NOT EXISTS (SELECT 1 FROM dbo.questoes q WHERE q.atividade_id = @id_demo AND q.ordem = i.ordem);

    UPDATE dbo.atividades
    SET enunciado = @enunciado
    WHERE id = @id_demo AND CONVERT(varbinary(max), titulo) = CONVERT(varbinary(max), @titulo)
      AND CONVERT(varbinary(max), enunciado) = CONVERT(varbinary(max), @enunciado_legado);

    IF @transacao_propria = 1 COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @transacao_propria = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
