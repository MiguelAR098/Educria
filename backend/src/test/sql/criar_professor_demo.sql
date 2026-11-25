-- Etapa 1 de scripts/sqlserver/carregar-demo.sh. Login: professor@educria.com / professor123
IF XACT_STATE() <> 1 OR OBJECT_ID(N'tempdb..#demo_turma', N'U') IS NULL
    THROW 51010, N'Execute a carga pelo script scripts/sqlserver/carregar-demo.sh.', 1;

DECLARE @email nvarchar(100) = N'professor@educria.com';
DECLARE @nome nvarchar(100) = N'Professora Demo';
DECLARE @senha nvarchar(255) = N'$2a$10$pDTii9Cwi6nFYZ2iKXnJU.lPmd7S639cak2NzsFwRs5EZDpX5i57S';
DECLARE @codigo nvarchar(20) = N'DEMO-8A';
DECLARE @nome_turma nvarchar(100) = N'8º A';
DECLARE @ano_letivo int = 2026;
DECLARE @materia_id bigint, @professor_id bigint, @turma_id bigint, @total_turmas bigint;

SELECT @materia_id = id FROM dbo.materias WITH (HOLDLOCK) WHERE nome = N'Matemática';
IF @materia_id IS NULL
    THROW 51011, N'A matéria Matemática não está disponível no catálogo.', 1;

SELECT @professor_id = id FROM dbo.usuarios WITH (UPDLOCK, HOLDLOCK) WHERE email = @email;
SELECT @turma_id = MIN(id), @total_turmas = COUNT_BIG(*)
FROM dbo.turmas WITH (UPDLOCK, HOLDLOCK)
WHERE codigo = @codigo;

IF @total_turmas > 1 OR (@turma_id IS NOT NULL AND @professor_id IS NULL)
    THROW 51012, N'Conflito com uma turma existente de código DEMO-8A.', 1;

IF @professor_id IS NULL
BEGIN
    INSERT INTO dbo.turmas (nome, codigo, ano_letivo) VALUES (@nome_turma, @codigo, @ano_letivo);
    SET @turma_id = CONVERT(bigint, SCOPE_IDENTITY());

    INSERT INTO dbo.pontuacao_coletiva (turma_id) VALUES (@turma_id);
    INSERT INTO dbo.usuarios (email, nome, senha, perfil) VALUES (@email, @nome, @senha, N'PROFESSOR');
    SET @professor_id = CONVERT(bigint, SCOPE_IDENTITY());

    INSERT INTO dbo.professores (usuario_id, materia_id) VALUES (@professor_id, @materia_id);
    INSERT INTO dbo.professor_turmas (professor_id, turma_id) VALUES (@professor_id, @turma_id);
END
ELSE
BEGIN
    -- Compara os dados exatos, inclusive em bancos com collation insensível a maiúsculas.
    IF NOT EXISTS (
        SELECT 1 FROM dbo.usuarios AS u
        JOIN dbo.professores AS p WITH (HOLDLOCK) ON p.usuario_id = u.id
        WHERE u.id = @professor_id AND u.ativo = 1 AND p.materia_id = @materia_id
          AND CONVERT(varbinary(200), u.email) = CONVERT(varbinary(200), @email)
          AND CONVERT(varbinary(200), u.nome) = CONVERT(varbinary(200), @nome)
          AND CONVERT(varbinary(510), u.senha) = CONVERT(varbinary(510), @senha)
          AND CONVERT(varbinary(40), u.perfil) = CONVERT(varbinary(40), N'PROFESSOR')
    ) OR EXISTS (SELECT 1 FROM dbo.alunos WITH (HOLDLOCK) WHERE usuario_id = @professor_id)
      OR EXISTS (SELECT 1 FROM dbo.admins WITH (HOLDLOCK) WHERE usuario_id = @professor_id)
        THROW 51013, N'Conflito com a conta existente professor@educria.com.', 1;

    IF NOT EXISTS (
        SELECT 1 FROM dbo.turmas AS t
        JOIN dbo.professor_turmas AS pt WITH (HOLDLOCK) ON pt.turma_id = t.id
        WHERE t.id = @turma_id AND pt.professor_id = @professor_id AND t.ano_letivo = @ano_letivo
          AND CONVERT(varbinary(40), t.codigo) = CONVERT(varbinary(40), @codigo)
          AND CONVERT(varbinary(200), t.nome) = CONVERT(varbinary(200), @nome_turma)
    )
        THROW 51014, N'A conta professor@educria.com não está vinculada à turma de demonstração esperada (DEMO-8A).', 1;
END;

INSERT INTO #demo_turma (id) VALUES (@turma_id);
