-- O gatilho da V2 inicializa o desempenho por assunto ao cadastrar cada aluno.
DECLARE @desempenho TABLE (
    email nvarchar(100) NOT NULL,
    assunto nvarchar(100) NOT NULL,
    nivel int NOT NULL,
    consecutivos int NOT NULL,
    acertos int NOT NULL,
    erros int NOT NULL
);

INSERT INTO @desempenho (email, assunto, nivel, consecutivos, acertos, erros)
VALUES
    (N'ana.demo@educria.com',   N'somar e subtrair',      3, 0, 4, 2),
    (N'ana.demo@educria.com',   N'frações',               1, 0, 1, 5),
    (N'ana.demo@educria.com',   N'porcentagem',           1, 0, 0, 3),
    (N'bruno.demo@educria.com', N'somar e subtrair',      9, 0, 6, 0),
    (N'bruno.demo@educria.com', N'multiplicar e dividir', 8, 0, 3, 1),
    (N'carla.demo@educria.com', N'somar e subtrair',      5, 0, 4, 1),
    (N'carla.demo@educria.com', N'equações',              2, 1, 1, 3),
    -- Três alunos com nível baixo em frações geram o alerta da turma.
    (N'bruno.demo@educria.com', N'frações',               2, 0, 1, 3),
    (N'carla.demo@educria.com', N'frações',               2, 0, 1, 3);

DECLARE @esperados int = (
    SELECT COUNT(*) FROM @desempenho v JOIN #demo_alunos a ON a.email = v.email WHERE a.novo = 1
);

UPDATE d
SET nivel_atual = v.nivel,
    erros_consecutivos = v.consecutivos,
    total_acertos = v.acertos,
    total_erros = v.erros
FROM @desempenho v
JOIN #demo_alunos aluno ON aluno.email = v.email
JOIN dbo.materias m ON m.nome = N'Matemática'
JOIN dbo.assuntos a ON a.materia_id = m.id AND a.nome = v.assunto
JOIN dbo.desempenho_por_assunto d ON d.aluno_id = aluno.id AND d.assunto_id = a.id
WHERE aluno.novo = 1;

IF @@ROWCOUNT <> @esperados
    THROW 51130, N'O catálogo de Matemática ou o desempenho inicial por assunto está incompleto.', 1;
