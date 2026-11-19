package com.educria.controller;

import com.educria.domain.entity.Turma;
import com.educria.service.AdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/turmas")
    public ResponseEntity<Turma> criarTurma(@RequestBody CriarTurmaRequest request) {
        Turma turma = adminService.criarTurma(request.getNome(), request.getCodigo(), request.getAnoLetivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(turma);
    }

    @PostMapping("/turmas/{turmaId}/pontos")
    public ResponseEntity<Void> lancarPontos(@PathVariable Long turmaId, @RequestBody LancarPontosRequest request) {
        adminService.lancarPontosColetivos(turmaId, request.getPontos());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/turmas/{turmaId}/alunos/{alunoId}")
    public ResponseEntity<Void> vincularAluno(@PathVariable Long turmaId, @PathVariable Long alunoId) {
        adminService.vincularAlunoTurma(turmaId, alunoId);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class CriarTurmaRequest {
        private String nome;
        private String codigo;
        private Integer anoLetivo;
    }

    @Data
    public static class LancarPontosRequest {
        private int pontos;
    }
}
