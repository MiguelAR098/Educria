-- Upgrade de uma cópia de esquema V8 com sessão interrompida e revisão já agendada.
\set ON_ERROR_STOP on
BEGIN;
CREATE SCHEMA teste_reaprendizagem_v9;
SET LOCAL search_path TO teste_reaprendizagem_v9;
\ir ../../main/resources/db/migration/V1__create_schema.sql
\ir ../../main/resources/db/migration/V2__desempenho_por_assunto.sql
\ir ../../main/resources/db/migration/V3__metadados_de_questoes.sql
\ir ../../main/resources/db/migration/V4__materias_do_catalogo.sql
\ir ../../main/resources/db/migration/V5__sessoes_de_estudo.sql
\ir ../../main/resources/db/migration/V6__resposta_da_sessao.sql
\ir ../../main/resources/db/migration/V7__niveis_da_resposta.sql
\ir ../../main/resources/db/migration/V8__revisao_por_questao.sql

INSERT INTO usuarios(id,nome,email,senha,perfil) VALUES (1,'Teste','legado@example.test','teste','ALUNO');
INSERT INTO alunos(usuario_id) VALUES (1);
INSERT INTO atividades(id,titulo) VALUES (1,'Teste');
INSERT INTO questoes(id,atividade_id,enunciado,resposta_correta) VALUES (1,1,'Q1','A'),(2,1,'Q2','B');
INSERT INTO sessoes_estudo(id,aluno_id,assunto_id,situacao,iniciada_em,atualizada_em)
SELECT 1,1,min(id),'INTERROMPIDA',now(),now() FROM assuntos;
INSERT INTO sessao_questoes VALUES (1,0,1),(1,1,2);
INSERT INTO respostas_aluno(aluno_id,sessao_id,questao_id,resposta_dada,correta) VALUES (1,1,1,'B',false);
INSERT INTO repeticao_espacada(aluno_id,questao_id,proxima_revisao) VALUES (1,1,'2027-01-15');
\ir ../../main/resources/db/migration/V9__tentativas_da_sessao.sql

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sessoes_estudo WHERE id=1 AND passo=2 AND situacao='INTERROMPIDA')
       OR NOT EXISTS (SELECT 1 FROM respostas_aluno WHERE sessao_id=1 AND passo=1 AND NOT reaprender AND NOT correta)
       OR NOT EXISTS (SELECT 1 FROM repeticao_espacada WHERE questao_id=1 AND proxima_revisao='2027-01-15')
    THEN RAISE EXCEPTION 'Estado legado alterado na migração'; END IF;
END $$;
ROLLBACK;
