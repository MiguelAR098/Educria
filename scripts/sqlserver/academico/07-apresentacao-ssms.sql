-- Apresentação de Banco de Dados do EduCria. Selecione um bloco inteiro por vez,
-- incluindo a validação no começo do bloco. GO separa os blocos no SSMS.
-- Intercale 05-dicionario.sql para chaves e restrições e 03-verificacao.sql para
-- as 22 contagens, IDENTITY(1000,10) e as cinco agregações. Consulte o guia
-- docs/sqlserver/ROTEIRO-APRESENTACAO-BANCO.md para a ordem completa.

-- Bloco 1. Confirme a instância, a base, os marcadores e as migrations.
IF (DB_NAME() <> N'educria_sqlserver_teste_academico'
    AND DB_NAME() NOT LIKE N'educria[_]sqlserver[_]teste[_]academico[_]_%')
    OR NOT EXISTS (SELECT 1 FROM sys.extended_properties
                   WHERE class=0 AND name=N'EducriaAmbiente' AND CONVERT(nvarchar(30),value)=N'descartavel')
    THROW 51060, N'Selecione uma base acadêmica descartável identificada.', 1;

SELECT SERVERPROPERTY(N'ServerName') AS instancia,
       SERVERPROPERTY(N'ProductVersion') AS versao_sql_server, DB_NAME() AS banco;
SELECT name AS propriedade, CONVERT(nvarchar(30),value) AS valor
FROM sys.extended_properties
WHERE class=0 AND name IN (N'EducriaAmbiente',N'EducriaDemonstracao')
ORDER BY name;

IF OBJECT_ID(N'dbo.flyway_schema_history',N'U') IS NULL
    THROW 51061, N'Aplique as migrations V1 a V5 na base acadêmica.', 1;

SELECT [version] AS versao, description AS migracao, success AS aplicada
FROM dbo.flyway_schema_history
WHERE [version] IN (N'1',N'2',N'3',N'4',N'5')
ORDER BY installed_rank;
GO

-- Bloco 2. Mostre os vínculos N:N e a turma de origem das participações.
IF (DB_NAME() <> N'educria_sqlserver_teste_academico'
    AND DB_NAME() NOT LIKE N'educria[_]sqlserver[_]teste[_]academico[_]_%')
    OR NOT EXISTS (SELECT 1 FROM sys.extended_properties
                   WHERE class=0 AND name=N'EducriaAmbiente' AND CONVERT(nvarchar(30),value)=N'descartavel')
    THROW 51060, N'Selecione uma base acadêmica descartável identificada.', 1;

SELECT u.nome AS professor, t.codigo AS turma, pt.professor_id, pt.turma_id
FROM dbo.professor_turmas AS pt
JOIN dbo.professores AS p ON p.usuario_id=pt.professor_id
JOIN dbo.usuarios AS u ON u.id=p.usuario_id
JOIN dbo.turmas AS t ON t.id=pt.turma_id
ORDER BY u.nome,t.codigo;

-- A FK composta da participação exige o par grupo e turma de origem.
SELECT g.nome AS grupo, g.tipo, t.codigo AS turma_de_origem, u.nome AS aluno,
       gp.entrada_em, gp.saida_em, g.arquivado_em
FROM dbo.grupo_participacoes AS gp
JOIN dbo.grupo_turmas AS gt ON gt.grupo_id=gp.grupo_id AND gt.turma_id=gp.turma_origem_id
JOIN dbo.grupos_pedagogicos AS g ON g.id=gt.grupo_id
JOIN dbo.turmas AS t ON t.id=gt.turma_id
JOIN dbo.alunos AS a ON a.usuario_id=gp.aluno_id
JOIN dbo.usuarios AS u ON u.id=a.usuario_id
ORDER BY g.id,t.codigo,u.nome;
GO

-- Bloco 3. Veja o trigger da V2, seu efeito na carga e as duas rotinas acadêmicas.
IF (DB_NAME() <> N'educria_sqlserver_teste_academico'
    AND DB_NAME() NOT LIKE N'educria[_]sqlserver[_]teste[_]academico[_]_%')
    OR NOT EXISTS (SELECT 1 FROM sys.extended_properties
                   WHERE class=0 AND name=N'EducriaAmbiente' AND CONVERT(nvarchar(30),value)=N'descartavel')
    THROW 51060, N'Selecione uma base acadêmica descartável identificada.', 1;

