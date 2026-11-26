package com.educria.controller;

import com.educria.dto.RankingsDaTurma;
import com.educria.service.PanoramaDaTurmaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professor/turmas/{turmaId}/rankings")
@RequiredArgsConstructor
public class RankingsDaTurmaController {
    private final PanoramaDaTurmaService panoramaService;

    @GetMapping
    public ResponseEntity<RankingsDaTurma> rankings(@AuthenticationPrincipal UserDetails usuario,
                                                     @PathVariable Long turmaId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(panoramaService.rankings(usuario.getUsername(), turmaId));
    }
}
