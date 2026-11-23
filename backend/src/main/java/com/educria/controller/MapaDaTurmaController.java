package com.educria.controller;

import com.educria.dto.MapaDaTurma;
import com.educria.service.MapaDaTurmaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professor/turmas/{turmaId}/mapa")
@RequiredArgsConstructor
public class MapaDaTurmaController {

    private final MapaDaTurmaService mapaService;

    @GetMapping
    public MapaDaTurma mapa(@AuthenticationPrincipal UserDetails usuario, @PathVariable Long turmaId) {
        return mapaService.mapa(usuario.getUsername(), turmaId);
    }
}
