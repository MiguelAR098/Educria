INSERT INTO dbo.materias(nome) VALUES (N'Matemática'), (N'Português'), (N'Ciências');
INSERT INTO dbo.assuntos(materia_id,nome)
SELECT m.id, a.nome
FROM dbo.materias m
CROSS JOIN (VALUES (N'somar e subtrair'),(N'multiplicar e dividir'),(N'frações'),(N'porcentagem'),
    (N'proporção'),(N'equações'),(N'área e perímetro'),(N'várias etapas')) a(nome)
WHERE m.nome=N'Matemática';
GO
CREATE TRIGGER dbo.trg_aluno_desempenho_por_assunto ON dbo.alunos
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.desempenho_por_assunto(aluno_id,assunto_id)
    SELECT i.usuario_id, a.id FROM inserted i CROSS JOIN dbo.assuntos a;
END;
GO
