-- SQL Server 2019+. Linha de migrations independente do PostgreSQL.
CREATE TABLE dbo.usuarios (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    email nvarchar(100) NOT NULL UNIQUE,
    nome nvarchar(100) NOT NULL,
    senha nvarchar(255) NOT NULL,
    perfil nvarchar(20) NOT NULL CHECK (perfil IN (N'ALUNO',N'PROFESSOR',N'ADMIN')),
    ativo bit NOT NULL DEFAULT 1,
    criado_em datetime2(6) NOT NULL DEFAULT CONVERT(datetime2(6), SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time'),
    atualizado_em datetime2(6)
);
CREATE TABLE dbo.turmas (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    nome nvarchar(100) NOT NULL,
    codigo nvarchar(20),
    ano_letivo int,
    criado_em datetime2(6) NOT NULL DEFAULT CONVERT(datetime2(6), SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time')
);
CREATE TABLE dbo.materias (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    nome nvarchar(100) NOT NULL UNIQUE
);
CREATE TABLE dbo.assuntos (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    materia_id bigint NOT NULL REFERENCES dbo.materias(id),
    nome nvarchar(100) NOT NULL,
    UNIQUE (materia_id, nome)
);
CREATE TABLE dbo.alunos (
    usuario_id bigint PRIMARY KEY REFERENCES dbo.usuarios(id) ON DELETE CASCADE,
    matricula nvarchar(30),
    turma_id bigint REFERENCES dbo.turmas(id)
);
CREATE TABLE dbo.admins (
    usuario_id bigint PRIMARY KEY REFERENCES dbo.usuarios(id) ON DELETE CASCADE
);
CREATE TABLE dbo.professores (
    usuario_id bigint PRIMARY KEY REFERENCES dbo.usuarios(id) ON DELETE CASCADE,
    materia_id bigint NOT NULL REFERENCES dbo.materias(id)
);
CREATE TABLE dbo.professor_turmas (
    professor_id bigint NOT NULL REFERENCES dbo.professores(usuario_id) ON DELETE CASCADE,
    turma_id bigint NOT NULL REFERENCES dbo.turmas(id) ON DELETE CASCADE,
    PRIMARY KEY (professor_id, turma_id)
);

-- Desempenho privado por aluno e pontuação coletiva por turma.
CREATE TABLE dbo.desempenho_adaptativo (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    aluno_id bigint NOT NULL UNIQUE REFERENCES dbo.alunos(usuario_id) ON DELETE CASCADE,
    nivel_atual int NOT NULL DEFAULT 1 CHECK (nivel_atual BETWEEN 1 AND 10),
    erros_consecutivos int NOT NULL DEFAULT 0,
    total_acertos int NOT NULL DEFAULT 0,
    total_erros int NOT NULL DEFAULT 0,
    atualizado_em datetime2(6)
);
CREATE TABLE dbo.desempenho_por_assunto (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    aluno_id bigint NOT NULL REFERENCES dbo.alunos(usuario_id) ON DELETE CASCADE,
    assunto_id bigint NOT NULL REFERENCES dbo.assuntos(id),
    nivel_atual int NOT NULL DEFAULT 1 CHECK (nivel_atual BETWEEN 1 AND 10),
    erros_consecutivos int NOT NULL DEFAULT 0 CHECK (erros_consecutivos >= 0),
    total_acertos int NOT NULL DEFAULT 0 CHECK (total_acertos >= 0),
    total_erros int NOT NULL DEFAULT 0 CHECK (total_erros >= 0),
    versao bigint NOT NULL DEFAULT 0,
    UNIQUE (aluno_id, assunto_id)
);
CREATE TABLE dbo.pontuacao_coletiva (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    turma_id bigint NOT NULL UNIQUE REFERENCES dbo.turmas(id) ON DELETE CASCADE,
    pontos_totais int NOT NULL DEFAULT 0,
    atualizado_em datetime2(6)
);
CREATE TABLE dbo.atividades (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    titulo nvarchar(200) NOT NULL,
    enunciado nvarchar(max),
    nivel_recomendado int,
    ativa bit NOT NULL DEFAULT 1,
    criado_em datetime2(6) NOT NULL DEFAULT CONVERT(datetime2(6), SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time')
);
CREATE TABLE dbo.questoes (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    atividade_id bigint NOT NULL REFERENCES dbo.atividades(id) ON DELETE CASCADE,
    enunciado nvarchar(max) NOT NULL,
    alternativa_a nvarchar(500),
    alternativa_b nvarchar(500),
    alternativa_c nvarchar(500),
    alternativa_d nvarchar(500),
    resposta_correta nvarchar(1) NOT NULL,
    ordem int,
    assunto_id bigint REFERENCES dbo.assuntos(id),
    dificuldade int CHECK (dificuldade BETWEEN 1 AND 10),
    explicacao nvarchar(max),
    ativa bit NOT NULL DEFAULT 1,
    autor_id bigint REFERENCES dbo.professores(usuario_id) ON DELETE SET NULL
);
CREATE TABLE dbo.sessoes_estudo (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    aluno_id bigint NOT NULL REFERENCES dbo.alunos(usuario_id) ON DELETE CASCADE,
    assunto_id bigint NOT NULL REFERENCES dbo.assuntos(id),
    situacao nvarchar(16) NOT NULL CHECK (situacao IN (N'ATIVA',N'INTERROMPIDA',N'ENCERRADA')),
    iniciada_em datetimeoffset(6) NOT NULL,
    atualizada_em datetimeoffset(6) NOT NULL,
    revisoes_apresentadas int NOT NULL DEFAULT 0 CHECK (revisoes_apresentadas BETWEEN 0 AND 3),
    passo int NOT NULL DEFAULT 1 CHECK (passo > 0),
    questao_retomada_id bigint,
    CONSTRAINT uk_sessao_titular UNIQUE (id, aluno_id)
);
CREATE TABLE dbo.sessao_questoes (
    sessao_id bigint NOT NULL REFERENCES dbo.sessoes_estudo(id) ON DELETE CASCADE,
    indice int NOT NULL CHECK (indice BETWEEN 0 AND 9),
    questao_id bigint NOT NULL REFERENCES dbo.questoes(id),
    PRIMARY KEY (sessao_id, indice),
    UNIQUE (sessao_id, questao_id)
);
-- A sessão entra com retomada nula. Os passos seguintes referenciam uma questão já apresentada.
ALTER TABLE dbo.sessoes_estudo ADD CONSTRAINT fk_sessao_questao_retomada
    FOREIGN KEY (id, questao_retomada_id) REFERENCES dbo.sessao_questoes(sessao_id, questao_id);

CREATE TABLE dbo.repeticao_espacada (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    aluno_id bigint NOT NULL REFERENCES dbo.alunos(usuario_id) ON DELETE CASCADE,
    atividade_id bigint REFERENCES dbo.atividades(id),
    intervalo_atual nvarchar(10) NOT NULL DEFAULT N'D1'
        CHECK (intervalo_atual IN (N'D1',N'D3',N'D7',N'D16',N'D35',N'D70')),
    proxima_revisao date NOT NULL,
    ultima_revisao date,
    atualizado_em datetime2(6),
    questao_id bigint REFERENCES dbo.questoes(id) ON DELETE CASCADE,
    CONSTRAINT ck_repeticao_alvo CHECK (
        (atividade_id IS NULL AND questao_id IS NOT NULL) OR (atividade_id IS NOT NULL AND questao_id IS NULL)
    )
);
CREATE TABLE dbo.respostas_aluno (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    aluno_id bigint NOT NULL REFERENCES dbo.alunos(usuario_id) ON DELETE CASCADE,
    questao_id bigint NOT NULL REFERENCES dbo.questoes(id) ON DELETE CASCADE,
    resposta_dada nvarchar(1),
    correta bit NOT NULL,
    respondido_em datetime2(6) NOT NULL DEFAULT CONVERT(datetime2(6), SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time'),
    sessao_id bigint,
    nivel_anterior int,
    nivel_atual int,
    passo int,
    reaprender bit NOT NULL DEFAULT 0,
    CONSTRAINT fk_resposta_questao_apresentada FOREIGN KEY (sessao_id, questao_id)
        REFERENCES dbo.sessao_questoes(sessao_id, questao_id),
    CONSTRAINT fk_resposta_sessao_titular FOREIGN KEY (sessao_id, aluno_id)
        REFERENCES dbo.sessoes_estudo(id, aluno_id),
    CONSTRAINT ck_alternativa_da_sessao CHECK (
        sessao_id IS NULL OR (resposta_dada IS NOT NULL AND resposta_dada IN (N'A',N'B',N'C',N'D'))
    ),
    CONSTRAINT ck_niveis_da_resposta CHECK (
        (nivel_anterior IS NULL AND nivel_atual IS NULL) OR
        (sessao_id IS NOT NULL AND nivel_anterior IS NOT NULL AND nivel_atual IS NOT NULL
            AND nivel_anterior BETWEEN 1 AND 10 AND nivel_atual BETWEEN 1 AND 10)
    ),
    CONSTRAINT ck_resposta_passo CHECK ((sessao_id IS NULL AND passo IS NULL) OR
        (sessao_id IS NOT NULL AND passo IS NOT NULL AND passo > 0)),
    CONSTRAINT ck_resposta_reaprender CHECK (reaprender = 0 OR (sessao_id IS NOT NULL AND correta = 0))
);
CREATE TABLE dbo.atividades_coletivas (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    turma_id bigint NOT NULL REFERENCES dbo.turmas(id) ON DELETE CASCADE,
    professor_id bigint REFERENCES dbo.professores(usuario_id) ON DELETE SET NULL,
    tipo nvarchar(20) NOT NULL,
    titulo nvarchar(120) NOT NULL,
    pontos int NOT NULL CHECK (pontos IN (20,40,60,80)),
    resumo nvarchar(300),
    criado_em datetime2(6) NOT NULL DEFAULT CONVERT(datetime2(6), SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time')
);
CREATE TABLE dbo.recados (
    id bigint IDENTITY(1000,10) PRIMARY KEY,
    aluno_id bigint NOT NULL REFERENCES dbo.alunos(usuario_id) ON DELETE CASCADE,
    professor_id bigint REFERENCES dbo.professores(usuario_id),
    texto nvarchar(500) NOT NULL,
    criado_em datetime2(6) NOT NULL DEFAULT CONVERT(datetime2(6), SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time'),
    lido_em datetime2(6)
);

-- Índices filtrados preservam a semântica PostgreSQL para chaves opcionais.
CREATE UNIQUE INDEX uk_sessao_aberta_por_aluno ON dbo.sessoes_estudo(aluno_id)
    WHERE situacao IN (N'ATIVA',N'INTERROMPIDA');
CREATE UNIQUE INDEX uk_resposta_sessao_passo ON dbo.respostas_aluno(sessao_id, passo) WHERE sessao_id IS NOT NULL;
CREATE UNIQUE INDEX uk_repeticao_aluno_atividade ON dbo.repeticao_espacada(aluno_id, atividade_id)
    WHERE atividade_id IS NOT NULL;
CREATE UNIQUE INDEX uk_repeticao_aluno_questao ON dbo.repeticao_espacada(aluno_id, questao_id)
    WHERE questao_id IS NOT NULL;
CREATE INDEX idx_sessoes_aluno_id ON dbo.sessoes_estudo(aluno_id,id DESC);
CREATE INDEX idx_alunos_turma ON dbo.alunos(turma_id);
CREATE INDEX idx_repeticao_proxima ON dbo.repeticao_espacada(proxima_revisao);
CREATE INDEX idx_respostas_aluno_data ON dbo.respostas_aluno(aluno_id, respondido_em, id);
CREATE INDEX idx_respostas_erros ON dbo.respostas_aluno(aluno_id, questao_id, respondido_em, id)
    INCLUDE (sessao_id) WHERE correta = 0;
CREATE INDEX idx_professor_turmas_turma ON dbo.professor_turmas(turma_id);
CREATE INDEX idx_atividades_coletivas_turma ON dbo.atividades_coletivas(turma_id, criado_em DESC);
CREATE INDEX idx_recados_aluno ON dbo.recados(aluno_id, criado_em DESC);
