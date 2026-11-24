package com.educria.controller;

import com.educria.dto.HistoricoDeAtividades;
import com.educria.service.AtividadesDaTurmaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aluno/atividades-da-turma")
@RequiredArgsConstructor
public class AtividadesDaTurmaAlunoController {

    private final AtividadesDaTurmaService atividadesService;

    @GetMapping
    public HistoricoDeAtividades daMinhaTurma(@AuthenticationPrincipal UserDetails usuario) {
        return atividadesService.daMinhaTurma(usuario.getUsername());
    }
}
