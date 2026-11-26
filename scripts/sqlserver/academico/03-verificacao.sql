DECLARE @contagem TABLE(tabela sysname PRIMARY KEY, total bigint);
INSERT @contagem
SELECT N'usuarios', COUNT_BIG(*) FROM dbo.usuarios UNION ALL
SELECT N'turmas', COUNT_BIG(*) FROM dbo.turmas UNION ALL
SELECT N'materias', COUNT_BIG(*) FROM dbo.materias UNION ALL
SELECT N'assuntos', COUNT_BIG(*) FROM dbo.assuntos UNION ALL
SELECT N'alunos', COUNT_BIG(*) FROM dbo.alunos UNION ALL
SELECT N'admins', COUNT_BIG(*) FROM dbo.admins UNION ALL
SELECT N'professores', COUNT_BIG(*) FROM dbo.professores UNION ALL
SELECT N'professor_turmas', COUNT_BIG(*) FROM dbo.professor_turmas UNION ALL
SELECT N'desempenho_adaptativo', COUNT_BIG(*) FROM dbo.desempenho_adaptativo UNION ALL
SELECT N'desempenho_por_assunto', COUNT_BIG(*) FROM dbo.desempenho_por_assunto UNION ALL
SELECT N'pontuacao_coletiva', COUNT_BIG(*) FROM dbo.pontuacao_coletiva UNION ALL
SELECT N'atividades', COUNT_BIG(*) FROM dbo.atividades UNION ALL
SELECT N'questoes', COUNT_BIG(*) FROM dbo.questoes UNION ALL
SELECT N'sessoes_estudo', COUNT_BIG(*) FROM dbo.sessoes_estudo UNION ALL
SELECT N'sessao_questoes', COUNT_BIG(*) FROM dbo.sessao_questoes UNION ALL
SELECT N'respostas_aluno', COUNT_BIG(*) FROM dbo.respostas_aluno UNION ALL
SELECT N'repeticao_espacada', COUNT_BIG(*) FROM dbo.repeticao_espacada UNION ALL
SELECT N'atividades_coletivas', COUNT_BIG(*) FROM dbo.atividades_coletivas UNION ALL
SELECT N'recados', COUNT_BIG(*) FROM dbo.recados;
SELECT tabela,total FROM @contagem ORDER BY tabela;
IF EXISTS (SELECT 1 FROM @contagem WHERE total < 3)
    THROW 51010, N'Uma tabela do domínio não atingiu três registros.', 1;
IF EXISTS (
    SELECT 1 FROM @contagem c
    WHERE NOT EXISTS (SELECT 1 FROM sys.key_constraints k
                      WHERE k.parent_object_id=OBJECT_ID(N'dbo.'+c.tabela) AND k.type='PK')
)
    THROW 51011, N'Tabela do domínio sem PK.', 1;
IF IDENT_SEED(N'dbo.recados') <> 1000 OR IDENT_INCR(N'dbo.recados') <> 10
    THROW 51012, N'Autonumeração dos recados fora do domínio previsto.', 1;
IF EXISTS (SELECT 1 FROM dbo.alunos a CROSS JOIN dbo.assuntos s LEFT JOIN dbo.desempenho_por_assunto d
           ON d.aluno_id=a.usuario_id AND d.assunto_id=s.id WHERE d.id IS NULL)
    THROW 51013, N'O trigger deveria preencher os níveis dos novos alunos.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.professor_turmas GROUP BY professor_id HAVING COUNT(*) >= 2)
   OR NOT EXISTS (SELECT 1 FROM dbo.professor_turmas GROUP BY turma_id HAVING COUNT(*) >= 2)
    THROW 51014, N'A demonstração do relacionamento N:N está incompleta.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE delete_referential_action_desc='CASCADE')
   OR NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.respostas_aluno') AND name=N'idx_respostas_erros')
    THROW 51015, N'Verifique a cascata e o índice de erros.', 1;
IF EXISTS (SELECT 1 FROM dbo.respostas_aluno r JOIN dbo.sessoes_estudo s ON s.id=r.sessao_id
           WHERE s.situacao=N'ENCERRADA' AND r.reaprender=1
             AND NOT EXISTS (SELECT 1 FROM dbo.respostas_aluno a
                             WHERE a.sessao_id=r.sessao_id AND a.questao_id=r.questao_id AND a.correta=1 AND a.passo>r.passo))
    THROW 51016, N'A demonstração contém uma pendência em sessão encerrada.', 1;

SELECT IDENT_SEED(N'dbo.recados') AS inicio, IDENT_INCR(N'dbo.recados') AS incremento,
       MIN(r.id) AS menor_id, MAX(r.id) AS maior_id
FROM dbo.recados r;

-- Aliases, três JOINs e as cinco agregações exigidas, sobre pontuação pública.
SELECT t.codigo AS turma, COUNT(a.id) AS atividades, SUM(a.pontos) AS pontos,
       AVG(CONVERT(decimal(8,2),a.pontos)) AS media, MAX(a.pontos) AS maximo, MIN(a.pontos) AS minimo
FROM dbo.atividades_coletivas a
JOIN dbo.turmas t ON t.id=a.turma_id
JOIN dbo.professores p ON p.usuario_id=a.professor_id
JOIN dbo.professor_turmas vinculo ON vinculo.turma_id=t.id AND vinculo.professor_id=p.usuario_id
GROUP BY t.codigo ORDER BY t.codigo;

DECLARE @idAluno bigint = (SELECT id FROM dbo.usuarios WHERE email=N'aluno1@educria.example.test');
DECLARE @dia date = (SELECT MIN(CONVERT(date,respondido_em)) FROM dbo.respostas_aluno WHERE aluno_id=@idAluno);
IF COALESCE(dbo.educria_fn_erros_diarios(@idAluno,@dia),0) <> 1
    THROW 51017, N'A função deveria contar um erro no dia do aluno de demonstração.', 1;
SELECT @dia AS dia, dbo.educria_fn_erros_diarios(@idAluno,@dia) AS erros_diarios;

DECLARE @idTurma bigint = (SELECT id FROM dbo.turmas WHERE codigo=N'DEMO1');
DECLARE @resumo TABLE(turma nvarchar(100),pontos_publicos int,atividades int,pontos_lancados int);
INSERT @resumo EXEC dbo.educria_sp_resumo_turma @turma=@idTurma;
IF NOT EXISTS (SELECT 1 FROM @resumo WHERE pontos_publicos=20 AND atividades=1 AND pontos_lancados=20)
    THROW 51018, N'A procedure deveria retornar uma atividade pública de vinte pontos.', 1;
SELECT turma,pontos_publicos,atividades,pontos_lancados FROM @resumo;
