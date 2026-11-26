package com.educria.service;

import com.educria.domain.entity.Admin;
import com.educria.domain.entity.Aluno;
import com.educria.domain.entity.Materia;
import com.educria.domain.entity.Professor;
import com.educria.domain.entity.Usuario;
import com.educria.domain.enums.Perfil;
import com.educria.dto.AssuntoDoAluno;
import com.educria.dto.DetalheDoAluno;
import com.educria.dto.MateriaResumo;
import com.educria.dto.RelatorioEvolucao;
import com.educria.dto.RelatorioEvolucao.Dia;
import com.educria.dto.ResultadoSessao;
import com.educria.repository.UsuarioRepository;
import com.educria.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
import java.util.Arrays;
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
    @Autowired private EntityManager entidades;

    @Test
    void recortaEvolucaoDoProfessorSemAlterarEvolucaoDoTitular() throws Exception {
        var titular = aluno();
        long turma = id("insert dbo.turmas(nome) output inserted.id values (N'Turma de teste')");
        db.update("update dbo.alunos set turma_id=? where usuario_id=?", turma, titular.id());
        long materia = materia();
        long assuntoProprio = assunto(materia, "Frações");
        long assuntoDeOutraMateria = assunto("Leitura");
        var professor = usuario(Professor.builder().perfil(Perfil.PROFESSOR)
                .materia(entidades.getReference(Materia.class, materia)));
        db.update("insert dbo.professor_turmas(professor_id,turma_id) values (?,?)", professor.id(), turma);
        long propria = sessao(titular, assuntoProprio, "2026-12-20T12:00:00Z");
        long outra = sessao(titular, assuntoDeOutraMateria, "2026-12-21T12:00:00Z");
        long questaoPropria = questao(assuntoProprio);
        long questaoDeOutraMateria = questao(assuntoDeOutraMateria);
        apresentar(propria, questaoPropria);
        apresentar(outra, questaoDeOutraMateria);
        responder(titular, propria, questaoPropria, 1, true, "2026-12-20T09:01:00", 1, 2);
        responder(titular, outra, questaoDeOutraMateria, 1, false, "2026-12-21T09:01:00", 1, 1);
        db.update("update dbo.questoes set assunto_id=? where id=?", assuntoDeOutraMateria, questaoPropria);
        entidades.clear();

        var detalhe = lerJson(get("/api/professor/turmas/{turma}/alunos/{aluno}", turma, titular.id()),
                professor, DetalheDoAluno.class);
        var evolucao = detalhe.evolucao();
        assertTotais(evolucao, 1, 1, 1, 0, 0, 1, 0, "100.00");
        assertEquals(1, evolucao.evolucaoDiaria().stream().mapToLong(Dia::tentativas).sum());
        assertEquals(List.of(assuntoProprio), evolucao.assuntos().stream().map(RelatorioEvolucao.Assunto::assuntoId).toList());
        assertEquals(2, ler(get(ROTA), titular).resumo().tentativas());
    }

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
    void totalizaPontosPrivadosPeloAssuntoDaSessaoEComparaSomenteOMesmoAssunto() throws Exception {
        var titular = aluno();
        var outro = aluno();
        db.update("insert into desempenho_adaptativo(aluno_id) values (?), (?)", titular.id(), outro.id());
        long materia = materia();
        long divisao = assunto(materia, "Divisão");
        long multiplicacao = assunto(materia, "Multiplicação");
        long outraMateria = materia();
        long divisaoOutraMateria = assunto(outraMateria, "Divisão");
        long questao = questao(divisao);
        long questaoDeMultiplicacao = questao(multiplicacao);
        long legada = sessao(titular, divisao, "2026-12-05T12:00:00Z");
        long anterior = sessao(titular, divisao, "2026-12-06T12:00:00Z");
        long deOutroAssunto = sessao(titular, multiplicacao, "2026-12-07T12:00:00Z");
        long atual = sessao(titular, divisao, "2026-12-08T12:00:00Z");
        long mista = sessao(titular, divisao, "2026-12-08T13:00:00Z");
        long interrompida = sessao(titular, divisao, "2026-12-09T12:00:00Z");
        long alheia = sessao(outro, divisao, "2026-12-10T12:00:00Z");
        for (long id : new long[] {legada, anterior, atual, mista, interrompida, alheia}) apresentar(id, questao);
        apresentar(deOutroAssunto, questaoDeMultiplicacao);
        responder(titular, legada, questao, 1, true, "2026-12-05T09:01:00", null, null);
        responder(titular, anterior, questao, 1, true, "2026-12-06T09:01:00", 5, 5);
        responder(titular, deOutroAssunto, questaoDeMultiplicacao, 1, true, "2026-12-07T09:01:00", 1, 1);
        responder(titular, atual, questao, 1, true, "2026-12-08T09:01:00", 5, 5);
        responder(titular, mista, questao, 1, true, "2026-12-08T10:01:00", null, null);
        responder(titular, mista, questao, 2, false, "2026-12-08T10:02:00", 5, 5);
        responder(titular, interrompida, questao, 1, false, "2026-12-09T09:01:00", 5, 5);
        responder(outro, alheia, questao, 1, true, "2026-12-10T09:01:00", 3, 4);
        pontosDaResposta(anterior, 50);
        pontosDaResposta(deOutroAssunto, 10);
        pontosDaResposta(atual, 15);
        db.update("update respostas_aluno set pontos_individuais = 0 where sessao_id = ? and passo = 2", mista);
        pontosDaResposta(interrompida, 0);
        pontosDaResposta(alheia, 100);
        db.update("update sessoes_estudo set situacao = 'INTERROMPIDA' where id = ?", interrompida);
        db.update("update questoes set assunto_id = ?, dificuldade = 10 where id = ?", divisaoOutraMateria, questao);

        var resultadoAtual = resultado(atual, titular);
        assertEquals(15L, resultadoAtual.sessao().pontosIndividuais());
        assertTrue(resultadoAtual.sessao().pontuacaoCompleta());
        assertEquals(1L, resultadoAtual.sessao().acertos());
        assertEquals(anterior, resultadoAtual.anterior().id());
        assertEquals(50L, resultadoAtual.anterior().pontosIndividuais());
        assertTrue(resultadoAtual.anterior().pontuacaoCompleta());
        assertEquals(65L, resultadoAtual.pontosIndividuaisNoAssunto());
        assertEquals(75L, resultadoAtual.pontosIndividuaisNaMateria());
        var indicadoresInterrompida = resultado(interrompida, titular).sessao();
        assertEquals(0L, indicadoresInterrompida.pontosIndividuais());
        assertTrue(indicadoresInterrompida.pontuacaoCompleta());
        var indicadoresLegada = resultado(legada, titular).sessao();
        assertNull(indicadoresLegada.pontosIndividuais());
        assertFalse(indicadoresLegada.pontuacaoCompleta());
        var resultadoMista = resultado(mista, titular);
        assertEquals(0L, resultadoMista.sessao().pontosIndividuais());
        assertFalse(resultadoMista.sessao().pontuacaoCompleta());
        assertTrue(resultadoMista.anterior().pontuacaoCompleta());
        assertNull(resultado(anterior, titular).anterior().pontosIndividuais());

        var historico = lerJson(get("/api/aluno/sessoes/historico"), titular, ResultadoSessao.Historico.class);
        assertEquals(Arrays.asList(0L, 0L, 15L, 10L, 50L, null), historico.sessoes().stream()
                .map(ResultadoSessao.Indicadores::pontosIndividuais).toList());

        var catalogo = Arrays.asList(lerJson(get("/api/aluno/materias").param("alunoId", Long.toString(outro.id())),
                titular, MateriaResumo[].class));
        var materiaDaSessao = catalogo.stream().filter(m -> m.id() == materia).findFirst().orElseThrow();
        assertEquals(2L, materiaDaSessao.totalAssuntos());
        assertEquals(75L, materiaDaSessao.pontosIndividuais());
        assertEquals(0L, catalogo.stream().filter(m -> m.id() == outraMateria).findFirst().orElseThrow().pontosIndividuais());

        var assuntos = lerJson(get("/api/aluno/materias/{materiaId}/assuntos", materia)
                .param("alunoId", Long.toString(outro.id())), titular, AssuntoDoAluno[].class);
        assertArrayEquals(new AssuntoDoAluno[] {new AssuntoDoAluno(divisao, "Divisão", 1, 65),
                new AssuntoDoAluno(multiplicacao, "Multiplicação", 1, 10)}, assuntos);
        var assuntosDeOutro = lerJson(get("/api/aluno/materias/{materiaId}/assuntos", materia), outro, AssuntoDoAluno[].class);
        assertEquals(100L, assuntosDeOutro[0].pontosIndividuais());
        assertEquals(0L, assuntosDeOutro[1].pontosIndividuais());
        var desempenho = lerJson(get("/api/aluno/desempenho").param("alunoId", Long.toString(outro.id())),
                titular, JsonNode.class);
        assertEquals(75L, desempenho.get("pontosIndividuais").longValue());
        assertEquals(1, desempenho.get("nivelAtual").intValue());
        var desempenhoDoOutro = lerJson(get("/api/aluno/desempenho"), outro, JsonNode.class);
        assertEquals(100L, desempenhoDoOutro.get("pontosIndividuais").longValue());
        mvc.perform(autenticar(get("/api/aluno/sessoes/{id}/resultado", alheia), titular))
                .andExpect(status().isNotFound());
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
        return lerJson(pedido, titular, RelatorioEvolucao.class);
    }

    private <T> T lerJson(MockHttpServletRequestBuilder pedido, Identidade titular, Class<T> tipo) throws Exception {
        return json.readValue(mvc.perform(autenticar(pedido, titular)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray(), tipo);
    }

    private MockHttpServletRequestBuilder autenticar(MockHttpServletRequestBuilder pedido, Identidade titular) {
        return pedido.header(HttpHeaders.AUTHORIZATION, "Bearer " + titular.token());
    }

    private ResultadoSessao resultado(long sessao, Identidade titular) throws Exception {
        return lerJson(get("/api/aluno/sessoes/{id}/resultado", sessao), titular, ResultadoSessao.class);
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
        return assunto(materia(), nome);
    }

    private long materia() {
        return id("insert into materias(nome) output inserted.id values (?)", UUID.randomUUID().toString());
    }

    private long assunto(long materia, String nome) {
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

    private void pontosDaResposta(long sessao, int pontos) {
        db.update("update respostas_aluno set pontos_individuais = ? where sessao_id = ? and passo = 1", pontos, sessao);
    }

    private long id(String sql, Object... params) {
        return db.queryForObject(sql, Long.class, params);
    }

    private record Identidade(long id, String token) {}
}
