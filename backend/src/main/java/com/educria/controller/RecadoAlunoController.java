package com.educria.controller;

import com.educria.dto.RecadosDoAluno;
import com.educria.service.RecadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lado do aluno dos recados enviados por professores. */
@RestController
@RequestMapping("/api/aluno/recados")
@RequiredArgsConstructor
public class RecadoAlunoController {

    private final RecadoService recadoService;

    @GetMapping
    public RecadosDoAluno meusRecados(@AuthenticationPrincipal UserDetails usuario) {
        return recadoService.meusRecados(usuario.getUsername());
    }

    @PostMapping("/lidos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarComoLidos(@AuthenticationPrincipal UserDetails usuario) {
        recadoService.marcarComoLidos(usuario.getUsername());
    }
}
