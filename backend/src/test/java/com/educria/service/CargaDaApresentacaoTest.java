package com.educria.service;

import java.time.Clock;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CargaDaApresentacaoTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final PasswordEncoder senhas = mock(PasswordEncoder.class);
    private final AtividadeColetivaService atividades = mock(AtividadeColetivaService.class);
    private final CargaDaApresentacao carga = new CargaDaApresentacao(db, senhas, Clock.systemUTC(), atividades);

    @Test
    void rejeitaUrlExternaAntesDeConsultarFlyway() {
        var ambiente = new MockEnvironment()
                .withProperty("spring.datasource.url", BancoDaApresentacao.URL)
                .withProperty("spring.flyway.url", "jdbc:sqlserver://localhost:1433;databaseName=educria");
        var flyway = mock(Flyway.class);

        var estrategia = new BancoDaApresentacao().migrarApresentacao(ambiente);
        assertThrows(IllegalStateException.class, () -> estrategia.migrate(flyway));
        verifyNoInteractions(flyway);
    }

    @Test
    void rejeitaBancoPrincipalMesmoComMarcador() {
        when(db.queryForObject("SELECT DB_NAME()", String.class)).thenReturn("educria");
        registrarPropriedade("EducriaAmbiente", List.of("descartavel"));
        assertThrows(IllegalStateException.class, () -> BancoDaApresentacao.verificarIdentificacao(db));
    }

    @Test
    void rejeitaBancoSemMarcador() {
        when(db.queryForObject("SELECT DB_NAME()", String.class)).thenReturn(BancoDaApresentacao.BANCO);
        registrarPropriedade("EducriaAmbiente", List.of());
        assertThrows(IllegalStateException.class, () -> carga.run(null));
        verify(db, never()).queryForObject(contains("sp_getapplock"), eq(Integer.class));
        semEscritas();
    }

    @Test
    void naoEscreveNemRecalculaSenhasAoReiniciar() {
        prepararBancoDaApresentacao();
        registrarPropriedade("EducriaCargaApresentacao", List.of("v1"));
        when(db.queryForObject(contains("LEFT JOIN dbo.admins"), eq(Integer.class),
                anyString(), anyString(), anyString(), anyString())).thenReturn(4);

        assertDoesNotThrow(() -> carga.run(null));
        semEscritas();
    }

    @Test
    void rejeitaIdentificadorJaReservadoSemMarcadorDaCarga() {
        prepararBancoDaApresentacao();
        registrarPropriedade("EducriaCargaApresentacao", List.of());
        when(db.queryForObject(contains("WHERE email COLLATE"), eq(Integer.class),
                anyString(), anyString(), anyString(), anyString())).thenReturn(1);
        when(db.queryForObject(contains("WHERE codigo COLLATE"), eq(Integer.class), anyString())).thenReturn(0);
        when(db.queryForObject(contains("WHERE matricula COLLATE"), eq(Integer.class))).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> carga.run(null));
        semEscritas();
    }

    private void registrarPropriedade(String nome, List<String> valores) {
        when(db.queryForList(contains("FROM sys.extended_properties"), eq(String.class), eq(nome))).thenReturn(valores);
    }

    private void prepararBancoDaApresentacao() {
        when(db.queryForObject("SELECT DB_NAME()", String.class)).thenReturn(BancoDaApresentacao.BANCO);
        registrarPropriedade("EducriaAmbiente", List.of("descartavel"));
        when(db.queryForObject(contains("sp_getapplock"), eq(Integer.class))).thenReturn(0);
    }

    private void semEscritas() {
        verify(db, never()).update(anyString(), any(Object[].class));
        verify(db, never()).queryForObject(contains("INSERT"), eq(Long.class), any(Object[].class));
        verifyNoInteractions(senhas, atividades);
    }
}
