package com.educria.controller;

import com.educria.dto.PerfilProfessor;
import com.educria.dto.TurmaComAlunos;
import com.educria.service.ProfessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professor")
@RequiredArgsConstructor
public class ProfessorController {

    private final ProfessorService professorService;

    @GetMapping("/perfil")
    public PerfilProfessor perfil(@AuthenticationPrincipal UserDetails usuario) {
        return professorService.perfil(usuario.getUsername());
    }

    @GetMapping("/turmas/{turmaId}")
    public TurmaComAlunos turma(@AuthenticationPrincipal UserDetails usuario, @PathVariable Long turmaId) {
        return professorService.turma(usuario.getUsername(), turmaId);
    }
}