SELECT tr.name AS gatilho, tr.is_disabled AS desabilitado, OBJECT_DEFINITION(tr.object_id) AS definicao
FROM sys.triggers AS tr
WHERE tr.object_id=OBJECT_ID(N'dbo.trg_aluno_desempenho_por_assunto',N'TR');

SELECT u.nome AS aluno, COUNT(d.assunto_id) AS assuntos_com_nivel
FROM dbo.alunos AS a
JOIN dbo.usuarios AS u ON u.id=a.usuario_id
LEFT JOIN dbo.desempenho_por_assunto AS d ON d.aluno_id=a.usuario_id
GROUP BY a.usuario_id,u.nome
ORDER BY u.nome;

-- Confira as definições com 02-automacao.sql antes de executar 03 ou o bloco 4.
SELECT o.name AS rotina, o.type_desc AS tipo, OBJECT_DEFINITION(o.object_id) AS definicao
FROM sys.objects AS o
WHERE o.object_id IN (OBJECT_ID(N'dbo.educria_fn_erros_diarios',N'FN'),
                      OBJECT_ID(N'dbo.educria_sp_resumo_turma',N'P'))
ORDER BY o.name;
GO

-- Bloco 4. Repita o resumo para DEMO2 após conferir a definição no bloco 3.
-- A procedure de 02-automacao.sql só usa SET NOCOUNT e SELECT.
IF (DB_NAME() <> N'educria_sqlserver_teste_academico'
    AND DB_NAME() NOT LIKE N'educria[_]sqlserver[_]teste[_]academico[_]_%')
    OR NOT EXISTS (SELECT 1 FROM sys.extended_properties
                   WHERE class=0 AND name=N'EducriaAmbiente' AND CONVERT(nvarchar(30),value)=N'descartavel')
    THROW 51060, N'Selecione uma base acadêmica descartável identificada.', 1;

IF OBJECT_ID(N'dbo.educria_sp_resumo_turma',N'P') IS NULL
    THROW 51062, N'Instale as rotinas acadêmicas pela etapa de demonstração.', 1;

DECLARE @turmaDemo bigint = (SELECT t.id FROM dbo.turmas AS t WHERE t.codigo=N'DEMO2');
IF @turmaDemo IS NULL
    THROW 51063, N'Carregue os dados acadêmicos para obter a turma DEMO2.', 1;

EXEC dbo.educria_sp_resumo_turma @turma=@turmaDemo;
GO

-- Bloco 5. Mostre as chaves, colunas incluídas e filtros dos índices.
IF (DB_NAME() <> N'educria_sqlserver_teste_academico'
    AND DB_NAME() NOT LIKE N'educria[_]sqlserver[_]teste[_]academico[_]_%')
    OR NOT EXISTS (SELECT 1 FROM sys.extended_properties
                   WHERE class=0 AND name=N'EducriaAmbiente' AND CONVERT(nvarchar(30),value)=N'descartavel')
    THROW 51060, N'Selecione uma base acadêmica descartável identificada.', 1;

SELECT OBJECT_NAME(i.object_id) AS tabela, i.name AS indice, i.is_unique AS unico,
       i.is_disabled AS desabilitado, i.filter_definition AS filtro, c.name AS coluna,
       ic.key_ordinal AS ordem_da_chave, ic.is_included_column AS coluna_incluida
FROM sys.indexes AS i
JOIN sys.index_columns AS ic ON ic.object_id=i.object_id AND ic.index_id=i.index_id
JOIN sys.columns AS c ON c.object_id=ic.object_id AND c.column_id=ic.column_id
WHERE (i.object_id=OBJECT_ID(N'dbo.respostas_aluno',N'U') AND i.name=N'idx_respostas_erros')
   OR (i.object_id=OBJECT_ID(N'dbo.grupo_participacoes',N'U') AND i.name=N'ux_grupo_participacao_ativa')
ORDER BY tabela,indice,ic.is_included_column,ic.key_ordinal,ic.index_column_id;
GO
