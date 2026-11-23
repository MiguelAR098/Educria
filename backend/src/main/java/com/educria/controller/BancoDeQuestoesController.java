package com.educria.controller;

import com.educria.dto.AtivarQuestaoRequest;
import com.educria.dto.BancoDeQuestoes;
import com.educria.dto.QuestaoDoProfessor;
import com.educria.dto.QuestaoRequest;
import com.educria.service.BancoDeQuestoesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professor/questoes")
@RequiredArgsConstructor
public class BancoDeQuestoesController {

    private final BancoDeQuestoesService bancoService;

    @GetMapping
    public BancoDeQuestoes listar(@AuthenticationPrincipal UserDetails usuario,
                                  @RequestParam(required = false) Long assuntoId) {
        return bancoService.listar(usuario.getUsername(), assuntoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestaoDoProfessor criar(@AuthenticationPrincipal UserDetails usuario,
                                    @Valid @RequestBody QuestaoRequest request) {
        return bancoService.criar(usuario.getUsername(), request);
    }

    @PutMapping("/{id}")
    public QuestaoDoProfessor atualizar(@AuthenticationPrincipal UserDetails usuario, @PathVariable Long id,
                                        @Valid @RequestBody QuestaoRequest request) {
        return bancoService.atualizar(usuario.getUsername(), id, request);
    }

    @PatchMapping("/{id}/ativa")
    public QuestaoDoProfessor definirAtiva(@AuthenticationPrincipal UserDetails usuario, @PathVariable Long id,
                                           @Valid @RequestBody AtivarQuestaoRequest request) {
        return bancoService.definirAtiva(usuario.getUsername(), id, request.ativa());
    }
}
