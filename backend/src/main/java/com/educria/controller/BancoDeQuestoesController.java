package com.educria.controller;

import com.educria.dto.AtivarQuestaoRequest;
import com.educria.dto.BancoDeQuestoes;
import com.educria.dto.CoberturaDoBanco;
import com.educria.dto.QuestaoDoProfessor;
import com.educria.dto.QuestaoExportada;
import com.educria.dto.QuestaoRequest;
import com.educria.dto.ResultadoImportacao;
import com.educria.service.BancoDeQuestoesService;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.List;

/**
 * Interface HTTP do banco da matéria do professor. PUT recebe os campos editáveis da questão,
 * enquanto PATCH altera somente a disponibilidade. A cobertura tem uma consulta agregada própria.
 */
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

    @GetMapping("/cobertura")
    public CoberturaDoBanco cobertura(@AuthenticationPrincipal UserDetails usuario) {
        return bancoService.cobertura(usuario.getUsername());
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

    @GetMapping("/exportar")
    public List<QuestaoExportada> exportar(@AuthenticationPrincipal UserDetails usuario) {
        return bancoService.exportar(usuario.getUsername());
    }

    /** Recebe uma lista JSON; cada item é validado à parte, então um item ruim não derruba os outros. */
    @PostMapping("/importar")
    public ResultadoImportacao importar(@AuthenticationPrincipal UserDetails usuario, @RequestBody JsonNode corpo) {
        return bancoService.importar(usuario.getUsername(), corpo);
    }
}
