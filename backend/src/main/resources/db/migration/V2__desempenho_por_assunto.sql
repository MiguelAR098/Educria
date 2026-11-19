CREATE TABLE materias (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE assuntos (
    id BIGSERIAL PRIMARY KEY,
    materia_id BIGINT NOT NULL REFERENCES materias(id),
    nome VARCHAR(100) NOT NULL,
    UNIQUE (materia_id, nome)
);

CREATE TABLE desempenho_por_assunto (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL REFERENCES alunos(usuario_id) ON DELETE CASCADE,
    assunto_id BIGINT NOT NULL REFERENCES assuntos(id),
    nivel_atual INTEGER NOT NULL DEFAULT 1 CHECK (nivel_atual BETWEEN 1 AND 10),
    erros_consecutivos INTEGER NOT NULL DEFAULT 0 CHECK (erros_consecutivos >= 0),
    total_acertos INTEGER NOT NULL DEFAULT 0 CHECK (total_acertos >= 0),
    total_erros INTEGER NOT NULL DEFAULT 0 CHECK (total_erros >= 0),
    versao BIGINT NOT NULL DEFAULT 0,
    UNIQUE (aluno_id, assunto_id)
);

INSERT INTO materias (nome) VALUES ('Matemática');

INSERT INTO assuntos (materia_id, nome)
SELECT m.id, a.nome
FROM materias m
CROSS JOIN (VALUES
    ('somar e subtrair'),
    ('multiplicar e dividir'),
    ('frações'),
    ('porcentagem'),
    ('proporção'),
    ('equações'),
    ('área e perímetro'),
    ('várias etapas')
) AS a(nome)
WHERE m.nome = 'Matemática';

-- Atende inclusive cadastros feitos pela versão anterior da aplicação.
CREATE FUNCTION inicializar_desempenho_por_assunto() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO desempenho_por_assunto (aluno_id, assunto_id)
    SELECT NEW.usuario_id, id FROM assuntos;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_aluno_desempenho_por_assunto
AFTER INSERT ON alunos
FOR EACH ROW EXECUTE FUNCTION inicializar_desempenho_por_assunto();

-- O trigger e a carga são instalados na mesma transação Flyway.
INSERT INTO desempenho_por_assunto (aluno_id, assunto_id)
SELECT a.usuario_id, s.id FROM alunos a CROSS JOIN assuntos s;
