CREATE INDEX idx_respostas_aluno_pontos_por_sessao ON dbo.respostas_aluno
    (aluno_id, sessao_id) INCLUDE (pontos_individuais)
    WHERE pontos_individuais IS NOT NULL;
