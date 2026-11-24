package com.educria.service;

import com.educria.repository.AssuntoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = IntegracaoSqlServer.VARIAVEL_URL, matches = ".+")
class SelecaoAdaptativaIT extends IntegracaoSqlServer {

    @Autowired private SelecaoAdaptativaService servico;
    @Autowired private AssuntoRepository assuntos;
    @Autowired private JdbcTemplate banco;
    @Autowired private PasswordEncoder encoder;

    @Test
    void consultaRealLimitaSelecaoAoAssuntoEAsAtividadesAtivas() {
        long materia = id("insert into materias(nome) output inserted.id values (?)", UUID.randomUUID().toString());
        long assunto = id("insert into assuntos(materia_id, nome) output inserted.id values (?, N'Frações')", materia);
        long outro = id("insert into assuntos(materia_id, nome) output inserted.id values (?, N'Equações')", materia);
        long ativa = id("insert into atividades(titulo, ativa) output inserted.id values ('Teste', 1)");
        long inativa = id("insert into atividades(titulo, ativa) output inserted.id values ('Teste', 0)");
        long elegivel = inserirQuestao(ativa, assunto, 3);
        long foraDoAssunto = inserirQuestao(ativa, outro, 10);
        long desativada = inserirQuestao(inativa, assunto, 10);
        inserirQuestao(ativa, assunto, null);

        LocalDate hoje = LocalDate.of(2026, 11, 19);
        Map<Long, LocalDate> revisoes = Map.of(foraDoAssunto, hoje, desativada, hoje);
        var inicio = new SelecaoAdaptativaService.Contexto(Set.of(), revisoes, 0, hoje);
        var selecionada = servico.selecionar(assunto, 10, inicio).orElseThrow();
        assertEquals(elegivel, selecionada.id());
        assertEquals(assunto, selecionada.assuntoId());
        assertEquals(3, selecionada.dificuldade());

        var fim = new SelecaoAdaptativaService.Contexto(Set.of(elegivel), revisoes, 0, hoje);
        assertTrue(servico.selecionar(assunto, 10, fim).isEmpty());
    }

    @Test
    void selecaoERecomendacaoExigemAlternativaCorretaDisponivel() {
        long materia = id("insert into materias(nome) output inserted.id values (?)", UUID.randomUUID().toString());
        long soInvalidas = id("insert into assuntos(materia_id, nome) output inserted.id values (?, N'Só inválidas')", materia);
        long misto = id("insert into assuntos(materia_id, nome) output inserted.id values (?, N'Com questão válida')", materia);
        long atividade = id("insert into atividades(titulo) output inserted.id values ('Teste')");
        long aluno = id("""
                insert into usuarios(nome, email, senha, perfil) output inserted.id values ('Aluno', ?, ?, 'ALUNO')
                """, UUID.randomUUID() + "@example.test", encoder.encode(segredoTemporario()));
        banco.update("insert into alunos(usuario_id) values (?)", aluno);
        banco.update("update desempenho_por_assunto set nivel_atual = 10 where aluno_id = ? and assunto_id not in (?, ?)",
                aluno, soInvalidas, misto);

        for (long assunto : new long[] {soInvalidas, misto}) {
            inserirQuestao(atividade, assunto, 1, "X", "opção disponível");
            inserirQuestao(atividade, assunto, 1, "B", null);
            inserirQuestao(atividade, assunto, 1, "B", "");
            inserirQuestao(atividade, assunto, 1, "B", "   ");
            inserirQuestao(atividade, assunto, 1, "B", "\t\n\u2003");
        }
        long valida = inserirQuestao(atividade, misto, 5, "b", "  2  ");

        LocalDate hoje = LocalDate.of(2026, 11, 19);
        var inicio = new SelecaoAdaptativaService.Contexto(Set.of(), Map.of(), 0, hoje);
        assertTrue(servico.selecionar(soInvalidas, 1, inicio).isEmpty());
        assertEquals(valida, servico.selecionar(misto, 1, inicio).orElseThrow().id());
        assertEquals(misto, assuntos.recomendar(aluno, hoje, PageRequest.of(0, 1)).getFirst().getId());
    }

    private long inserirQuestao(long atividade, long assunto, Integer dificuldade) {
        return inserirQuestao(atividade, assunto, dificuldade, "B", "2");
    }

    private long inserirQuestao(long atividade, long assunto, Integer dificuldade, String gabarito, String alternativaB) {
        return id("""
                insert into questoes (atividade_id, assunto_id, dificuldade, enunciado, alternativa_a,
                    alternativa_b, alternativa_c, alternativa_d, resposta_correta, explicacao)
                output inserted.id values (?, ?, ?, N'Quanto é 1 + 1?', '1', ?, '3', '4', ?, N'A resposta é 2.')
                """, atividade, assunto, dificuldade, alternativaB, gabarito);
    }

    private long id(String sql, Object... parametros) {
        return banco.queryForObject(sql, Long.class, parametros);
    }
}
