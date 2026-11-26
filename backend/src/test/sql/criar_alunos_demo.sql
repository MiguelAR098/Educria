-- Cria 4 alunos de demonstração na primeira turma do professor@educria.com,
-- cada um em uma situação diferente. Login de todos: <email> / aluno123
-- Rode depois de criar_professor_demo.sql:
--   docker compose exec -T db psql -U educria -d educria < backend/src/test/sql/criar_alunos_demo.sql
\set ON_ERROR_STOP on
BEGIN;

INSERT INTO usuarios (email, nome, senha, perfil)
SELECT v.email, v.nome, '$2a$10$dLG/Mt2a83PQtiaEXxiEsuntVCSm3PAVknmQ091bQJrIY/PdHytxm', 'ALUNO'
FROM (VALUES
    ('ana.demo@educria.com',   'Ana Lima'),
    ('bruno.demo@educria.com', 'Bruno Reis'),
    ('carla.demo@educria.com', 'Carla Souza'),
    ('diego.demo@educria.com', 'Diego Alves')
) AS v(email, nome)
ON CONFLICT (email) DO NOTHING;

INSERT INTO alunos (usuario_id, matricula, turma_id)
SELECT u.id, v.matricula,
       (SELECT pt.turma_id FROM professor_turmas pt
          JOIN usuarios p ON p.id = pt.professor_id
         WHERE p.email = 'professor@educria.com'
         ORDER BY pt.turma_id LIMIT 1)
FROM usuarios u
JOIN (VALUES
    ('ana.demo@educria.com',   '2026001'),
    ('bruno.demo@educria.com', '2026002'),
    ('carla.demo@educria.com', '2026003'),
    ('diego.demo@educria.com', '2026004')
) AS v(email, matricula) ON v.email = u.email
ON CONFLICT (usuario_id) DO NOTHING;

-- Esperado: Ana EM_RISCO, Carla ATENCAO, Bruno NO_CAMINHO, Diego SEM_DADOS
INSERT INTO desempenho_adaptativo
    (aluno_id, nivel_atual, erros_consecutivos, total_acertos, total_erros, atualizado_em)
SELECT u.id, v.nivel, v.consecutivos, v.acertos, v.erros, now() - v.atras
FROM usuarios u
JOIN (VALUES
    ('ana.demo@educria.com',   2, 0, 2, 8, interval '1 day'),
    ('bruno.demo@educria.com', 7, 0, 9, 1, interval '0 day'),
    ('carla.demo@educria.com', 4, 1, 5, 4, interval '5 days'),
    ('diego.demo@educria.com', 1, 0, 0, 0, interval '0 day')
) AS v(email, nivel, consecutivos, acertos, erros, atras) ON v.email = u.email
ON CONFLICT (aluno_id) DO NOTHING;

COMMIT;
