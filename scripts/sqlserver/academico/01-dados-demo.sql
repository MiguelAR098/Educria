IF DB_NAME() NOT LIKE N'educria[_]sqlserver[_]teste[_]%'
    OR NOT EXISTS (SELECT 1 FROM sys.extended_properties
                   WHERE class=0 AND name=N'EducriaAmbiente' AND CONVERT(nvarchar(30),value)=N'descartavel')
    THROW 51000, N'A alimentação exige banco descartável identificado.', 1;
IF @HashDemo IS NULL OR LEN(@HashDemo) <> 60 OR LEFT(@HashDemo,4) NOT IN (N'$2a$',N'$2b$',N'$2y$')
    THROW 51001, N'Forneça um hash BCrypt para os usuários de demonstração.', 1;
IF EXISTS (SELECT 1 FROM sys.extended_properties WHERE class=0 AND name=N'EducriaDemonstracao')
    RETURN;
IF EXISTS (SELECT 1 FROM dbo.usuarios) OR EXISTS (SELECT 1 FROM dbo.turmas) OR EXISTS (SELECT 1 FROM dbo.atividades)
    THROW 51002, N'Use um banco recém-migrado, contendo apenas o catálogo inicial.', 1;

SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @dados TABLE (
    n int PRIMARY KEY, assunto nvarchar(100), enunciado nvarchar(200),
    a nvarchar(30), b nvarchar(30), c nvarchar(30), d nvarchar(30), explicacao nvarchar(200)
);
INSERT @dados VALUES
    (1,N'somar e subtrair',N'Quanto é 7 + 8?',N'15',N'14',N'16',N'17',N'7 + 8 = 15.'),
    (2,N'multiplicar e dividir',N'Quanto é 3 × 4?',N'12',N'10',N'13',N'14',N'Três grupos de quatro somam doze.'),
    (3,N'frações',N'Quanto é metade de 8?',N'4',N'2',N'6',N'8',N'Divida 8 em dois grupos iguais de 4.');

INSERT dbo.turmas(nome,codigo,ano_letivo)
SELECT CONCAT(N'Turma Demo ',n), CONCAT(N'DEMO',n), YEAR(SYSDATETIME()) FROM @dados;
INSERT dbo.usuarios(email,nome,senha,perfil)
SELECT CONCAT(p.apelido,n,N'@educria.example.test'), CONCAT(p.perfil,N' Demo ',n), @HashDemo, p.perfil
FROM @dados CROSS JOIN (VALUES (N'aluno',N'ALUNO'),(N'professor',N'PROFESSOR'),(N'admin',N'ADMIN')) p(apelido,perfil);

DECLARE @vinculos TABLE (n int PRIMARY KEY, aluno bigint, professor bigint, admin bigint, turma bigint, assunto bigint);
INSERT @vinculos
SELECT d.n,u.id,p.id,ad.id,t.id,a.id FROM @dados d
JOIN dbo.usuarios u ON u.email=CONCAT(N'aluno',d.n,N'@educria.example.test')
JOIN dbo.usuarios p ON p.email=CONCAT(N'professor',d.n,N'@educria.example.test')
JOIN dbo.usuarios ad ON ad.email=CONCAT(N'admin',d.n,N'@educria.example.test')
JOIN dbo.turmas t ON t.codigo=CONCAT(N'DEMO',d.n)
JOIN dbo.materias m ON m.nome=N'Matemática'
JOIN dbo.assuntos a ON a.materia_id=m.id AND a.nome=d.assunto;

INSERT dbo.admins(usuario_id) SELECT admin FROM @vinculos;
INSERT dbo.professores(usuario_id,materia_id)
SELECT v.professor,a.materia_id FROM @vinculos v JOIN dbo.assuntos a ON a.id=v.assunto;
INSERT dbo.professor_turmas(professor_id,turma_id)
SELECT professor,turma FROM @vinculos
UNION ALL SELECT p.professor,t.turma FROM @vinculos p CROSS JOIN @vinculos t WHERE p.n=1 AND t.n=2;
INSERT dbo.alunos(usuario_id,matricula,turma_id)
SELECT aluno,CONCAT(N'DEMO',n),turma FROM @vinculos;
INSERT dbo.desempenho_adaptativo(aluno_id) SELECT aluno FROM @vinculos;

