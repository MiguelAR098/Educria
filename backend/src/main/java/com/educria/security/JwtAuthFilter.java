package com.educria.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Interpreta a autenticação Bearer antes da autorização da rota. {@link OncePerRequestFilter}
 * organiza a execução do filtro por requisição, e o contexto do Spring Security fornece o usuário ao controller.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        var context = SecurityContextHolder.getContext();
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)
                || context.getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Recarrega a conta e suas permissões do banco. Desativação e troca de perfil valem mesmo com JWT vigente.
        UserDetails userDetails;
        try {
            String userEmail = jwtService.extractUsername(authHeader.substring(7));
            userDetails = userDetailsService.loadUserByUsername(userEmail);
            if (!userDetails.isEnabled() || !userEmail.equals(userDetails.getUsername())) {
                rejeitar(response);
                return;
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            rejeitar(response);
            return;
        }

        var authToken = UsernamePasswordAuthenticationToken.authenticated(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        context.setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }

    private void rejeitar(HttpServletResponse response) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
