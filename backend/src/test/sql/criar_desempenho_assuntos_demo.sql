-- Preenche o desempenho por assunto dos alunos de criar_alunos_demo.sql.
-- (O banco já cria as linhas zeradas por gatilho ao cadastrar o aluno; aqui só atualizamos.)
--   docker compose exec -T db psql -U educria -d educria < backend/src/test/sql/criar_desempenho_assuntos_demo.sql
\set ON_ERROR_STOP on
BEGIN;

UPDATE desempenho_por_assunto d
SET nivel_atual = v.nivel, erros_consecutivos = v.consecutivos,
    total_acertos = v.acertos, total_erros = v.erros
FROM (VALUES
    -- aluno,                   assunto,                 nível, seguidos, acertos, erros
    ('ana.demo@educria.com',   'somar e subtrair',        3, 0, 4, 2),
    ('ana.demo@educria.com',   'frações',                 1, 0, 1, 5),
    ('ana.demo@educria.com',   'porcentagem',             1, 0, 0, 3),
    ('bruno.demo@educria.com', 'somar e subtrair',        9, 0, 6, 0),
    ('bruno.demo@educria.com', 'multiplicar e dividir',   8, 0, 3, 1),
    ('carla.demo@educria.com', 'somar e subtrair',        5, 0, 4, 1),
    ('carla.demo@educria.com', 'equações',                2, 1, 1, 3)
) AS v(email, assunto, nivel, consecutivos, acertos, erros)
JOIN usuarios u ON u.email = v.email
JOIN assuntos a ON a.nome = v.assunto
WHERE d.aluno_id = u.id AND d.assunto_id = a.id;

COMMIT;
