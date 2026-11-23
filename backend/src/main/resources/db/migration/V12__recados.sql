-- =====================================================
-- EDUCRIA - Recados do professor para o aluno (Flyway V12)
-- =====================================================

CREATE TABLE recados (
    id              BIGSERIAL PRIMARY KEY,
    aluno_id        BIGINT NOT NULL REFERENCES alunos(usuario_id) ON DELETE CASCADE,
    professor_id    BIGINT REFERENCES professores(usuario_id) ON DELETE SET NULL,
    texto           VARCHAR(500) NOT NULL,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lido_em         TIMESTAMP
);

CREATE INDEX idx_recados_aluno ON recados(aluno_id, criado_em DESC);
