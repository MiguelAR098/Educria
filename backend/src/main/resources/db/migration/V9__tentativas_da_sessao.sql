-- V5 continua limitando as dez questões distintas. O passo conta apresentações.
ALTER TABLE sessoes_estudo
    ADD COLUMN passo INTEGER NOT NULL DEFAULT 1 CHECK (passo > 0),
    ADD COLUMN questao_retomada_id BIGINT,
    ADD FOREIGN KEY (id, questao_retomada_id) REFERENCES sessao_questoes(sessao_id, questao_id)
        DEFERRABLE INITIALLY DEFERRED;
UPDATE sessoes_estudo s SET passo = ult.passo
FROM (SELECT sessao_id, max(indice) + 1 AS passo FROM sessao_questoes GROUP BY sessao_id) ult
WHERE s.id = ult.sessao_id;

ALTER TABLE respostas_aluno ADD COLUMN passo INTEGER,
    ADD COLUMN reaprender BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE respostas_aluno r SET passo = sq.indice + 1 FROM sessao_questoes sq
WHERE sq.sessao_id = r.sessao_id AND sq.questao_id = r.questao_id;
ALTER TABLE respostas_aluno DROP CONSTRAINT uk_resposta_da_sessao,
    ADD UNIQUE (sessao_id, passo),
    ADD CHECK ((sessao_id IS NULL AND passo IS NULL) OR
               (sessao_id IS NOT NULL AND passo IS NOT NULL AND passo > 0)),
    ADD CHECK (NOT reaprender OR (sessao_id IS NOT NULL AND NOT correta));
