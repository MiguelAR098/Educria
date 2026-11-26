package com.educria.controller;

import com.educria.dto.AuthResponse;
import com.educria.dto.LoginRequest;
import com.educria.dto.RegisterAlunoRequest;
import com.educria.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Porta de entrada HTTP do cadastro e do login. {@code @RestController} serializa o retorno,
 * e {@code @RequestMapping} define o prefixo compartilhado pelas rotas.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Recebe o JSON, aplica a validação do DTO e devolve HTTP 201 após o serviço criar o aluno. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterAlunoRequest request) {
        AuthResponse response = authService.registerAluno(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
