package com.educria.controller;

import com.educria.domain.entity.Turma;
import com.educria.service.AdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ── Turmas ──────────────────────────────────────────────────────────────

    @PostMapping("/turmas")
    public ResponseEntity<TurmaCriada> criarTurma(@Valid @RequestBody CriarTurmaRequest request) {
        Turma turma = adminService.criarTurma(request.getNome(), request.getCodigo(), request.getAnoLetivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(TurmaCriada.from(turma));
    }

    /** NOVO — lista todas as turmas com pontuação coletiva */
    @GetMapping("/turmas")
    public ResponseEntity<List<TurmaResumo>> listarTurmas() {
        return ResponseEntity.ok(adminService.listarTurmas());
    }

    /** NOVO — detalhe de uma turma com alunos */
    @GetMapping("/turmas/{turmaId}")
    public ResponseEntity<TurmaDetalhe> detalharTurma(@PathVariable Long turmaId) {
        return ResponseEntity.ok(adminService.detalharTurma(turmaId));
    }

    @PostMapping("/turmas/{turmaId}/pontos")
    public ResponseEntity<Void> lancarPontos(
            @PathVariable Long turmaId,
            @Valid @RequestBody LancarPontosRequest request) {
        adminService.lancarPontosColetivos(turmaId, request.getPontos());
        return ResponseEntity.ok().build();
    }

    // ── Alunos ──────────────────────────────────────────────────────────────

    @PostMapping("/turmas/{turmaId}/alunos/{alunoId}")
    public ResponseEntity<Void> vincularAluno(
            @PathVariable Long turmaId,
            @PathVariable Long alunoId) {
        adminService.vincularAlunoTurma(turmaId, alunoId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/turmas/{turmaId}/alunos/{alunoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincularAluno(@PathVariable Long turmaId, @PathVariable Long alunoId) {
        adminService.desvincularAlunoTurma(turmaId, alunoId);
    }

    /** NOVO — lista alunos de uma turma (nome, id, matricula) */
    @GetMapping("/turmas/{turmaId}/alunos")
    public ResponseEntity<List<AlunoResumo>> listarAlunosDaTurma(@PathVariable Long turmaId) {
        return ResponseEntity.ok(adminService.listarAlunosDaTurma(turmaId));
    }

    // ── Records / DTOs internos ──────────────────────────────────────────────

    public record TurmaCriada(Long id, String nome, String codigo, Integer anoLetivo, LocalDateTime criadoEm) {
        public static TurmaCriada from(Turma t) {
            return new TurmaCriada(t.getId(), t.getNome(), t.getCodigo(), t.getAnoLetivo(), t.getCriadoEm());
        }
    }

    public record TurmaResumo(Long id, String nome, String codigo, Integer anoLetivo,
                               int pontosTotais, long totalAlunos) {}

    public record TurmaDetalhe(Long id, String nome, String codigo, Integer anoLetivo,
                                int pontosTotais, List<AlunoResumo> alunos) {}

    public record AlunoResumo(Long id, String nome, String matricula) {}

    @Data
    public static class CriarTurmaRequest {
        @NotBlank private String nome;
        @NotBlank private String codigo;
        @NotNull  private Integer anoLetivo;
    }

    @Data
    public static class LancarPontosRequest {
        @Min(1) private int pontos;
    }
}
