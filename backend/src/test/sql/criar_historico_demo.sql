-- Em uma base nova, o callback Flyway do perfil dev fornece as questões de Matemática.
DECLARE @agora datetime2(6) = CONVERT(datetime2(6), SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
DECLARE @matematica bigint = (SELECT id FROM dbo.materias WHERE nome = N'Matemática');
DECLARE @carla bigint = (SELECT id FROM #demo_alunos WHERE email = N'carla.demo@educria.com' AND novo = 1);
DECLARE @ana bigint = (SELECT id FROM #demo_alunos WHERE email = N'ana.demo@educria.com' AND novo = 1);

-- Carla começa com oito revisões vencidas, agendadas para ontem em São Paulo.
IF @carla IS NOT NULL
BEGIN
    INSERT INTO dbo.repeticao_espacada (aluno_id, questao_id, intervalo_atual, proxima_revisao)
    SELECT TOP (8) @carla, q.id, N'D1', DATEADD(day, -1, CONVERT(date, @agora))
    FROM dbo.questoes q
    JOIN dbo.atividades atividade ON atividade.id = q.atividade_id
    JOIN dbo.assuntos a ON a.id = q.assunto_id
    WHERE atividade.ativa = 1 AND q.ativa = 1 AND q.dificuldade IS NOT NULL AND a.materia_id = @matematica
    ORDER BY q.id;

    IF @@ROWCOUNT <> 8
        THROW 51140, N'A carga das revisões exige pelo menos oito questões ativas de Matemática com dificuldade cadastrada.', 1;
END;

-- Ana tem três erros recentes em frações. Sessão, passo e níveis permanecem nulos.
IF @ana IS NOT NULL
BEGIN
    INSERT INTO dbo.respostas_aluno (aluno_id, questao_id, resposta_dada, correta, respondido_em)
    SELECT TOP (3) @ana, q.id, resposta.letra, 0,
        DATEADD(day, -CONVERT(int, ROW_NUMBER() OVER (ORDER BY q.id)), @agora)
    FROM dbo.questoes q
    JOIN dbo.atividades atividade ON atividade.id = q.atividade_id
    JOIN dbo.assuntos a ON a.id = q.assunto_id
    CROSS APPLY (
        SELECT TOP (1) alternativa.letra
        FROM (VALUES (N'A', q.alternativa_a), (N'B', q.alternativa_b),
                     (N'C', q.alternativa_c), (N'D', q.alternativa_d)) AS alternativa(letra, texto)
        WHERE alternativa.letra <> UPPER(q.resposta_correta)
          AND NULLIF(TRIM(alternativa.texto), N'') IS NOT NULL
        ORDER BY alternativa.letra
    ) resposta
    WHERE atividade.ativa = 1 AND q.ativa = 1 AND a.materia_id = @matematica AND a.nome = N'frações'
    ORDER BY q.id;

    IF @@ROWCOUNT <> 3
        THROW 51141, N'A carga do histórico exige pelo menos três questões ativas de frações de Matemática com uma alternativa incorreta preenchida.', 1;
END;
