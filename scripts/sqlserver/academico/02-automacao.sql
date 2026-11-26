-- Rotinas de demonstração. A pontuação continua sendo lançada pela API.
IF DB_NAME() NOT LIKE N'educria[_]sqlserver[_]teste[_]%'
    OR NOT EXISTS (SELECT 1 FROM sys.extended_properties
                   WHERE class=0 AND name=N'EducriaAmbiente' AND CONVERT(nvarchar(30),value)=N'descartavel')
    THROW 51030, N'As rotinas acadêmicas exigem banco descartável identificado.', 1;

-- Cada definição precisa iniciar o próprio lote de compilação no SQL Server.
-- EXEC envia a definição em um lote separado. A função escalar retorna um
-- número por aluno e dia. DISTINCT mantém cada questão errada uma única vez.
EXEC(N'
CREATE OR ALTER FUNCTION dbo.educria_fn_erros_diarios(@aluno bigint, @dia date)
RETURNS int
AS
BEGIN
    RETURN (
        SELECT COUNT(DISTINCT r.questao_id)
        FROM dbo.respostas_aluno r
        WHERE r.aluno_id=@aluno AND r.correta=0
          AND r.respondido_em >= @dia AND r.respondido_em < DATEADD(day,1,@dia)
    );
END;
');
-- A procedure retorna uma linha de resumo para a turma informada. LEFT JOIN
-- mantém turmas sem atividades. COUNT sobre a PK ignora a linha sem atividade
-- e COALESCE fornece zero quando o saldo ou a soma são nulos.
EXEC(N'
CREATE OR ALTER PROCEDURE dbo.educria_sp_resumo_turma @turma bigint
AS
BEGIN
    SET NOCOUNT ON;
    SELECT t.nome AS turma, COALESCE(p.pontos_totais,0) AS pontos_publicos,
           COUNT(a.id) AS atividades, COALESCE(SUM(a.pontos),0) AS pontos_lancados
    FROM dbo.turmas t
    LEFT JOIN dbo.pontuacao_coletiva p ON p.turma_id=t.id
    LEFT JOIN dbo.atividades_coletivas a ON a.turma_id=t.id
    WHERE t.id=@turma
    GROUP BY t.id,t.nome,p.pontos_totais;
END;
');
