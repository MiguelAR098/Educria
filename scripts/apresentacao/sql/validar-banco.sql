SET NOCOUNT ON;

IF DB_NAME() COLLATE Latin1_General_100_BIN2 <> N'educria_demo_apresentacao'
   OR NOT EXISTS (
       SELECT 1 FROM sys.extended_properties
       WHERE class = 0 AND name = N'EducriaAmbiente'
         AND CONVERT(nvarchar(30), value) COLLATE Latin1_General_100_BIN2 = N'descartavel'
   )
    THROW 51203, N'A verificação exige a base reservada e marcada da apresentação.', 1;
GO

IF (SELECT COUNT(*) FROM dbo.flyway_schema_history WHERE version IS NOT NULL) <> 5
   OR (SELECT COUNT(DISTINCT version) FROM dbo.flyway_schema_history
       WHERE success = 1 AND version IN ('1', '2', '3', '4', '5')) <> 5
    THROW 51204, N'Esperadas as cinco migrations SQL Server V1–V5 com sucesso.', 1;

IF NOT EXISTS (
    SELECT 1 FROM sys.extended_properties WHERE class = 0 AND name = N'EducriaCargaApresentacao'
      AND CONVERT(nvarchar(30), value) = N'v1'
)
    THROW 51205, N'Marcador da carga sintética ausente.', 1;

IF (SELECT COUNT(*) FROM dbo.questoes WHERE atividade_id = -1) <> 160
   OR (SELECT COUNT(DISTINCT ordem) FROM dbo.questoes
       WHERE atividade_id = -1 AND ordem BETWEEN 1 AND 160) <> 160
    THROW 51206, N'Esperadas 160 questões de demonstração em ordens distintas.', 1;

IF EXISTS (
    SELECT a.nome, n.nivel
    FROM (VALUES (N'somar e subtrair'), (N'multiplicar e dividir'), (N'frações'),
        (N'porcentagem'), (N'proporção'), (N'equações'), (N'área e perímetro'), (N'várias etapas')) a(nome)
    CROSS JOIN (VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10)) n(nivel)
    LEFT JOIN dbo.materias m ON m.nome = N'Matemática'
    LEFT JOIN dbo.assuntos s ON s.materia_id = m.id AND s.nome = a.nome
    LEFT JOIN dbo.questoes q ON q.atividade_id = -1 AND q.assunto_id = s.id AND q.dificuldade = n.nivel
    GROUP BY a.nome, n.nivel
    HAVING COUNT(q.id) <> 2
)
    THROW 51207, N'Distribuição da demonstração deve ser 8 assuntos × 10 níveis × 2.', 1;

DECLARE @turma bigint, @professor bigint, @aluno bigint, @colega bigint, @admin bigint;
IF (SELECT COUNT(*) FROM dbo.turmas WHERE codigo = N'DEMO-EDUCRIA') <> 1
    THROW 51208, N'Esperada uma turma com o código reservado da apresentação.', 1;

SELECT @turma = id FROM dbo.turmas WHERE codigo = N'DEMO-EDUCRIA';
SELECT @professor = id FROM dbo.usuarios WHERE email = N'professor@demo.educria.test' AND perfil = N'PROFESSOR';
SELECT @aluno = id FROM dbo.usuarios WHERE email = N'aluno@demo.educria.test' AND perfil = N'ALUNO';
SELECT @colega = id FROM dbo.usuarios WHERE email = N'colega@demo.educria.test' AND perfil = N'ALUNO';
SELECT @admin = id FROM dbo.usuarios WHERE email = N'admin@demo.educria.test' AND perfil = N'ADMIN';

IF @admin IS NULL OR NOT EXISTS (SELECT 1 FROM dbo.admins WHERE usuario_id = @admin)
   OR NOT EXISTS (
       SELECT 1 FROM dbo.professor_turmas pt
       JOIN dbo.professores p ON p.usuario_id = pt.professor_id
       JOIN dbo.materias m ON m.id = p.materia_id
       WHERE pt.professor_id = @professor AND pt.turma_id = @turma AND m.nome = N'Matemática'
   )
   OR (SELECT COUNT(*) FROM dbo.alunos WHERE usuario_id IN (@aluno, @colega) AND turma_id = @turma) <> 2
   OR (SELECT COUNT(*) FROM dbo.sessoes_estudo WHERE aluno_id = @aluno AND situacao = N'ENCERRADA') < 2
    THROW 51209, N'Contas, vínculos ou histórico da demonstração incompletos.', 1;

-- A assinatura compara o conteúdo da demonstração antes e depois da parada, sem divulgar os registros.
DECLARE @snapshot nvarchar(max) = (
    SELECT @turma AS turma, @professor AS professor, @aluno AS aluno, @colega AS colega, @admin AS administrador,
        (SELECT id, ordem, assunto_id, dificuldade, enunciado, alternativa_a, alternativa_b, alternativa_c,
                alternativa_d, resposta_correta, explicacao, ativa
         FROM dbo.questoes WHERE atividade_id = -1 ORDER BY ordem FOR JSON PATH) AS questoes,
        (SELECT id, aluno_id, assunto_id, situacao, passo
         FROM dbo.sessoes_estudo WHERE aluno_id IN (@aluno, @colega) ORDER BY id FOR JSON PATH) AS sessoes,
        (SELECT id, aluno_id, questao_id, sessao_id, passo, correta, resposta_dada, pontos_individuais
         FROM dbo.respostas_aluno WHERE aluno_id IN (@aluno, @colega) ORDER BY id FOR JSON PATH) AS respostas,
        (SELECT aluno_id, assunto_id, nivel_atual, total_acertos, total_erros
         FROM dbo.desempenho_por_assunto WHERE aluno_id IN (@aluno, @colega)
         ORDER BY aluno_id, assunto_id FOR JSON PATH) AS niveis,
        (SELECT pontos_totais FROM dbo.pontuacao_coletiva WHERE turma_id = @turma FOR JSON PATH) AS pontos
    FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
);

SELECT 'SNAPSHOT=' + CONVERT(varchar(64), HASHBYTES('SHA2_256', @snapshot), 2);
GO
