SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;

IF DB_NAME() NOT LIKE N'educria[_]demo[_]%' OR NOT EXISTS (
    SELECT 1 FROM sys.extended_properties
    WHERE class = 0 AND name = N'EducriaAmbiente' AND CONVERT(nvarchar(30), value) = N'descartavel'
)
    THROW 51100, N'A carga exige um banco educria_demo_ com o marcador EducriaAmbiente=descartavel.', 1;

BEGIN TRANSACTION;

DECLARE @bloqueio int;
EXEC @bloqueio = sys.sp_getapplock @Resource = N'educria.carga_demo_contas',
    @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 15000;
IF @bloqueio < 0
    THROW 51101, N'Não foi possível obter o bloqueio da carga de demonstração.', 1;

CREATE TABLE #demo_turma (id bigint NOT NULL PRIMARY KEY);
CREATE TABLE #demo_alunos (
    email nvarchar(100) COLLATE DATABASE_DEFAULT NOT NULL PRIMARY KEY,
    id bigint NOT NULL,
    novo bit NOT NULL
);
GO
