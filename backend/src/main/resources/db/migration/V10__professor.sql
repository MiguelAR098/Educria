-- =====================================================
-- EDUCRIA - Perfil PROFESSOR (Flyway V10)
-- Professor leciona uma matéria do catálogo em uma ou mais turmas.
-- =====================================================

CREATE TABLE professores (
    usuario_id      BIGINT PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    materia_id      BIGINT NOT NULL REFERENCES materias(id)
);

CREATE TABLE professor_turmas (
    professor_id    BIGINT NOT NULL REFERENCES professores(usuario_id) ON DELETE CASCADE,
    turma_id        BIGINT NOT NULL REFERENCES turmas(id) ON DELETE CASCADE,
    PRIMARY KEY (professor_id, turma_id)
);

CREATE INDEX idx_professor_turmas_turma ON professor_turmas(turma_id);
