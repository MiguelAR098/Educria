SET NOCOUNT ON;
SET XACT_ABORT ON;

IF DB_NAME() <> N'master'
    THROW 51200, N'A criação da base de apresentação exige master no contêiner SQL dedicado.', 1;

DECLARE @bloqueio int;
EXEC @bloqueio = sys.sp_getapplock @Resource = N'educria.apresentacao.criar_banco',
    @LockMode = 'Exclusive', @LockOwner = 'Session', @LockTimeout = 4000;
IF @bloqueio < 0
    THROW 51201, N'Não foi possível verificar a base de apresentação a tempo.', 1;

IF DB_ID(N'educria_demo_apresentacao') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE [educria_demo_apresentacao] COLLATE Latin1_General_100_CS_AS_SC');
    EXEC [educria_demo_apresentacao].sys.sp_addextendedproperty
        @name = N'EducriaAmbiente', @value = N'descartavel';
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.databases
    WHERE name COLLATE Latin1_General_100_BIN2 = N'educria_demo_apresentacao' AND state = 0
) OR NOT EXISTS (
    SELECT 1 FROM [educria_demo_apresentacao].sys.extended_properties
    WHERE class = 0 AND name = N'EducriaAmbiente'
      AND CONVERT(nvarchar(30), value) COLLATE Latin1_General_100_BIN2 = N'descartavel'
)
    THROW 51202, N'Base de apresentação indisponível ou sem marcador. Nenhum marcador foi aplicado a base existente.', 1;
GO
