IF DB_NAME() NOT LIKE N'educria[_]sqlserver[_]teste[_]%'
    OR NOT EXISTS (SELECT 1 FROM sys.extended_properties
                   WHERE class=0 AND name=N'EducriaAmbiente' AND CONVERT(nvarchar(30),value)=N'descartavel')
    THROW 51020, N'A prova exige banco descartável identificado.', 1;

-- A transação contém somente registros de prova; os dados da demonstração ficam intactos.
-- XACT_ABORT OFF permite capturar as violações esperadas e prosseguir na
-- mesma transação. ROLLBACK desfaz os registros, mas não recupera valores de IDENTITY.
SET XACT_ABORT OFF;
BEGIN TRANSACTION;
BEGIN TRY
    INSERT dbo.turmas(nome) VALUES (N'Prova temporária de cascata');
    DECLARE @turma bigint = SCOPE_IDENTITY();
    INSERT dbo.pontuacao_coletiva(turma_id) VALUES (@turma);
    DELETE FROM dbo.turmas WHERE id=@turma;
    IF EXISTS (SELECT 1 FROM dbo.pontuacao_coletiva WHERE turma_id=@turma)
        THROW 51021, N'A cascata não removeu a pontuação da turma temporária.', 1;

    DECLARE @aluno bigint = (SELECT id FROM dbo.usuarios WHERE email=N'aluno1@educria.example.test');
    DECLARE @outro bigint = (SELECT id FROM dbo.usuarios WHERE email=N'aluno2@educria.example.test');
    DECLARE @sessao bigint = (SELECT MIN(id) FROM dbo.sessoes_estudo WHERE aluno_id=@aluno);
    DECLARE @questao bigint = (SELECT questao_id FROM dbo.sessao_questoes WHERE sessao_id=@sessao AND indice=0);
    DECLARE @assunto bigint = (SELECT assunto_id FROM dbo.sessoes_estudo WHERE id=@sessao);
    -- Se a operação inválida for aceita, o THROW da prova marca a falha.
    -- O erro 547 é a rejeição esperada para estes casos de FK ou CHECK.
    BEGIN TRY
        INSERT dbo.respostas_aluno(aluno_id,questao_id,sessao_id,passo,resposta_dada,correta)
        VALUES (@outro,@questao,@sessao,3,N'A',1);
        THROW 51022, N'A FK deveria rejeitar a resposta de outro titular.', 1;
    END TRY
    BEGIN CATCH
        IF ERROR_NUMBER() <> 547 THROW;
    END CATCH;
    BEGIN TRY
        INSERT dbo.respostas_aluno(aluno_id,questao_id,sessao_id,passo,resposta_dada,correta)
        VALUES (@aluno,@questao,@sessao,0,N'A',1);
        THROW 51023, N'O CHECK deveria rejeitar o passo zero.', 1;
    END TRY
    BEGIN CATCH
        IF ERROR_NUMBER() <> 547 THROW;
    END CATCH;

    INSERT dbo.sessoes_estudo(aluno_id,assunto_id,situacao,iniciada_em,atualizada_em)
    VALUES (@aluno,@assunto,N'ATIVA',SYSDATETIMEOFFSET(),SYSDATETIMEOFFSET());
    -- ATIVA e INTERROMPIDA estão no filtro do índice único. Os erros 2601 e
    -- 2627 identificam a tentativa rejeitada por duplicidade.
    BEGIN TRY
        INSERT dbo.sessoes_estudo(aluno_id,assunto_id,situacao,iniciada_em,atualizada_em)
        VALUES (@aluno,@assunto,N'INTERROMPIDA',SYSDATETIMEOFFSET(),SYSDATETIMEOFFSET());
        THROW 51024, N'O índice filtrado deveria rejeitar a segunda sessão aberta.', 1;
    END TRY
    BEGIN CATCH
        IF ERROR_NUMBER() NOT IN (2601,2627) THROW;
    END CATCH;

    DECLARE @hash nvarchar(255) = (SELECT senha FROM dbo.usuarios WHERE id=@aluno);
    INSERT dbo.usuarios(email,nome,senha,perfil)
    VALUES (CONCAT(N'prova-aluno-',NEWID(),N'@educria.example.test'),N'Aluno temporário',@hash,N'ALUNO');
    DECLARE @alunoProva bigint = SCOPE_IDENTITY();
    INSERT dbo.alunos(usuario_id) VALUES (@alunoProva);
    INSERT dbo.sessoes_estudo(aluno_id,assunto_id,situacao,iniciada_em,atualizada_em)
    VALUES (@alunoProva,@assunto,N'ATIVA',SYSDATETIMEOFFSET(),SYSDATETIMEOFFSET());
    DECLARE @sessaoProva bigint = SCOPE_IDENTITY();
    -- A FK de retomada é validada no UPDATE. Inserir a apresentação antes
    -- permite gravar a referência, como mostra a segunda tentativa abaixo.
    BEGIN TRY
        UPDATE dbo.sessoes_estudo SET questao_retomada_id=@questao WHERE id=@sessaoProva;
        THROW 51025, N'A FK imediata deveria rejeitar a retomada antes da apresentação.', 1;
    END TRY
    BEGIN CATCH
        IF ERROR_NUMBER() <> 547 THROW;
    END CATCH;
    INSERT dbo.sessao_questoes(sessao_id,indice,questao_id) VALUES (@sessaoProva,0,@questao);
    UPDATE dbo.sessoes_estudo SET questao_retomada_id=@questao WHERE id=@sessaoProva;
    INSERT dbo.respostas_aluno(aluno_id,questao_id,sessao_id,passo,resposta_dada,correta)
    VALUES (@alunoProva,@questao,@sessaoProva,1,N'A',1);
    BEGIN TRY
        INSERT dbo.respostas_aluno(aluno_id,questao_id,sessao_id,passo,resposta_dada,correta)
        VALUES (@alunoProva,@questao,@sessaoProva,1,N'B',0);
        THROW 51031, N'O índice filtrado deveria rejeitar outra resposta para o mesmo passo.', 1;
    END TRY
    BEGIN CATCH
        IF ERROR_NUMBER() NOT IN (2601,2627) THROW;
    END CATCH;
    BEGIN TRY
        DELETE FROM dbo.sessoes_estudo WHERE id=@sessaoProva;
        THROW 51026, N'Uma sessão com resposta deveria resistir à exclusão direta.', 1;
    END TRY
    BEGIN CATCH
        IF ERROR_NUMBER() <> 547 THROW;
    END CATCH;

    INSERT dbo.atividades(titulo) VALUES (N'Atividade temporária');
    DECLARE @atividadeProva bigint = SCOPE_IDENTITY();
    INSERT dbo.repeticao_espacada(aluno_id,atividade_id,proxima_revisao)
    VALUES (@alunoProva,@atividadeProva,CONVERT(date,SYSDATETIME()));
    BEGIN TRY
        DELETE FROM dbo.atividades WHERE id=@atividadeProva;
        THROW 51027, N'Uma atividade referenciada pela agenda legada deveria resistir à exclusão direta.', 1;
    END TRY
    BEGIN CATCH
        IF ERROR_NUMBER() <> 547 THROW;
    END CATCH;

    INSERT dbo.usuarios(email,nome,senha,perfil)
    VALUES (CONCAT(N'prova-professor-',NEWID(),N'@educria.example.test'),N'Professor temporário',@hash,N'PROFESSOR');
    DECLARE @professorProva bigint = SCOPE_IDENTITY();
    INSERT dbo.professores(usuario_id,materia_id)
    SELECT @professorProva,materia_id FROM dbo.assuntos WHERE id=@assunto;
    INSERT dbo.recados(aluno_id,professor_id,texto) VALUES (@alunoProva,@professorProva,N'Recado temporário');
    BEGIN TRY
        DELETE FROM dbo.professores WHERE usuario_id=@professorProva;
        THROW 51028, N'Um professor com recado deveria resistir à exclusão direta.', 1;
    END TRY
    BEGIN CATCH
        IF ERROR_NUMBER() <> 547 THROW;
    END CATCH;

    -- A exclusão do aluno dispara as cascatas das relações dependentes. Por
    -- exemplo, as questões apresentadas saem pela relação com a sessão.
    DELETE FROM dbo.alunos WHERE usuario_id=@alunoProva;
    IF EXISTS (SELECT 1 FROM dbo.sessoes_estudo WHERE aluno_id=@alunoProva)
       OR EXISTS (SELECT 1 FROM dbo.sessao_questoes WHERE sessao_id=@sessaoProva)
       OR EXISTS (SELECT 1 FROM dbo.respostas_aluno WHERE aluno_id=@alunoProva)
       OR EXISTS (SELECT 1 FROM dbo.repeticao_espacada WHERE aluno_id=@alunoProva)
       OR EXISTS (SELECT 1 FROM dbo.recados WHERE aluno_id=@alunoProva)
        THROW 51029, N'A cascata do aluno de prova deveria remover seus dados dependentes.', 1;
    ROLLBACK;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
SELECT N'Cascata, FK composta, CHECK e unicidade de sessão aberta e de passo' AS verificacao, N'OK' AS resultado;
SELECT N'FK imediata, exclusões protegidas e cascata de dados do aluno' AS verificacao, N'OK' AS resultado;
