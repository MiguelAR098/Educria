-- Respostas já registradas não têm um histórico confiável dos níveis.
ALTER TABLE respostas_aluno
    ADD COLUMN nivel_anterior INTEGER,
    ADD COLUMN nivel_atual INTEGER,
    ADD CONSTRAINT ck_niveis_da_resposta CHECK (
        (nivel_anterior IS NULL AND nivel_atual IS NULL)
        OR (sessao_id IS NOT NULL AND nivel_anterior IS NOT NULL AND nivel_atual IS NOT NULL
            AND nivel_anterior BETWEEN 1 AND 10 AND nivel_atual BETWEEN 1 AND 10)
    );
