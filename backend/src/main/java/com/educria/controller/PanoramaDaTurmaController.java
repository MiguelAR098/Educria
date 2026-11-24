package com.educria.controller;

import com.educria.dto.PanoramaDaTurma;
import com.educria.service.PanoramaDaTurmaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professor/turmas/{turmaId}/panorama")
@RequiredArgsConstructor
public class PanoramaDaTurmaController {

    private final PanoramaDaTurmaService panoramaService;

    @GetMapping
    public PanoramaDaTurma panorama(@AuthenticationPrincipal UserDetails usuario, @PathVariable Long turmaId) {
        return panoramaService.panorama(usuario.getUsername(), turmaId);
    }
}
