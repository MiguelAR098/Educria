-- Cria as quatro contas sintéticas na turma DEMO-8A. Login: e-mail / aluno123.
-- Executado por scripts/sqlserver/carregar-demo.sh, na mesma transação do restante da carga.
DECLARE @turma_id bigint = (SELECT id FROM #demo_turma);
IF @turma_id IS NULL
    THROW 51030, N'A turma de demonstração não foi identificada para a carga dos alunos.', 1;

DECLARE @senha nvarchar(255) = N'$2a$10$dLG/Mt2a83PQtiaEXxiEsuntVCSm3PAVknmQ091bQJrIY/PdHytxm';
DECLARE @agora datetime2(6) = CONVERT(datetime2(6), SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
DECLARE @alunos TABLE (
    email nvarchar(100) PRIMARY KEY,
    nome nvarchar(100) NOT NULL,
    matricula nvarchar(30) NOT NULL,
    nivel int NOT NULL,
    consecutivos int NOT NULL,
    acertos int NOT NULL,
    erros int NOT NULL,
    dias_atras int NOT NULL,
    usuario_existente bigint
);

-- Ana EM_RISCO, Bruno NO_CAMINHO, Carla ATENCAO e Diego SEM_DADOS.
INSERT INTO @alunos (email, nome, matricula, nivel, consecutivos, acertos, erros, dias_atras)
VALUES
    (N'ana.demo@educria.com',   N'Ana Lima',    N'2026001', 2, 0, 2, 8, 1),
    (N'bruno.demo@educria.com', N'Bruno Reis',  N'2026002', 7, 0, 9, 1, 0),
    (N'carla.demo@educria.com', N'Carla Souza', N'2026003', 4, 1, 5, 4, 5),
    (N'diego.demo@educria.com', N'Diego Alves', N'2026004', 1, 0, 0, 0, 0);

UPDATE demo
SET usuario_existente = u.id
FROM @alunos AS demo
LEFT JOIN dbo.usuarios AS u WITH (UPDLOCK, HOLDLOCK) ON u.email = demo.email;

-- A comparação binária distingue maiúsculas, acentos e espaços, inclusive no hash BCrypt.
DECLARE @email_conflito nvarchar(100) = (
    SELECT TOP (1) demo.email
    FROM @alunos AS demo
    JOIN dbo.usuarios AS u ON u.id = demo.usuario_existente
    LEFT JOIN dbo.alunos AS a WITH (UPDLOCK, HOLDLOCK) ON a.usuario_id = u.id
    WHERE CONVERT(varbinary(200), u.email) <> CONVERT(varbinary(200), demo.email)
       OR CONVERT(varbinary(200), u.nome) <> CONVERT(varbinary(200), demo.nome)
       OR CONVERT(varbinary(510), u.senha) <> CONVERT(varbinary(510), @senha)
       OR CONVERT(varbinary(40), u.perfil) <> CONVERT(varbinary(40), N'ALUNO')
       OR u.ativo = 0
       OR a.turma_id IS NULL OR a.turma_id <> @turma_id
       OR a.matricula IS NULL
       OR CONVERT(varbinary(60), a.matricula) <> CONVERT(varbinary(60), demo.matricula)
       OR EXISTS (SELECT 1 FROM dbo.professores WITH (UPDLOCK, HOLDLOCK) WHERE usuario_id = u.id)
       OR EXISTS (SELECT 1 FROM dbo.admins WITH (UPDLOCK, HOLDLOCK) WHERE usuario_id = u.id)
    ORDER BY demo.email
);

DECLARE @mensagem nvarchar(2048);
IF @email_conflito IS NOT NULL
BEGIN
    SET @mensagem = CONCAT(N'Conflito com a conta existente ', @email_conflito,
                           N'. Confira o cadastro, a matrícula e o vínculo com a turma de demonstração.');
    THROW 51031, @mensagem, 1;
END;

DECLARE @matricula_conflito nvarchar(30) = (
    SELECT TOP (1) demo.matricula
    FROM @alunos AS demo
    JOIN dbo.alunos AS a WITH (UPDLOCK, HOLDLOCK) ON a.matricula = demo.matricula
    WHERE demo.usuario_existente IS NULL OR a.usuario_id <> demo.usuario_existente
    ORDER BY demo.matricula
);

IF @matricula_conflito IS NOT NULL
BEGIN
    SET @mensagem = CONCAT(N'A matrícula de demonstração ', @matricula_conflito,
                           N' está vinculada a outra conta.');
    THROW 51032, @mensagem, 1;
END;

INSERT INTO #demo_alunos (email, id, novo)
SELECT email, usuario_existente, 0
FROM @alunos
WHERE usuario_existente IS NOT NULL;

INSERT INTO dbo.usuarios (email, nome, senha, perfil)
OUTPUT inserted.email, inserted.id, CONVERT(bit, 1) INTO #demo_alunos (email, id, novo)
SELECT email, nome, @senha, N'ALUNO'
FROM @alunos
WHERE usuario_existente IS NULL;

INSERT INTO dbo.alunos (usuario_id, matricula, turma_id)
SELECT carga.id, demo.matricula, @turma_id
FROM #demo_alunos AS carga
JOIN @alunos AS demo ON demo.email = carga.email
WHERE carga.novo = 1;

INSERT INTO dbo.desempenho_adaptativo
    (aluno_id, nivel_atual, erros_consecutivos, total_acertos, total_erros, atualizado_em)
SELECT carga.id, demo.nivel, demo.consecutivos, demo.acertos, demo.erros, DATEADD(day, -demo.dias_atras, @agora)
FROM #demo_alunos AS carga
JOIN @alunos AS demo ON demo.email = carga.email
WHERE carga.novo = 1;
