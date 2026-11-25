-- NULL conserva as respostas anteriores à pontuação sem inventar valores retroativos.
ALTER TABLE dbo.respostas_aluno ADD pontos_individuais int NULL;
GO

ALTER TABLE dbo.respostas_aluno ADD CONSTRAINT ck_resposta_pontos_individuais CHECK (
    pontos_individuais IS NULL OR (
        sessao_id IS NOT NULL AND pontos_individuais BETWEEN 0 AND 100
        AND (correta = 1 OR pontos_individuais = 0)
    )
);

CREATE INDEX idx_respostas_aluno_questao_recente ON dbo.respostas_aluno
    (aluno_id, questao_id, respondido_em DESC, id DESC) INCLUDE (correta);
