package com.educria.controller;

import com.educria.dto.AssuntoSimples;
import com.educria.dto.NovoAssuntoRequest;
import com.educria.service.AssuntoDoProfessorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professor/assuntos")
@RequiredArgsConstructor
public class AssuntoDoProfessorController {

    private final AssuntoDoProfessorService assuntoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssuntoSimples criar(@AuthenticationPrincipal UserDetails usuario,
                                @Valid @RequestBody NovoAssuntoRequest request) {
        return assuntoService.criar(usuario.getUsername(), request.nome());
    }
}
