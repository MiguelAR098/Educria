package com.educria.service;

import com.educria.domain.NivelAdaptativo;
import com.educria.domain.enums.IntervaloRepeticao;
import com.educria.domain.enums.Perfil;
import com.educria.domain.enums.SituacaoSessao;
import com.educria.domain.enums.TipoAtividadeColetiva;
import com.educria.dto.LancarAtividadeRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("apresentacao")
@DependsOnDatabaseInitialization
@RequiredArgsConstructor
class CargaDaApresentacao implements ApplicationRunner {
    private static final String MARCADOR = "EducriaCargaApresentacao";
    private static final String VERSAO = "v1";
    private static final String CODIGO_TURMA = "DEMO-EDUCRIA";
    private static final String ALUNO = "aluno@demo.educria.test";
    private static final String COLEGA = "colega@demo.educria.test";
    private static final String PROFESSOR = "professor@demo.educria.test";
    private static final String ADMIN = "admin@demo.educria.test";
    private static final Path ARQUIVO_SENHA = Path.of("/run/secrets/educria_demo_senha");

    private final JdbcTemplate db;
    private final PasswordEncoder senhas;
    private final Clock relogio;
    private final AtividadeColetivaService atividades;

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        BancoDaApresentacao.verificarIdentificacao(db);
        int bloqueio = db.queryForObject("""
                DECLARE @resultado int;
                EXEC @resultado = sys.sp_getapplock @Resource = N'educria.carga_apresentacao',
                    @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 15000;
                SELECT @resultado;
                """, Integer.class);
        if (bloqueio < 0) {
            throw new IllegalStateException("Não foi possível obter o bloqueio da carga de apresentação.");
        }

        var versoes = BancoDaApresentacao.propriedade(db, MARCADOR);
        if (!versoes.isEmpty()) {
            if (!versoes.equals(List.of(VERSAO))) {
                throw new IllegalStateException("O marcador da carga de apresentação possui uma versão desconhecida.");
            }
            verificarPerfisReservados();
            return;
        }
        verificarColisoes();

        long matematica = id("SELECT id FROM dbo.materias WHERE nome = N'Matemática'");
        verificarCatalogo(matematica);
        long fracoes = id("SELECT id FROM dbo.assuntos WHERE materia_id = ? AND nome = N'frações'", matematica);
        String senha = lerSenha();
        long turma = id("""
                INSERT dbo.turmas(nome, codigo, ano_letivo) OUTPUT inserted.id
                VALUES (N'Turma de demonstração', ?, ?)
                """, CODIGO_TURMA, LocalDate.now(relogio).getYear());
        long aluno = criarAluno(ALUNO, "Lia Demonstração", "DEMO-ALUNO", senha, turma);
        long colega = criarAluno(COLEGA, "Rui Demonstração", "DEMO-COLEGA", senha, turma);
        long professor = criarUsuario(PROFESSOR, "Docente Demonstração", senha, Perfil.PROFESSOR);
        db.update("INSERT dbo.professores(usuario_id, materia_id) VALUES (?, ?)", professor, matematica);
        db.update("INSERT dbo.professor_turmas(professor_id, turma_id) VALUES (?, ?)", professor, turma);
        long admin = criarUsuario(ADMIN, "Administração Demonstração", senha, Perfil.ADMIN);
        db.update("INSERT dbo.admins(usuario_id) VALUES (?)", admin);

        historico(aluno, fracoes, List.of(new Pratica(21, -8), new Pratica(23, -4)));
        historico(colega, fracoes, List.of(new Pratica(22, -3)));
        var proxima = questaoDemo(fracoes, 25);
        sessaoComQuestao(aluno, fracoes, proxima.id(), SituacaoSessao.INTERROMPIDA, 0);

        atividades.lancar(PROFESSOR, turma, new LancarAtividadeRequest(TipoAtividadeColetiva.DESAFIO,
                "Desafio coletivo de frações", 40, "Registro fictício para o painel da turma."));