INSERT dbo.pontuacao_coletiva(turma_id,pontos_totais) SELECT turma,n*20 FROM @vinculos;
INSERT dbo.atividades_coletivas(turma_id,professor_id,tipo,titulo,pontos,resumo)
SELECT turma,professor,N'PROJETO',CONCAT(N'Projeto coletivo ',n),n*20,N'Participação da turma no projeto.'
FROM @vinculos;
INSERT dbo.recados(aluno_id,professor_id,texto)
SELECT aluno,professor,N'Confira as atividades da sua turma.' FROM @vinculos;

INSERT dbo.atividades(titulo,enunciado,nivel_recomendado)
SELECT CONCAT(N'Atividade Demo ',n),enunciado,1 FROM @dados;
INSERT dbo.questoes(atividade_id,assunto_id,autor_id,enunciado,alternativa_a,alternativa_b,alternativa_c,
                    alternativa_d,resposta_correta,ordem,dificuldade,explicacao)
SELECT atv.id,v.assunto,v.professor,d.enunciado,d.a,d.b,d.c,d.d,N'A',1,1,d.explicacao
FROM @dados d JOIN @vinculos v ON v.n=d.n JOIN dbo.atividades atv ON atv.titulo=CONCAT(N'Atividade Demo ',d.n);

DECLARE @inicio datetimeoffset(6) = DATEADD(day,-1,SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
DECLARE @respondido datetime2(6) = CONVERT(datetime2(6),@inicio);
INSERT dbo.sessoes_estudo(aluno_id,assunto_id,situacao,iniciada_em,atualizada_em,passo)
SELECT aluno,assunto,N'ENCERRADA',@inicio,DATEADD(second,3,@inicio),CASE WHEN n=1 THEN 2 ELSE 1 END FROM @vinculos;

DECLARE @estudo TABLE(n int PRIMARY KEY, aluno bigint, sessao bigint, questao bigint, assunto bigint);
INSERT @estudo SELECT v.n,v.aluno,s.id,q.id,v.assunto
FROM @vinculos v JOIN dbo.sessoes_estudo s ON s.aluno_id=v.aluno
JOIN dbo.questoes q ON q.autor_id=v.professor AND q.assunto_id=v.assunto;
INSERT dbo.sessao_questoes(sessao_id,indice,questao_id) SELECT sessao,0,questao FROM @estudo;
UPDATE s SET questao_retomada_id=e.questao
FROM dbo.sessoes_estudo s JOIN @estudo e ON e.sessao=s.id WHERE e.n=1;
-- A única pendência criada nesta demonstração é resolvida na segunda resposta.
INSERT dbo.respostas_aluno(aluno_id,questao_id,sessao_id,passo,resposta_dada,correta,reaprender,
                           respondido_em,nivel_anterior,nivel_atual)
SELECT aluno,questao,sessao,1,N'B',0,1,DATEADD(second,1,@respondido),1,1 FROM @estudo WHERE n=1;
INSERT dbo.respostas_aluno(aluno_id,questao_id,sessao_id,passo,resposta_dada,correta,
                           respondido_em,nivel_anterior,nivel_atual)
SELECT aluno,questao,sessao,CASE WHEN n=1 THEN 2 ELSE 1 END,N'A',1,
       DATEADD(second,2,@respondido),1,CASE WHEN n=1 THEN 1 ELSE 2 END FROM @estudo;
UPDATE d SET nivel_atual=CASE WHEN e.n=1 THEN 1 ELSE 2 END,
    erros_consecutivos=CASE WHEN e.n=1 THEN 1 ELSE 0 END,
    total_erros=CASE WHEN e.n=1 THEN 1 ELSE 0 END, total_acertos=CASE WHEN e.n=1 THEN 0 ELSE 1 END, versao=1
FROM dbo.desempenho_por_assunto d JOIN @estudo e ON d.aluno_id=e.aluno AND d.assunto_id=e.assunto;
INSERT dbo.repeticao_espacada(aluno_id,questao_id,proxima_revisao,ultima_revisao)
SELECT aluno,questao,DATEADD(day,1,CONVERT(date,DATEADD(second,2,@respondido))),CONVERT(date,DATEADD(second,2,@respondido))
FROM @estudo;
EXEC sys.sp_addextendedproperty @name=N'EducriaDemonstracao', @value=N'1';
COMMIT;
