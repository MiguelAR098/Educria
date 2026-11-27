package com.educria.service;

import java.util.List;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("apresentacao")
class BancoDaApresentacao {
    static final String BANCO = "educria_demo_apresentacao";
    static final String URL = "jdbc:sqlserver://db:1433;databaseName=" + BANCO
            + ";encrypt=true;trustServerCertificate=true";

    @Bean
    FlywayMigrationStrategy migrarApresentacao(Environment ambiente) {
        return flyway -> {
            // Rejeita um destino externo antes de abrir a conexão usada pelo Flyway.
            if (!URL.equals(ambiente.getProperty("spring.datasource.url"))
                    || !URL.equals(ambiente.getProperty("spring.flyway.url", URL))) {
                throw new IllegalStateException("O perfil de apresentação exige o banco reservado no serviço db.");
            }
            verificarIdentificacao(new JdbcTemplate(flyway.getConfiguration().getDataSource()));
            flyway.migrate();
        };
    }

    static void verificarIdentificacao(JdbcTemplate db) {
        String nome = db.queryForObject("SELECT DB_NAME()", String.class);
        if (!BANCO.equals(nome) || !List.of("descartavel").equals(propriedade(db, "EducriaAmbiente"))) {
            throw new IllegalStateException("Banco de apresentação ausente ou sem a identificação de banco descartável.");
        }
    }

    static List<String> propriedade(JdbcTemplate db, String nome) {
        return db.queryForList("""
                SELECT CONVERT(nvarchar(100), value)
                FROM sys.extended_properties
                WHERE class = 0 AND major_id = 0 AND minor_id = 0
                  AND name COLLATE Latin1_General_100_BIN2 = ?
                """, String.class, nome);
    }
}
