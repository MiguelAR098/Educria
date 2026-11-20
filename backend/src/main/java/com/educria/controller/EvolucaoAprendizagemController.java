package com.educria.controller;

import com.educria.dto.RelatorioEvolucao;
import com.educria.exception.ApiException;
import com.educria.service.EvolucaoAprendizagemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/aluno/evolucao")
@RequiredArgsConstructor
public class EvolucaoAprendizagemController {
    private final EvolucaoAprendizagemService evolucao;

    @GetMapping
    public RelatorioEvolucao obter(Principal titular, @RequestParam(required = false) String assuntoId) {
        return evolucao.obter(titular.getName(), idDoAssunto(assuntoId));
    }

    private static Long idDoAssunto(String parametro) {
        if (parametro == null) return null;
        try {
            long id = Long.parseLong(parametro);
            if (id > 0) return id;
        } catch (NumberFormatException ex) {
            throw new ApiException("assuntoId deve ser um inteiro positivo", HttpStatus.BAD_REQUEST);
        }
        throw new ApiException("assuntoId deve ser um inteiro positivo", HttpStatus.BAD_REQUEST);
    }
}
