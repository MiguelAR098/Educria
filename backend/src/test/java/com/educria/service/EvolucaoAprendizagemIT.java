package com.educria.service;

import com.educria.domain.entity.Admin;
import com.educria.domain.entity.Aluno;
import com.educria.domain.entity.Usuario;
import com.educria.domain.enums.Perfil;
import com.educria.dto.RelatorioEvolucao;
import com.educria.dto.RelatorioEvolucao.Dia;
import com.educria.dto.ResultadoSessao;
import com.educria.repository.UsuarioRepository;
import com.educria.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(EvolucaoAprendizagemIT.RelogioDeTeste.class)
@EnabledIfEnvironmentVariable(named = IntegracaoSqlServer.VARIAVEL_URL, matches = ".+")
class EvolucaoAprendizagemIT extends IntegracaoSqlServer {
    private static final String ROTA = "/api/aluno/evolucao";
    private static final LocalDate INICIO = LocalDate.of(2026, 12, 2);
    private static final LocalDate HOJE = LocalDate.of(2026, 12, 31);

    @TestConfiguration(proxyBeanMethods = false)
    static class RelogioDeTeste {
        @Bean
        @Primary
        Clock relogioFixoDeEvolucao() {
            return Clock.fixed(Instant.parse("2027-01-01T02:30:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
    }

    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder encoder;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate db;
    @Autowired private JwtService jwt;

    @Test
    void alunoSemSessoesRecebeTrintaDiasZerados() throws Exception {
        var relatorio = ler(get(ROTA), aluno());

        assertEquals(INICIO, relatorio.dataInicial());
        assertEquals(HOJE, relatorio.dataFinal());
        assertEquals(INICIO.datesUntil(HOJE.plusDays(1)).map(data -> new Dia(data, 0, 0, 0, 0)).toList(),
                relatorio.evolucaoDiaria());
        assertTotais(relatorio, 0, 0, 0, 0, 0, 0, 0, null);
        assertTrue(relatorio.assuntos().isEmpty());
    }

    @Test
    void agregaPeloTitularPeloAssuntoEPelaDataRealDeCadaEvento() throws Exception {
        var titular = aluno();
        var outro = aluno();
        long fracoes = assunto("Frações");
        long homonimo = assunto("Frações");
        long compartilhada = questao(fracoes);
        long nova = questao(fracoes);
        long deOutroAssunto = questao(homonimo);

        long antiga = sessao(titular, fracoes, "2026-12-02T02:59:59.999999Z");
        long noInicio = sessao(titular, fracoes, "2026-12-02T03:00:00Z");
        long recente = sessao(titular, fracoes, "2026-12-31T03:00:00Z");
        sessao(titular, homonimo, "2026-12-15T12:00:00Z");
        long foraDoPeriodo = sessao(titular, fracoes, "2027-01-01T03:00:00Z");
        long alheia = sessao(outro, homonimo, "2026-12-20T12:00:00Z");
        apresentar(antiga, compartilhada);
        apresentar(noInicio, compartilhada, nova);
        apresentar(recente, compartilhada);
        apresentar(foraDoPeriodo, nova);
        apresentar(alheia, deOutroAssunto);

        // A resposta mais recente recebe ID menor, para que os níveis dependam da data.
        responder(titular, recente, compartilhada, 1, true, "2026-12-31T23:59:59.999999", 9, 10);
        responder(titular, antiga, compartilhada, 1, false, "2026-12-01T23:59:59.999999", 2, 2);
        responder(titular, antiga, compartilhada, 2, true, "2026-12-02T00:00:00", 2, 2);
        responder(titular, noInicio, compartilhada, 1, false, "2026-12-02T00:01:00", 6, 6);
        responder(titular, noInicio, compartilhada, 2, false, "2026-12-02T00:02:00", 6, 6);
        responder(titular, noInicio, compartilhada, 3, true, "2026-12-02T00:03:00", 6, 6);
        responder(titular, noInicio, nova, 4, true, "2026-12-02T00:04:00", 6, 7);
        responder(titular, foraDoPeriodo, nova, 1, true, "2027-01-01T00:00:00", 3, 4);
        responder(outro, alheia, deOutroAssunto, 1, true, "2026-12-20T09:00:00", 4, 5);

        var relatorio = ler(get(ROTA).param("alunoId", Long.toString(outro.id())), titular);
        assertTotais(relatorio, 3, 6, 4, 2, 1, 2, 3, "66.67");
        assertEquals(new Dia(INICIO, 5, 3, 2, 1), relatorio.evolucaoDiaria().getFirst());
        assertEquals(new Dia(HOJE, 1, 1, 0, 0), relatorio.evolucaoDiaria().getLast());
        assertEquals(0, relatorio.evolucaoDiaria().get(18).tentativas());
        assertEquals(List.of(fracoes, homonimo), relatorio.assuntos().stream()
                .map(RelatorioEvolucao.Assunto::assuntoId).toList());
        assertEquals(new RelatorioEvolucao.Assunto(fracoes, "Frações", 2, 6, 4, 2, 1, 2,
                new BigDecimal("66.67"), 2, 10), relatorio.assuntos().getFirst());
        assertEquals(new RelatorioEvolucao.Assunto(homonimo, "Frações", 1, 0, 0, 0, 0, 0,
                null, null, null), relatorio.assuntos().getLast());
        assertTotais(ler(get(ROTA), outro), 1, 1, 1, 0, 0, 1, 0, "100.00");

        var filtrado = ler(get(ROTA).param("assuntoId", Long.toString(fracoes)), titular);
        assertTotais(filtrado, 2, 6, 4, 2, 1, 2, 3, "66.67");
        assertEquals(List.of(relatorio.assuntos().getFirst()), filtrado.assuntos());
        var semRespostas = ler(get(ROTA).param("assuntoId", Long.toString(homonimo)), titular);
        assertTotais(semRespostas, 1, 0, 0, 0, 0, 0, 0, null);
        assertTrue(semRespostas.evolucaoDiaria().stream().allMatch(dia -> dia.tentativas() == 0));
    }

    @Test
    void atribuiOErroAoPrimeiroEventoMesmoComEmpateEFiltroPorOutroAssunto() throws Exception {
        var titular = aluno();
        var outro = aluno();
        long fracoes = assunto("Frações");
        long proporcao = assunto("Proporção");
        long compartilhada = questao(fracoes);
        long distinta = questao(fracoes);
        long maisAntiga = sessao(titular, fracoes, "2026-12-02T12:00:00Z");
        long maisNova = sessao(titular, proporcao, "2026-12-02T13:00:00Z");
        long outroDia = sessao(titular, fracoes, "2026-12-03T03:00:00Z");
        long alheia = sessao(outro, fracoes, "2026-12-02T12:00:00Z");
        apresentar(maisAntiga, compartilhada, distinta);
        // Simula a mesma questão registrada em assuntos diferentes no histórico.
        apresentar(maisNova, compartilhada);
        apresentar(outroDia, compartilhada);
        apresentar(alheia, compartilhada);

        String fimDoDia = "2026-12-02T23:59:59.999999";
        responder(titular, maisNova, compartilhada, 1, false, fimDoDia, 3, 3);
        responder(titular, maisAntiga, compartilhada, 1, false, fimDoDia, 2, 2);
        responder(titular, maisAntiga, compartilhada, 2, false, fimDoDia, 2, 2);
        responder(titular, maisAntiga, compartilhada, 3, true, fimDoDia, 2, 2);
        responder(titular, maisAntiga, distinta, 4, false, fimDoDia, 2, 1);
        responder(titular, outroDia, compartilhada, 1, false, "2026-12-03T00:00:00", 1, 1);
        responder(outro, alheia, compartilhada, 1, false, fimDoDia, 1, 1);

        var geral = ler(get(ROTA), titular);
        assertTotais(geral, 3, 6, 1, 5, 3, 2, 2, "0.00");
        assertEquals(new Dia(INICIO, 5, 1, 4, 2), geral.evolucaoDiaria().getFirst());
        assertEquals(new Dia(INICIO.plusDays(1), 1, 0, 1, 1), geral.evolucaoDiaria().get(1));
        var filtrado = ler(get(ROTA).param("assuntoId", Long.toString(fracoes)), titular);
        assertTotais(filtrado, 2, 5, 1, 4, 2, 2, 2, "0.00");
        assertEquals(2, filtrado.assuntos().getFirst().erros());
        assertTotais(ler(get(ROTA), outro), 1, 1, 0, 1, 1, 1, 0, "0.00");

        var antiga = resultado(maisAntiga, titular);
        assertEquals(4, antiga.sessao().tentativas());
        assertEquals(3, antiga.sessao().tentativasIncorretas());
        assertEquals(1, antiga.sessao().erros());
        assertEquals(4, antiga.questoes().size());
        var nova = resultado(maisNova, titular);
        assertEquals(1, nova.sessao().erros());
        var diaSeguinte = resultado(outroDia, titular);
        assertEquals(1, diaSeguinte.sessao().erros());
        assertEquals(antiga.sessao(), diaSeguinte.anterior());
        assertEquals(geral.resumo().erros(), antiga.sessao().erros() + nova.sessao().erros() + diaSeguinte.sessao().erros());
        var pagina = json.readValue(mvc.perform(autenticar(get("/api/aluno/sessoes/historico")
                        .param("antesDe", Long.toString(outroDia)), titular)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray(), ResultadoSessao.Historico.class);
        assertEquals(List.of(nova.sessao(), antiga.sessao()), pagina.sessoes());
    }

    @Test
    void incluiRespostasLegadasSemInventarSessaoOuNivel() throws Exception {
        var titular = aluno();
        long assunto = assunto("Equações");
        long questao = questao(assunto);
        long sessao = sessao(titular, assunto, "2026-12-25T12:00:00Z");
        apresentar(sessao, questao);
        responder(titular, sessao, questao, 1, false, "2026-12-25T09:01:00", null, null);
        responder(titular, null, questao, null, false, "2026-12-25T09:00:00", null, null);
        responder(titular, null, questao(null), null, true, "2026-12-25T09:03:00", null, null);

        var relatorio = ler(get(ROTA), titular);

        assertTotais(relatorio, 1, 3, 1, 2, 1, 2, 0, "0.00");
        assertEquals(List.of(new RelatorioEvolucao.Assunto(assunto, "Equações", 1, 2, 0, 2, 1, 1,
                new BigDecimal("0.00"), null, null)), relatorio.assuntos());
        assertEquals(new Dia(LocalDate.of(2026, 12, 25), 3, 1, 2, 1), relatorio.evolucaoDiaria().get(23));
        assertEquals(1, resultado(sessao, titular).sessao().tentativasIncorretas());
        assertEquals(0, resultado(sessao, titular).sessao().erros());
    }

    @Test
    void validaFiltroEDevolveResumoVazioParaAssuntoExistenteSemAtividade() throws Exception {
        var titular = aluno();
        for (String invalido : List.of("", "abc", "0", "-1", "9223372036854775808")) {
            mvc.perform(autenticar(get(ROTA).param("assuntoId", invalido), titular)).andExpect(status().isBadRequest());
        }
        mvc.perform(autenticar(get(ROTA).param("assuntoId", Long.toString(Long.MAX_VALUE)), titular))
                .andExpect(status().isNotFound());

        var relatorio = ler(get(ROTA).param("assuntoId", Long.toString(assunto("Geometria"))), titular);
        assertTotais(relatorio, 0, 0, 0, 0, 0, 0, 0, null);
        assertTrue(relatorio.assuntos().isEmpty());
    }

    @Test
    void exigeAutenticacaoEPerfilAluno() throws Exception {
        int anonimo = mvc.perform(get(ROTA)).andReturn().getResponse().getStatus();
        assertTrue(anonimo == 401 || anonimo == 403);
        mvc.perform(autenticar(get(ROTA), usuario(Admin.builder().perfil(Perfil.ADMIN))))
                .andExpect(status().isForbidden());
    }

    private RelatorioEvolucao ler(MockHttpServletRequestBuilder pedido, Identidade titular) throws Exception {
        byte[] resposta = mvc.perform(autenticar(pedido, titular)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return json.readValue(resposta, RelatorioEvolucao.class);
    }

    private MockHttpServletRequestBuilder autenticar(MockHttpServletRequestBuilder pedido, Identidade titular) {
        return pedido.header(HttpHeaders.AUTHORIZATION, "Bearer " + titular.token());
    }

    private ResultadoSessao resultado(long sessao, Identidade titular) throws Exception {
        return json.readValue(mvc.perform(autenticar(get("/api/aluno/sessoes/{id}/resultado", sessao), titular))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray(), ResultadoSessao.class);
    }

    private void assertTotais(RelatorioEvolucao relatorio, long sessoes, long tentativas, long acertos, long incorretas, long erros,
                              long unicas, long reaprendizagens, String taxa) {
        assertEquals(new RelatorioEvolucao.Resumo(sessoes, tentativas, acertos, incorretas, erros, unicas, reaprendizagens,
                taxa == null ? null : new BigDecimal(taxa)), relatorio.resumo());
    }

    private Identidade aluno() {
        return usuario(Aluno.builder().perfil(Perfil.ALUNO));
    }

    private Identidade usuario(Usuario.UsuarioBuilder<?, ?> builder) {
        Usuario conta = usuarios.saveAndFlush(builder.nome("Teste").email(UUID.randomUUID() + "@example.test")
                .senha(encoder.encode(segredoTemporario())).build());
        return new Identidade(conta.getId(), jwt.generateToken(conta));
    }

    private long assunto(String nome) {
        long materia = id("insert into materias(nome) output inserted.id values (?)", UUID.randomUUID().toString());
        return id("insert into assuntos(materia_id, nome) output inserted.id values (?, ?)", materia, nome);
    }

    private long questao(Long assunto) {
        long atividade = id("insert into atividades(titulo) output inserted.id values ('Teste')");
        return id("""
                insert into questoes(atividade_id, assunto_id, dificuldade, enunciado, alternativa_a, alternativa_b,
                    resposta_correta)
                output inserted.id values (?, ?, 1, N'Questão', 'Certa', 'Errada', 'A')
                """, atividade, assunto);
    }

    private long sessao(Identidade titular, long assunto, String inicio) {
        OffsetDateTime instante = OffsetDateTime.parse(inicio);
        return id("""
                insert into sessoes_estudo(aluno_id, assunto_id, situacao, iniciada_em, atualizada_em)
                output inserted.id values (?, ?, 'ENCERRADA', ?, ?)
                """, titular.id(), assunto, instante, instante);
    }

    private void apresentar(long sessao, long... questoes) {
        for (int indice = 0; indice < questoes.length; indice++) {
            db.update("insert into sessao_questoes(sessao_id, indice, questao_id) values (?, ?, ?)",
                    sessao, indice, questoes[indice]);
        }
    }

    private void responder(Identidade titular, Long sessao, long questao, Integer passo, boolean correta, String data,
                           Integer nivelAnterior, Integer nivelAtual) {
        db.update("""
                insert into respostas_aluno(aluno_id, sessao_id, questao_id, passo, resposta_dada, correta, reaprender,
                    respondido_em, nivel_anterior, nivel_atual)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, titular.id(), sessao, questao, passo, correta ? "A" : "B", correta,
                sessao != null && !correta && nivelAnterior != null, LocalDateTime.parse(data), nivelAnterior, nivelAtual);
    }

    private long id(String sql, Object... params) {
        return db.queryForObject(sql, Long.class, params);
    }

    private record Identidade(long id, String token) {}
}
