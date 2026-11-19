-- Respostas anteriores às sessões conservam sessao_id nulo.
ALTER TABLE respostas_aluno
    ADD COLUMN sessao_id BIGINT,
    ADD CONSTRAINT fk_resposta_questao_apresentada
        FOREIGN KEY (sessao_id, questao_id) REFERENCES sessao_questoes(sessao_id, questao_id) ON DELETE CASCADE,
    ADD CONSTRAINT uk_resposta_da_sessao UNIQUE (sessao_id, questao_id),
    ADD CONSTRAINT ck_alternativa_da_sessao CHECK
        (sessao_id IS NULL OR (resposta_dada IS NOT NULL AND resposta_dada IN ('A', 'B', 'C', 'D')));
