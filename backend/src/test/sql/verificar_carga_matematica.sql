-- Executar via psql a partir do arquivo, numa base de testes com permissão CREATE.
\set ON_ERROR_STOP on
BEGIN;
CREATE SCHEMA teste_carga_matematica;
SET LOCAL search_path TO teste_carga_matematica;
\ir ../../main/resources/db/migration/V1__create_schema.sql
\ir ../../main/resources/db/migration/V2__desempenho_por_assunto.sql

-- Mesmo título e enunciado da demo, mas pertencem a uma atividade legada.
INSERT INTO atividades (titulo) VALUES ('Demonstração de Matemática do EDUCRIA');
INSERT INTO questoes (atividade_id, ordem, enunciado, resposta_correta)
SELECT id, 1, 'Quanto é 7 + 8?', 'B' FROM atividades;
\ir ../../main/resources/db/migration/V3__metadados_de_questoes.sql

\ir ../../main/resources/db/demo/afterMigrate__questoes_de_matematica.sql
CREATE TEMP TABLE primeira_carga ON COMMIT DROP AS SELECT * FROM questoes;
\ir ../../main/resources/db/demo/afterMigrate__questoes_de_matematica.sql

DO $teste$
BEGIN
    IF (SELECT count(*) FROM atividades) <> 2
        OR (SELECT count(*) FROM questoes WHERE atividade_id = -1) <> 80
        OR (SELECT count(DISTINCT ordem) FROM questoes WHERE atividade_id = -1) <> 80 THEN
        RAISE EXCEPTION 'Esperadas uma atividade demo e 80 questões com ordens distintas';
    END IF;

    IF EXISTS (SELECT * FROM primeira_carga EXCEPT ALL SELECT * FROM questoes)
        OR EXISTS (SELECT * FROM questoes EXCEPT ALL SELECT * FROM primeira_carga) THEN
        RAISE EXCEPTION 'A segunda execução alterou o conteúdo ou os ids das questões';
    END IF;

    IF (SELECT count(*) FROM questoes WHERE atividade_id <> -1 AND assunto_id IS NULL
        AND dificuldade IS NULL AND explicacao IS NULL) <> 1 THEN
        RAISE EXCEPTION 'A questão legada deve permanecer intacta';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM assuntos s JOIN materias m ON s.materia_id = m.id
        CROSS JOIN generate_series(1, 5) AS d(nivel)
        LEFT JOIN questoes q ON q.atividade_id = -1 AND q.assunto_id = s.id
                            AND q.dificuldade = d.nivel
        WHERE m.nome = 'Matemática'
        GROUP BY s.id, d.nivel HAVING count(q.id) <> 2
    ) OR (SELECT count(DISTINCT assunto_id) FROM questoes WHERE atividade_id = -1) <> 8 THEN
        RAISE EXCEPTION 'Esperadas duas questões por dificuldade em cada um dos oito assuntos';
    END IF;
END;
$teste$;
ROLLBACK;
\echo OK: carga repetida, ids estáveis, legado preservado e distribuição 8 x 5 x 2.
