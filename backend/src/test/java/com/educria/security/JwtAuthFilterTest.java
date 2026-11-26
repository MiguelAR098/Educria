package com.educria.security;

import com.educria.domain.entity.Aluno;
import com.educria.domain.enums.Perfil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Testa o filtro em memória, com JWTs reais e a consulta de contas substituída por uma função. */
class JwtAuthFilterTest {

    private static final String EMAIL = "aluno@example.test";
    private static final byte[] CHAVE = "chave-jwt-exclusiva-para-testes-01".getBytes(StandardCharsets.UTF_8);

    private final JwtService tokens = new JwtService();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();

    @BeforeEach
    void configurarJwt() {
        ReflectionTestUtils.setField(tokens, "secret", Base64.getEncoder().encodeToString(CHAVE));
        ReflectionTestUtils.setField(tokens, "expirationMs", 60_000L);
    }

    // Limpa a identidade da thread para que o próximo caso comece sem contexto autenticado.
    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticaContaAtiva() throws Exception {
        UserDetails conta = conta(false);
        filtrar(emitirToken(), email -> {
            assertEquals(EMAIL, email);
            return conta;
        });

        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(autenticacao.isAuthenticated());
        assertSame(conta, autenticacao.getPrincipal());
        assertTrue(autenticacao.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ALUNO")));
        assertSame(request, chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    void rejeitaTokenValidoDeContaDesativada() throws Exception {
        filtrar(emitirToken(), email -> conta(true));
        assertRejeitado();
    }

    @Test
    void rejeitaTokenDeContaInexistenteSemExporDetalhes() throws Exception {
        filtrar(emitirToken(), email -> {
            throw new UsernameNotFoundException("Detalhe interno da consulta");
        });
        assertRejeitado();
    }

    /** O JUnit executa a mesma verificação para cada JWT fornecido pela fonte de argumentos. */
    @ParameterizedTest(name = "token rejeitado {index}")
    @MethodSource("tokensRejeitados")
    void rejeitaTokenInvalidoAntesDeConsultarConta(String token) throws Exception {
        filtrar(token, email -> fail("JWT inválido não deve consultar a conta"));
        assertRejeitado();
    }

    private static Stream<String> tokensRejeitados() {
        var chave = Keys.hmacShaKeyFor(CHAVE);
        var outraChave = Keys.hmacShaKeyFor("outra-chave-jwt-de-teste-xyz-012345".getBytes(StandardCharsets.UTF_8));
        Date futuro = Date.from(Instant.now().plusSeconds(60));
        return Stream.of(
                "",
                "token-malformado",
                Jwts.builder().subject(EMAIL).expiration(futuro).signWith(outraChave).compact(),
                Jwts.builder().subject(EMAIL).expiration(Date.from(Instant.EPOCH)).signWith(chave).compact(),
                Jwts.builder().subject(EMAIL).signWith(chave).compact(),
                Jwts.builder().expiration(futuro).signWith(chave).compact()
        );
    }

    private UserDetails conta(boolean desativada) {
        return User.withUsername(EMAIL).password("senha-de-teste").roles("ALUNO").disabled(desativada).build();
    }

    private String emitirToken() {
        return tokens.generateToken(Aluno.builder().email(EMAIL).nome("Aluno").id(1L).perfil(Perfil.ALUNO).build());
    }

    private void filtrar(String token, UserDetailsService usuarios) throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        new JwtAuthFilter(tokens, usuarios).doFilter(request, response, chain);
    }

    private void assertRejeitado() throws Exception {
        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
        assertTrue(response.getContentAsString().isEmpty());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(chain.getRequest());
    }
}
