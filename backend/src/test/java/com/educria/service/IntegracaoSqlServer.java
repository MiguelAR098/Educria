package com.educria.service;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.SecureRandom;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Base das integrações. O perfil de teste recebe a conexão por {@code @DynamicPropertySource}
 * somente depois da validação do endereço local e do marcador no SQL Server.
 */
@ActiveProfiles("integracao")
abstract class IntegracaoSqlServer {
    static final String VARIAVEL_URL = "EDUCRIA_SQLSERVER_TEST_URL";
    private static final Pattern ALVO_LOCAL = Pattern.compile(
            "jdbc:sqlserver://(?:localhost|127\\.0\\.0\\.1):[0-9]+;databaseName=(educria_sqlserver_teste_[A-Za-z0-9_]+)"
                    + "(?:;(?:encrypt|trustServerCertificate|integratedSecurity)=(?:true|false))*;?");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry props) throws SQLException {
        String url = System.getenv(VARIAVEL_URL);
        var alvo = ALVO_LOCAL.matcher(url == null ? "" : url);
        if (!alvo.matches()) throw new IllegalStateException("Use a URL local de uma base SQL Server descartável.");
        String banco = alvo.group(1);
        String user = System.getenv().getOrDefault("EDUCRIA_SQLSERVER_TEST_USER", "");
        String password = System.getenv().getOrDefault("EDUCRIA_SQLSERVER_TEST_PASSWORD", "");
        var credenciais = new Properties();
        if (!user.isEmpty()) credenciais.setProperty("user", user);
        if (!password.isEmpty()) credenciais.setProperty("password", password);
        try (var c = DriverManager.getConnection(url, credenciais); var cmd = c.createStatement();
             var rs = cmd.executeQuery("""
                     select DB_NAME(), (select count(*) from sys.extended_properties
                         where class=0 and name=N'EducriaAmbiente' and convert(nvarchar(30),value)=N'descartavel')
                     """)) {
            if (!rs.next() || !banco.equals(rs.getString(1)) || rs.getInt(2) != 1) {
                throw new IllegalStateException("Base SQL Server de teste sem identificação de isolamento.");
            }
        }
        props.add("spring.datasource.url", () -> url);
        props.add("spring.datasource.username", () -> user);
        props.add("spring.datasource.password", () -> password);
        props.add("spring.flyway.url", () -> url);
        props.add("spring.flyway.user", () -> user);
        props.add("spring.flyway.password", () -> password);
        String jwt = segredoTemporario();
        props.add("educria.jwt.secret", () -> jwt);
    }

    protected static String segredoTemporario() {
        var bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
