package com.educria.config;

import com.educria.domain.entity.*;
import com.educria.dto.QuestaoParaAluno;
import com.educria.repository.AssuntoRepository;
import com.educria.repository.QuestaoRepository;
import com.educria.repository.RespostaAlunoRepository;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PersistenciaSqlServerTest {
    @Test
    void configuracaoPadraoUsaSqlServer() {
        contexto("default").run(contexto -> {
            assertNull(contexto.getStartupFailure());
            var env = contexto.getEnvironment();
            assertEquals("classpath:db/sqlserver/migration", env.getProperty("spring.flyway.locations"));
            assertEquals("false", env.getProperty("spring.flyway.baseline-on-migrate"));
            assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", env.getProperty("spring.datasource.driver-class-name"));
            assertEquals("validate", env.getProperty("spring.jpa.hibernate.ddl-auto"));
        });
    }

    @Test
    void devIncluiApenasOCallbackDeDemonstracaoDoSqlServer() {
        contexto("dev").run(contexto -> {
            assertNull(contexto.getStartupFailure());
            var env = contexto.getEnvironment();
            assertEquals("classpath:db/sqlserver/migration,classpath:db/sqlserver/demo", env.getProperty("spring.flyway.locations"));
            assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", env.getProperty("spring.datasource.driver-class-name"));
        });
    }

    @Test
    void mapeiaEntidadesEConsultasHibernateSemAbrirConexao() throws Exception {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", SQLServerDialect.class.getName())
                .applySetting("hibernate.use_nationalized_character_data", true)
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.connection.provider_class", UserSuppliedConnectionProviderImpl.class.getName())
                .build();
        try {
            var fontes = new MetadataSources(registry);
            for (var entidade : List.of(Usuario.class, Admin.class, Aluno.class, Professor.class, Turma.class,
                    Materia.class, Assunto.class, DesempenhoAdaptativo.class, DesempenhoPorAssunto.class,
                    PontuacaoColetiva.class, Atividade.class, Questao.class, SessaoEstudo.class, RespostaAluno.class,
                    RepeticaoEspacada.class, AtividadeColetiva.class, Recado.class)) {
                fontes.addAnnotatedClass(entidade);
            }
            var metadata = fontes.buildMetadata();
            assertEquals("nvarchar(max)", tipo(metadata, Questao.class, "enunciado"));
            assertEquals("nvarchar(500)", tipo(metadata, Recado.class, "texto"));
            assertEquals("nvarchar(20)", tipo(metadata, Usuario.class, "perfil"));
            assertTrue(tipo(metadata, SessaoEstudo.class, "iniciadaEm").startsWith("datetimeoffset"));
            assertTrue(tipo(metadata, RespostaAluno.class, "respondidoEm").startsWith("datetime2"));
            try (var factory = metadata.buildSessionFactory()) {
                var motor = factory.unwrap(SessionFactoryImplementor.class).getQueryEngine();
                assertNotNull(motor.getSqmFunctionRegistry().findFunctionDescriptor("educria_tamanho_texto"));
                var tradutor = motor.getHqlTranslator();
                tradutor.translate(QuestaoRepository.class.getMethod("listarParaSelecao", Long.class)
                        .getAnnotation(Query.class).value(), QuestaoParaAluno.class);
                tradutor.translate(AssuntoRepository.class.getMethod("recomendar", Long.class, LocalDate.class, Pageable.class)
                        .getAnnotation(Query.class).value(), Assunto.class);
                tradutor.translate(RespostaAlunoRepository.class.getMethod("fila", Long.class, Long.class)
                        .getAnnotation(Query.class).value(), Object[].class);
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private static ApplicationContextRunner contexto(String perfis) {
        return new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=" + perfis,
                        "EDUCRIA_SQLSERVER_URL=jdbc:sqlserver://127.0.0.1:1433;databaseName=educria_sqlserver_teste_fixture");
    }

    private static String tipo(Metadata metadata, Class<?> entidade, String atributo) {
        var coluna = (Column) metadata.getEntityBinding(entidade.getName()).getProperty(atributo).getSelectables().getFirst();
        return coluna.getSqlType(metadata);
    }
}
