-- Dados de histórico para os alertas da turma (etapa 9). Rode depois de criar_alunos_demo.sql
-- e criar_desempenho_assuntos_demo.sql:
--   docker compose exec -T db psql -U educria -d educria < backend/src/test/sql/criar_historico_demo.sql
-- Esperado no painel da turma: alerta "Assunto travado na turma" (frações), alerta "Revisões acumuladas"
-- (Carla) e "Erra mais em: frações" para a Ana. Evolução em 30 dias, tendência e os alertas de perda de nível
-- e de "sem desafio" dependem de respostas com nível, que só existem quando o aluno estuda pelo sistema.
\set ON_ERROR_STOP on
BEGIN;

-- 1) Frações travada: Ana já está no nível 1; Bruno e Carla passam ao nível 2 (3 alunos no nível baixo).
UPDATE desempenho_por_assunto d
SET nivel_atual = 2, erros_consecutivos = 0, total_acertos = 1, total_erros = 3
FROM usuarios u, assuntos a
WHERE d.aluno_id = u.id AND d.assunto_id = a.id AND a.nome = 'frações'
  AND u.email IN ('bruno.demo@educria.com', 'carla.demo@educria.com');

-- 2) Carla com 8 revisões vencidas de questões de Matemática.
INSERT INTO repeticao_espacada (aluno_id, questao_id, intervalo_atual, proxima_revisao)
SELECT u.id, q.id, 'D1', CURRENT_DATE - 1
FROM usuarios u,
     (SELECT qq.id FROM questoes qq
        JOIN assuntos a ON a.id = qq.assunto_id
        JOIN materias m ON m.id = a.materia_id
       WHERE m.nome = 'Matemática'
       ORDER BY qq.id LIMIT 8) q
WHERE u.email = 'carla.demo@educria.com'
ON CONFLICT DO NOTHING;

-- 3) Ana com 3 erros recentes em frações (respostas sem sessão, portanto sem nível).
INSERT INTO respostas_aluno (aluno_id, questao_id, resposta_dada, correta, respondido_em)
SELECT u.id, q.id, 'A', FALSE, now() - (q.n || ' days')::interval
FROM usuarios u,
     (SELECT qq.id, row_number() OVER (ORDER BY qq.id) AS n
        FROM questoes qq
        JOIN assuntos a ON a.id = qq.assunto_id
       WHERE a.nome = 'frações'
       ORDER BY qq.id LIMIT 3) q
WHERE u.email = 'ana.demo@educria.com'
  AND NOT EXISTS (SELECT 1 FROM respostas_aluno r
                   WHERE r.aluno_id = u.id AND r.questao_id = q.id AND r.sessao_id IS NULL);

COMMIT;
