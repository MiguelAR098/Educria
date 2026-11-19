-- psql -X -v ON_ERROR_STOP=1 -f src/test/sql/verificar_v2.sql DATABASE
\set ON_ERROR_STOP on
BEGIN;
CREATE SCHEMA educria_teste_v2;
SET LOCAL search_path TO educria_teste_v2;

\ir ../../main/resources/db/migration/V1__create_schema.sql

INSERT INTO usuarios (nome, email, senha, perfil)
VALUES ('Legado', 'legado@example.test', 'teste', 'ALUNO');
INSERT INTO alunos (usuario_id) SELECT id FROM usuarios;
INSERT INTO desempenho_adaptativo
    (aluno_id, nivel_atual, erros_consecutivos, total_acertos, total_erros)
SELECT usuario_id, 7, 1, 21, 8 FROM alunos;
CREATE TEMP TABLE global_antes AS SELECT * FROM desempenho_adaptativo;

\ir ../../main/resources/db/migration/V2__desempenho_por_assunto.sql

INSERT INTO usuarios (nome, email, senha, perfil)
VALUES ('Novo', 'novo@example.test', 'teste', 'ALUNO');
INSERT INTO alunos (usuario_id) SELECT id FROM usuarios WHERE nome = 'Novo';

DO $$
DECLARE
    aluno_legado bigint := (SELECT id FROM usuarios WHERE nome = 'Legado');
    fracoes bigint := (SELECT id FROM assuntos WHERE nome = 'frações');
BEGIN
    IF (SELECT count(*) FROM assuntos s JOIN materias m ON m.id = s.materia_id
        WHERE m.nome = 'Matemática') <> 8 THEN
        RAISE EXCEPTION 'Esperados oito assuntos de Matemática';
    END IF;

    IF EXISTS (SELECT * FROM global_antes EXCEPT SELECT * FROM desempenho_adaptativo)
       OR EXISTS (SELECT * FROM desempenho_adaptativo EXCEPT SELECT * FROM global_antes) THEN
        RAISE EXCEPTION 'O registro global mudou';
    END IF;

    IF EXISTS (SELECT 1 FROM alunos a CROSS JOIN assuntos s
               LEFT JOIN desempenho_por_assunto d
                  ON d.aluno_id = a.usuario_id AND d.assunto_id = s.id
               WHERE d.id IS NULL OR d.nivel_atual <> 1 OR d.total_acertos <> 0
                  OR d.total_erros <> 0 OR d.erros_consecutivos <> 0) THEN
        RAISE EXCEPTION 'Falhou a inicialização por aluno e assunto';
    END IF;

    BEGIN
        INSERT INTO desempenho_por_assunto (aluno_id, assunto_id) VALUES (aluno_legado, fracoes);
        RAISE EXCEPTION 'O banco aceitou o par duplicado';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;

    BEGIN
        UPDATE desempenho_por_assunto SET nivel_atual = 0 WHERE assunto_id = fracoes;
        RAISE EXCEPTION 'O banco aceitou nível 0';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        UPDATE desempenho_por_assunto SET nivel_atual = 11 WHERE assunto_id = fracoes;
        RAISE EXCEPTION 'O banco aceitou nível 11';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    UPDATE desempenho_por_assunto SET nivel_atual = 10
    WHERE aluno_id = aluno_legado AND assunto_id = fracoes;
    IF (SELECT count(*) FROM desempenho_por_assunto WHERE nivel_atual = 1) <> 15
       OR (SELECT count(*) FROM desempenho_por_assunto WHERE nivel_atual = 10) <> 1 THEN
        RAISE EXCEPTION 'Falhou o isolamento entre alunos ou assuntos';
    END IF;
END;
$$;
ROLLBACK;
\echo Verificação da V2 concluída.
