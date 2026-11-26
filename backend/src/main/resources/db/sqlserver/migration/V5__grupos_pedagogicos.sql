CREATE TABLE dbo.grupos_pedagogicos (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    professor_id bigint NOT NULL,
    nome nvarchar(120) NOT NULL,
    descricao nvarchar(1000),
    tipo nvarchar(10) NOT NULL,
    criado_em datetimeoffset(6) NOT NULL,
    arquivado_em datetimeoffset(6),
    CONSTRAINT fk_grupo_professor FOREIGN KEY (professor_id) REFERENCES dbo.professores(usuario_id),
    CONSTRAINT ck_grupo_nome CHECK (LEN(LTRIM(RTRIM(nome))) > 0),
    CONSTRAINT ck_grupo_tipo CHECK (tipo IN (N'GRUPO', N'PROJETO')),
    CONSTRAINT ck_grupo_datas CHECK (arquivado_em >= criado_em)
);

CREATE TABLE dbo.grupo_turmas (
    grupo_id bigint NOT NULL,
    turma_id bigint NOT NULL,
    CONSTRAINT pk_grupo_turmas PRIMARY KEY (grupo_id, turma_id),
    CONSTRAINT fk_grupo_turma_grupo FOREIGN KEY (grupo_id) REFERENCES dbo.grupos_pedagogicos(id),
    CONSTRAINT fk_grupo_turma_turma FOREIGN KEY (turma_id) REFERENCES dbo.turmas(id)
);

CREATE TABLE dbo.grupo_participacoes (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    grupo_id bigint NOT NULL,
    aluno_id bigint NOT NULL,
    turma_origem_id bigint NOT NULL,
    entrada_em datetimeoffset(6) NOT NULL,
    saida_em datetimeoffset(6),
    CONSTRAINT fk_participacao_contexto FOREIGN KEY (grupo_id, turma_origem_id)
        REFERENCES dbo.grupo_turmas(grupo_id, turma_id),
    CONSTRAINT fk_participacao_aluno FOREIGN KEY (aluno_id) REFERENCES dbo.alunos(usuario_id),
    CONSTRAINT ck_participacao_datas CHECK (saida_em >= entrada_em)
);

CREATE INDEX idx_grupos_professor ON dbo.grupos_pedagogicos (professor_id, id);
CREATE INDEX idx_grupo_turmas_turma ON dbo.grupo_turmas (turma_id, grupo_id);
CREATE INDEX idx_grupo_participacoes_aluno ON dbo.grupo_participacoes (aluno_id, grupo_id);
CREATE UNIQUE INDEX ux_grupo_participacao_ativa ON dbo.grupo_participacoes (grupo_id, aluno_id)
    INCLUDE (saida_em) WHERE saida_em IS NULL;
