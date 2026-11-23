-- =====================================================
-- EDUCRIA - Banco de questões do professor (Flyway V13)
-- ativa: o professor pode tirar uma questão de circulação sem apagá-la
--        (apagar removeria em cascata as respostas dos alunos).
-- autor_id: professor que cadastrou; nulo para questões da carga inicial.
-- =====================================================

ALTER TABLE questoes
    ADD COLUMN ativa BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN autor_id BIGINT REFERENCES professores(usuario_id) ON DELETE SET NULL;
