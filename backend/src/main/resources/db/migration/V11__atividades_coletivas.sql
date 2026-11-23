-- =====================================================
-- EDUCRIA - Atividades coletivas lançadas por professores (Flyway V11)
-- Cada atividade soma pontos à pontuação coletiva da turma (pública),
-- sem relação com o desempenho individual dos alunos.
-- =====================================================

CREATE TABLE atividades_coletivas (
    id              BIGSERIAL PRIMARY KEY,
    turma_id        BIGINT NOT NULL REFERENCES turmas(id) ON DELETE CASCADE,
    professor_id    BIGINT REFERENCES professores(usuario_id) ON DELETE SET NULL,
    tipo            VARCHAR(20)  NOT NULL,
    titulo          VARCHAR(120) NOT NULL,
    pontos          INTEGER      NOT NULL CHECK (pontos IN (20, 40, 60, 80)),
    resumo          VARCHAR(300),
    criado_em       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_atividades_coletivas_turma ON atividades_coletivas(turma_id, criado_em DESC);
