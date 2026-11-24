-- Professor de Português para testar uma matéria sem assuntos e sem questões.
-- Login: portugues@educria.com / professor123. Leciona nas mesmas turmas do professor demo.
--   docker compose exec -T db psql -U educria -d educria < backend/src/test/sql/criar_professor_portugues_demo.sql
\set ON_ERROR_STOP on
BEGIN;

INSERT INTO usuarios (email, nome, senha, perfil)
VALUES ('portugues@educria.com', 'Professor de Português', '$2a$10$pDTii9Cwi6nFYZ2iKXnJU.lPmd7S639cak2NzsFwRs5EZDpX5i57S', 'PROFESSOR')
ON CONFLICT (email) DO NOTHING;

INSERT INTO professores (usuario_id, materia_id)
SELECT u.id, m.id FROM usuarios u, materias m
WHERE u.email = 'portugues@educria.com' AND m.nome = 'Português'
ON CONFLICT DO NOTHING;

INSERT INTO professor_turmas (professor_id, turma_id)
SELECT p.usuario_id, pt.turma_id
FROM professores p
JOIN usuarios u ON u.id = p.usuario_id AND u.email = 'portugues@educria.com'
CROSS JOIN (SELECT DISTINCT turma_id FROM professor_turmas) pt
ON CONFLICT DO NOTHING;

COMMIT;
