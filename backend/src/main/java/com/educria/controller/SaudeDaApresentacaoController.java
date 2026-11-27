package com.educria.controller;

import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("apresentacao")
class SaudeDaApresentacaoController {
    private final ApplicationAvailability disponibilidade;

    SaudeDaApresentacaoController(ApplicationAvailability disponibilidade) {
        this.disponibilidade = disponibilidade;
    }

    @GetMapping("/actuator/health")
    ResponseEntity<Void> pronta() {
        boolean aceitando = disponibilidade.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC;
        return ResponseEntity.status(aceitando ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore()).build();
    }
}
