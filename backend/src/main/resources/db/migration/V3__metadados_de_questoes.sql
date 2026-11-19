-- Questões anteriores à V3 podem continuar sem classificação.
ALTER TABLE questoes
    ADD COLUMN assunto_id BIGINT REFERENCES assuntos(id),
    ADD COLUMN dificuldade INTEGER CHECK (dificuldade BETWEEN 1 AND 10),
    ADD COLUMN explicacao TEXT;
