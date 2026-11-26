package com.educria.controller;

import com.educria.dto.IndicadoresDoGrupo;
import com.educria.service.IndicadoresDoGrupoService;
import jakarta.validation.constraints.Positive;
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
@RequestMapping("/api/professor/grupos/{id}/indicadores")
@RequiredArgsConstructor
public class IndicadoresDoGrupoController {
    private final IndicadoresDoGrupoService servico;

    @GetMapping
    public ResponseEntity<IndicadoresDoGrupo> indicadores(@AuthenticationPrincipal UserDetails usuario,
                                                          @PathVariable @Positive Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servico.indicadores(usuario.getUsername(), id));
    }
}
