IF DB_NAME() NOT LIKE N'educria[_]sqlserver[_]teste[_]%'
    OR NOT EXISTS (SELECT 1 FROM sys.extended_properties
                   WHERE class=0 AND name=N'EducriaAmbiente' AND CONVERT(nvarchar(30),value)=N'descartavel')
    THROW 51050, N'A medição exige uma base descartável identificada.', 1;

DECLARE @hash nvarchar(255) = (SELECT senha FROM dbo.usuarios WHERE email = N'aluno1@educria.example.test');
IF @hash IS NULL THROW 51051, N'Execute primeiro a carga acadêmica.', 1;

SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
BEGIN TRY
    DECLARE @lote nvarchar(36) = CONVERT(nvarchar(36), NEWID());
    -- O produto cartesiano dos dígitos gera os números de 1 a 100 em um único INSERT.
    DECLARE @numeros TABLE (n int PRIMARY KEY);
    INSERT @numeros
    SELECT d.n * 10 + u.n + 1
    FROM (VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9)) d(n)
    CROSS JOIN (VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9)) u(n);

    -- OUTPUT captura as chaves geradas por IDENTITY para montar os vínculos do lote.
    DECLARE @alunos TABLE (id bigint PRIMARY KEY);
    INSERT dbo.usuarios(email,nome,senha,perfil)
    OUTPUT inserted.id INTO @alunos
    SELECT CONCAT(N'idx-',@lote,N'-',n,N'@example.test'),N'Aluno de medição',@hash,N'ALUNO' FROM @numeros;
    INSERT dbo.alunos(usuario_id) SELECT id FROM @alunos;

    INSERT dbo.atividades(titulo) VALUES (N'Atividade sintética para medir índice');
    DECLARE @atividade bigint = SCOPE_IDENTITY();
    DECLARE @questoes TABLE (id bigint PRIMARY KEY, ordem int);
    INSERT dbo.questoes(atividade_id,enunciado,alternativa_a,alternativa_b,resposta_correta,ordem)
    OUTPUT inserted.id, inserted.ordem INTO @questoes
    SELECT @atividade, CONCAT(N'Questão sintética ',n), N'Correta', N'Incorreta', N'A', n FROM @numeros;

    -- As combinações de alunos, questões e repetições criam 200 mil respostas
    -- distribuídas em cinco dias. O módulo cinco distribui os erros sintéticos.
    DECLARE @dia date = '20261120';
    INSERT dbo.respostas_aluno(aluno_id,questao_id,resposta_dada,correta,respondido_em)
    SELECT a.id, q.id, CASE WHEN (e.n + q.ordem) % 5 = 0 THEN N'B' ELSE N'A' END,
           CASE WHEN (e.n + q.ordem) % 5 = 0 THEN 0 ELSE 1 END,
           DATEADD(minute, e.n, DATEADD(day, e.n % 5, CAST(@dia AS datetime2)))
    FROM @alunos a CROSS JOIN @questoes q CROSS JOIN @numeros e WHERE e.n <= 20;
    DECLARE @inseridas int = @@ROWCOUNT;
    DECLARE @aluno bigint = (SELECT MIN(id) FROM @alunos);
    SELECT @inseridas AS respostas_sinteticas, 100 AS alunos, 100 AS questoes, 5 AS dias;

    -- Predicado de primeiro erro do relatório. Somente a busca interna muda no comparativo.
    -- NOT EXISTS mantém o erro que não possui outro anterior no mesmo dia e
    -- questão. O ID desempata respostas com o mesmo horário.
    -- STATISTICS XML devolve o plano real. O segundo cenário fixa um índice
    -- na busca interna, mantendo os filtros e a contagem para comparação.
    SET STATISTICS XML ON;
    SELECT N'Plano escolhido pelo otimizador' AS cenario, COUNT_BIG(*) AS erros
    FROM dbo.respostas_aluno r
    WHERE r.aluno_id=@aluno AND r.respondido_em >= @dia AND r.respondido_em < DATEADD(day,1,@dia)
      AND r.correta=0 AND NOT EXISTS (
          SELECT 1 FROM dbo.respostas_aluno anterior
          WHERE anterior.aluno_id=r.aluno_id AND anterior.questao_id=r.questao_id AND anterior.correta=0
            AND anterior.respondido_em >= CAST(r.respondido_em AS date)
            AND (anterior.respondido_em < r.respondido_em OR (anterior.respondido_em=r.respondido_em AND anterior.id<r.id))
            AND (anterior.sessao_id IS NULL OR EXISTS (
                SELECT 1 FROM dbo.sessoes_estudo titular WHERE titular.id=anterior.sessao_id AND titular.aluno_id=anterior.aluno_id
            ))
      ) OPTION (RECOMPILE);

    SELECT N'Busca interna com índice geral' AS cenario, COUNT_BIG(*) AS erros
    FROM dbo.respostas_aluno r
    WHERE r.aluno_id=@aluno AND r.respondido_em >= @dia AND r.respondido_em < DATEADD(day,1,@dia)
      AND r.correta=0 AND NOT EXISTS (
          SELECT 1 FROM dbo.respostas_aluno anterior WITH (INDEX(idx_respostas_aluno_data))
          WHERE anterior.aluno_id=r.aluno_id AND anterior.questao_id=r.questao_id AND anterior.correta=0
            AND anterior.respondido_em >= CAST(r.respondido_em AS date)
            AND (anterior.respondido_em < r.respondido_em OR (anterior.respondido_em=r.respondido_em AND anterior.id<r.id))
            AND (anterior.sessao_id IS NULL OR EXISTS (
                SELECT 1 FROM dbo.sessoes_estudo titular WHERE titular.id=anterior.sessao_id AND titular.aluno_id=anterior.aluno_id
            ))
      ) OPTION (RECOMPILE);
    SET STATISTICS XML OFF;
    -- Remove o lote de medição. IDs já reservados pelo SQL Server podem deixar lacunas.
    ROLLBACK;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
