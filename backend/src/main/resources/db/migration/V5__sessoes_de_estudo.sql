CREATE TABLE sessoes_estudo (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL REFERENCES alunos(usuario_id) ON DELETE CASCADE,
    assunto_id BIGINT NOT NULL REFERENCES assuntos(id),
    situacao VARCHAR(16) NOT NULL CHECK (situacao IN ('ATIVA', 'INTERROMPIDA', 'ENCERRADA')),
    iniciada_em TIMESTAMPTZ NOT NULL,
    atualizada_em TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX uk_sessao_aberta_por_aluno ON sessoes_estudo (aluno_id)
    WHERE situacao IN ('ATIVA', 'INTERROMPIDA');
CREATE INDEX idx_sessoes_aluno_id ON sessoes_estudo (aluno_id, id DESC);

CREATE TABLE sessao_questoes (
    sessao_id BIGINT NOT NULL REFERENCES sessoes_estudo(id) ON DELETE CASCADE,
    indice INTEGER NOT NULL CHECK (indice BETWEEN 0 AND 9),
    questao_id BIGINT NOT NULL REFERENCES questoes(id),
    PRIMARY KEY (sessao_id, indice),
    UNIQUE (sessao_id, questao_id)
);
