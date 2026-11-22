-- Cria um professor de demonstração (e uma turma, se não houver nenhuma).
-- Login: professor@educria.com / professor123
-- Uso (com a aplicação já iniciada, para o Flyway ter criado as tabelas):
--   psql -X -v ON_ERROR_STOP=1 -f backend/src/test/sql/criar_professor_demo.sql DATABASE
\set ON_ERROR_STOP on
BEGIN;

INSERT INTO turmas (nome, codigo, ano_letivo)
SELECT '8º A', '8A', 2026
WHERE NOT EXISTS (SELECT 1 FROM turmas);

INSERT INTO pontuacao_coletiva (turma_id)
SELECT t.id FROM turmas t
WHERE NOT EXISTS (SELECT 1 FROM pontuacao_coletiva p WHERE p.turma_id = t.id);

INSERT INTO usuarios (email, nome, senha, perfil)
VALUES ('professor@educria.com', 'Professora Demo',
        '$2a$10$pDTii9Cwi6nFYZ2iKXnJU.lPmd7S639cak2NzsFwRs5EZDpX5i57S', 'PROFESSOR')
ON CONFLICT (email) DO NOTHING;

INSERT INTO professores (usuario_id, materia_id)
SELECT u.id, m.id FROM usuarios u, materias m
WHERE u.email = 'professor@educria.com' AND m.nome = 'Matemática'
ON CONFLICT DO NOTHING;

INSERT INTO professor_turmas (professor_id, turma_id)
SELECT p.usuario_id, t.id
FROM professores p
JOIN usuarios u ON u.id = p.usuario_id AND u.email = 'professor@educria.com'
CROSS JOIN turmas t
ON CONFLICT DO NOTHING;

COMMIT;
