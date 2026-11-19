-- =====================================================
-- EDUCRIA - Schema inicial (Flyway V1)
-- Isolamento estrito entre desempenho privado e pontos coletivos
-- =====================================================

CREATE TABLE usuarios (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(100) NOT NULL UNIQUE,
    nome            VARCHAR(100) NOT NULL,
    senha           VARCHAR(255) NOT NULL,
    perfil          VARCHAR(20)  NOT NULL,
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP
);

CREATE TABLE turmas (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(100) NOT NULL,
    codigo          VARCHAR(20),
    ano_letivo      INTEGER,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE alunos (
    usuario_id      BIGINT PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    matricula       VARCHAR(30),
    turma_id        BIGINT REFERENCES turmas(id)
);

CREATE TABLE admins (
    usuario_id      BIGINT PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Desempenho individual PRIVADO (sem FK para ranking)
CREATE TABLE desempenho_adaptativo (
    id                      BIGSERIAL PRIMARY KEY,
    aluno_id                BIGINT NOT NULL UNIQUE REFERENCES alunos(usuario_id) ON DELETE CASCADE,
    nivel_atual             INTEGER NOT NULL DEFAULT 1 CHECK (nivel_atual BETWEEN 1 AND 10),
    erros_consecutivos      INTEGER NOT NULL DEFAULT 0,
    total_acertos           INTEGER DEFAULT 0,
    total_erros             INTEGER DEFAULT 0,
    atualizado_em           TIMESTAMP
);

-- Pontuação coletiva PÚBLICA (por turma) - isolada
CREATE TABLE pontuacao_coletiva (
    id                      BIGSERIAL PRIMARY KEY,
    turma_id                BIGINT NOT NULL UNIQUE REFERENCES turmas(id) ON DELETE CASCADE,
    pontos_totais           INTEGER NOT NULL DEFAULT 0,
    atualizado_em           TIMESTAMP
);

CREATE TABLE atividades (
    id                      BIGSERIAL PRIMARY KEY,
    titulo                  VARCHAR(200) NOT NULL,
    enunciado               TEXT,
    nivel_recomendado       INTEGER,
    ativa                   BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE questoes (
    id                      BIGSERIAL PRIMARY KEY,
    atividade_id            BIGINT NOT NULL REFERENCES atividades(id) ON DELETE CASCADE,
    enunciado               TEXT NOT NULL,
    alternativa_a           VARCHAR(500),
    alternativa_b           VARCHAR(500),
    alternativa_c           VARCHAR(500),
    alternativa_d           VARCHAR(500),
    resposta_correta        VARCHAR(1) NOT NULL,
    ordem                   INTEGER
);

CREATE TABLE repeticao_espacada (
    id                      BIGSERIAL PRIMARY KEY,
    aluno_id                BIGINT NOT NULL REFERENCES alunos(usuario_id) ON DELETE CASCADE,
    atividade_id            BIGINT NOT NULL REFERENCES atividades(id) ON DELETE CASCADE,
    intervalo_atual         VARCHAR(10) NOT NULL DEFAULT 'D1',
    proxima_revisao         DATE NOT NULL,
    ultima_revisao          DATE,
    atualizado_em           TIMESTAMP,
    CONSTRAINT uk_aluno_atividade UNIQUE (aluno_id, atividade_id)
);

CREATE TABLE respostas_aluno (
    id                      BIGSERIAL PRIMARY KEY,
    aluno_id                BIGINT NOT NULL REFERENCES alunos(usuario_id) ON DELETE CASCADE,
    questao_id              BIGINT NOT NULL REFERENCES questoes(id) ON DELETE CASCADE,
    resposta_dada           VARCHAR(1),
    correta                 BOOLEAN NOT NULL,
    respondido_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices úteis
CREATE INDEX idx_alunos_turma ON alunos(turma_id);
CREATE INDEX idx_repeticao_proxima ON repeticao_espacada(proxima_revisao);
CREATE INDEX idx_respostas_aluno ON respostas_aluno(aluno_id);