        db.update("EXEC sys.sp_addextendedproperty @name = ?, @value = ?", MARCADOR, VERSAO);
    }

    private void verificarColisoes() {
        int usuarios = db.queryForObject("""
                SELECT COUNT(*) FROM dbo.usuarios
                WHERE email COLLATE Latin1_General_100_CI_AS IN (?, ?, ?, ?)
                """, Integer.class, ALUNO, COLEGA, PROFESSOR, ADMIN);
        int turmas = db.queryForObject("""
                SELECT COUNT(*) FROM dbo.turmas WHERE codigo COLLATE Latin1_General_100_CI_AS = ?
                """, Integer.class, CODIGO_TURMA);
        int matriculas = db.queryForObject("""
                SELECT COUNT(*) FROM dbo.alunos
                WHERE matricula COLLATE Latin1_General_100_CI_AS IN (N'DEMO-ALUNO', N'DEMO-COLEGA')
                """, Integer.class);
        if (usuarios != 0 || turmas != 0 || matriculas != 0) {
            throw new IllegalStateException("Identificadores reservados da apresentação já estão em uso sem marcador de carga.");
        }
    }

    private void verificarPerfisReservados() {
        int registros = db.queryForObject("""
                SELECT COUNT(*) FROM dbo.usuarios u
                LEFT JOIN dbo.alunos a ON a.usuario_id = u.id
                LEFT JOIN dbo.professores p ON p.usuario_id = u.id
                LEFT JOIN dbo.admins adm ON adm.usuario_id = u.id
                WHERE (u.email IN (?, ?) AND u.perfil = N'ALUNO' AND a.usuario_id IS NOT NULL)
                   OR (u.email = ? AND u.perfil = N'PROFESSOR' AND p.usuario_id IS NOT NULL)
                   OR (u.email = ? AND u.perfil = N'ADMIN' AND adm.usuario_id IS NOT NULL)
                """, Integer.class, ALUNO, COLEGA, PROFESSOR, ADMIN);
        if (registros != 4) {
            throw new IllegalStateException("Os perfis reservados da apresentação divergem do marcador de carga.");
        }
    }

    private void verificarCatalogo(long matematica) {
        boolean completo = db.queryForObject("""
                WITH itens AS (
                    SELECT q.ordem, q.assunto_id, q.dificuldade
                    FROM dbo.questoes q
                    JOIN dbo.atividades a ON a.id = q.atividade_id AND a.ativa = 1
                    JOIN dbo.assuntos s ON s.id = q.assunto_id AND s.materia_id = ?
                    WHERE q.atividade_id = -1 AND q.ativa = 1
                )
                SELECT CAST(CASE WHEN
                    (SELECT COUNT(*) FROM dbo.questoes WHERE atividade_id = -1) = 160
                    AND (SELECT COUNT(DISTINCT ordem) FROM itens WHERE ordem BETWEEN 1 AND 160) = 160
                    AND (SELECT COUNT(DISTINCT assunto_id) FROM itens) = 8
                    AND NOT EXISTS (
                        SELECT 1 FROM itens GROUP BY assunto_id, dificuldade
                        HAVING COUNT(*) <> 2 OR dificuldade IS NULL OR dificuldade NOT BETWEEN 1 AND 10
                    )
                    THEN 1 ELSE 0 END AS bit)
                """, Boolean.class, matematica);
        if (!completo) throw new IllegalStateException("A carga de apresentação exige as 160 questões demo de Matemática.");
    }

    private static String lerSenha() {
        try {
            String senha = Files.readString(ARQUIVO_SENHA).strip();
            if (senha.length() >= 24 && senha.matches("[!-~]{24,72}")) return senha;
        } catch (IOException e) {
            throw new IllegalStateException("Segredo local das contas de apresentação indisponível.");
        }
        throw new IllegalStateException("Segredo local das contas de apresentação inválido.");
    }

    private long criarUsuario(String email, String nome, String senha, Perfil perfil) {
        return id("""
                INSERT dbo.usuarios(email, nome, senha, perfil) OUTPUT inserted.id VALUES (?, ?, ?, ?)
                """, email, nome, senhas.encode(senha), perfil.name());
    }

    private long criarAluno(String email, String nome, String matricula, String senha, long turma) {
        long aluno = criarUsuario(email, nome, senha, Perfil.ALUNO);
        // O trigger existente cria o desempenho por assunto para o novo aluno.
        db.update("INSERT dbo.alunos(usuario_id, matricula, turma_id) VALUES (?, ?, ?)", aluno, matricula, turma);
        return aluno;
    }

    private void historico(long aluno, long assunto, List<Pratica> praticas) {
        var desempenho = new NivelAdaptativo(NivelAdaptativo.MINIMO, 0);
        for (var pratica : praticas) {
            var questao = questaoDemo(assunto, pratica.ordem());
            long sessao = sessaoComQuestao(aluno, assunto, questao.id(), SituacaoSessao.ENCERRADA, pratica.dias());
            var depois = desempenho.aposResposta(true);
            db.update("""
                    INSERT dbo.respostas_aluno(aluno_id, questao_id, resposta_dada, correta, respondido_em,
                        sessao_id, nivel_anterior, nivel_atual, passo, pontos_individuais)
                    VALUES (?, ?, ?, 1, DATEADD(minute, -2, DATEADD(day, ?,
                        CONVERT(datetime2(6), SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time'))),
                        ?, ?, ?, 1, ?)
                    """, aluno, questao.id(), questao.letra(), pratica.dias(), sessao, desempenho.nivel(),
                    depois.nivel(), PontuacaoIndividual.acerto(questao.dificuldade(), Optional.empty()));
            desempenho = depois;

            var intervalo = IntervaloRepeticao.doPrimeiro();
            var dia = LocalDate.now(relogio).plusDays(pratica.dias());
            db.update("""
                    INSERT dbo.repeticao_espacada(aluno_id, questao_id, intervalo_atual, ultima_revisao, proxima_revisao)
                    VALUES (?, ?, ?, ?, ?)
                    """, aluno, questao.id(), intervalo.name(), Date.valueOf(dia), Date.valueOf(dia.plusDays(intervalo.getDias())));
        }

        int porAssunto = db.update("""
                UPDATE dbo.desempenho_por_assunto SET nivel_atual = ?, total_acertos = ?
                WHERE aluno_id = ? AND assunto_id = ?
                """, desempenho.nivel(), praticas.size(), aluno, assunto);
        if (porAssunto != 1) throw new IllegalStateException("O desempenho inicial do aluno não foi criado pelo catálogo.");
        db.update("""
                INSERT dbo.desempenho_adaptativo(aluno_id, nivel_atual, total_acertos, atualizado_em)
                VALUES (?, ?, ?, DATEADD(day, ?,
                    CONVERT(datetime2(6), SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time')))
                """, aluno, desempenho.nivel(), praticas.size(), praticas.getLast().dias());
    }

    private QuestaoDaDemo questaoDemo(long assunto, int ordem) {
        return db.queryForObject("""
                SELECT id, dificuldade, resposta_correta FROM dbo.questoes
                WHERE atividade_id = -1 AND assunto_id = ? AND ordem = ? AND ativa = 1
                  AND resposta_correta IN (N'A', N'B', N'C', N'D')
                  AND NULLIF(LTRIM(RTRIM(alternativa_a)), N'') IS NOT NULL
                  AND NULLIF(LTRIM(RTRIM(alternativa_b)), N'') IS NOT NULL
                  AND NULLIF(LTRIM(RTRIM(alternativa_c)), N'') IS NOT NULL
                  AND NULLIF(LTRIM(RTRIM(alternativa_d)), N'') IS NOT NULL
                """, (rs, numero) -> new QuestaoDaDemo(rs.getLong(1), rs.getInt(2), rs.getString(3)), assunto, ordem);
    }

    private long sessaoComQuestao(long aluno, long assunto, long questao, SituacaoSessao situacao, int dias) {
        long sessao = id("""
                INSERT dbo.sessoes_estudo(aluno_id, assunto_id, situacao, iniciada_em, atualizada_em)
                OUTPUT inserted.id VALUES (?, ?, ?,
                    DATEADD(minute, -3, DATEADD(day, ?, SYSDATETIMEOFFSET())),
                    DATEADD(minute, -1, DATEADD(day, ?, SYSDATETIMEOFFSET())))
                """, aluno, assunto, situacao.name(), dias, dias);
        db.update("INSERT dbo.sessao_questoes(sessao_id, indice, questao_id) VALUES (?, 0, ?)", sessao, questao);
        return sessao;
    }

    private long id(String sql, Object... parametros) {
        return db.queryForObject(sql, Long.class, parametros);
    }

    private record Pratica(int ordem, int dias) {}
    private record QuestaoDaDemo(long id, int dificuldade, String letra) {}
}
