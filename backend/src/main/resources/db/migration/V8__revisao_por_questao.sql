-- A agenda legada permanece vinculada à atividade, sem inferir qual questão foi revisada.
ALTER TABLE repeticao_espacada
    ALTER COLUMN atividade_id DROP NOT NULL,
    ADD COLUMN questao_id BIGINT REFERENCES questoes(id) ON DELETE CASCADE,
    ADD CONSTRAINT ck_repeticao_alvo CHECK (num_nonnulls(atividade_id, questao_id) = 1),
    ADD CONSTRAINT uk_repeticao_aluno_questao UNIQUE (aluno_id, questao_id);

-- A cota da seleção conta revisões no momento da apresentação, inclusive após retomada.
ALTER TABLE sessoes_estudo ADD COLUMN revisoes_apresentadas INTEGER NOT NULL DEFAULT 0
    CHECK (revisoes_apresentadas BETWEEN 0 AND 3);
