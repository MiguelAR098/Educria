-- Etapa 2 de scripts/sqlserver/carregar-demo.sh. Login: portugues@educria.com / professor123
IF XACT_STATE() <> 1 OR OBJECT_ID(N'tempdb..#demo_turma', N'U') IS NULL
    THROW 51020, N'Execute a carga pelo script scripts/sqlserver/carregar-demo.sh.', 1;

DECLARE @email nvarchar(100) = N'portugues@educria.com';
DECLARE @nome nvarchar(100) = N'Professor de Português';
DECLARE @senha nvarchar(255) = N'$2a$10$pDTii9Cwi6nFYZ2iKXnJU.lPmd7S639cak2NzsFwRs5EZDpX5i57S';
DECLARE @turma_id bigint = (SELECT id FROM #demo_turma);
DECLARE @materia_id bigint, @professor_id bigint;

IF @turma_id IS NULL
    THROW 51021, N'A turma de demonstração DEMO-8A ainda não foi identificada.', 1;

SELECT @materia_id = id FROM dbo.materias WITH (HOLDLOCK) WHERE nome = N'Português';
IF @materia_id IS NULL
    THROW 51022, N'A matéria Português não está disponível no catálogo.', 1;

SELECT @professor_id = id FROM dbo.usuarios WITH (UPDLOCK, HOLDLOCK) WHERE email = @email;
IF @professor_id IS NULL
BEGIN
    INSERT INTO dbo.usuarios (email, nome, senha, perfil) VALUES (@email, @nome, @senha, N'PROFESSOR');
    SET @professor_id = CONVERT(bigint, SCOPE_IDENTITY());

    INSERT INTO dbo.professores (usuario_id, materia_id) VALUES (@professor_id, @materia_id);
    INSERT INTO dbo.professor_turmas (professor_id, turma_id) VALUES (@professor_id, @turma_id);
END
ELSE
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM dbo.usuarios AS u
        JOIN dbo.professores AS p WITH (HOLDLOCK) ON p.usuario_id = u.id
        JOIN dbo.professor_turmas AS pt WITH (HOLDLOCK) ON pt.professor_id = p.usuario_id
        WHERE u.id = @professor_id AND u.ativo = 1 AND p.materia_id = @materia_id AND pt.turma_id = @turma_id
          AND CONVERT(varbinary(200), u.email) = CONVERT(varbinary(200), @email)
          AND CONVERT(varbinary(200), u.nome) = CONVERT(varbinary(200), @nome)
          AND CONVERT(varbinary(510), u.senha) = CONVERT(varbinary(510), @senha)
          AND CONVERT(varbinary(40), u.perfil) = CONVERT(varbinary(40), N'PROFESSOR')
    ) OR EXISTS (SELECT 1 FROM dbo.alunos WITH (HOLDLOCK) WHERE usuario_id = @professor_id)
      OR EXISTS (SELECT 1 FROM dbo.admins WITH (HOLDLOCK) WHERE usuario_id = @professor_id)
        THROW 51023, N'Conflito com a conta existente portugues@educria.com ou com o vínculo à turma DEMO-8A.', 1;
END;
