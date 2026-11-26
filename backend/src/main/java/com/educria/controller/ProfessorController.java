package com.educria.controller;

import com.educria.dto.AtividadeColetivaResponse;
import com.educria.dto.DetalheDoAluno;
import com.educria.dto.EnviarRecadoRequest;
import com.educria.dto.HistoricoDeAtividades;
import com.educria.dto.LancarAtividadeRequest;
import com.educria.dto.PerfilProfessor;
import com.educria.dto.RecadoResponse;
import com.educria.dto.TurmaComAlunos;
import com.educria.service.AtividadeColetivaService;
import com.educria.service.ProfessorService;
import com.educria.service.RecadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/professor")
@RequiredArgsConstructor
public class ProfessorController {

    private final ProfessorService professorService;
    private final AtividadeColetivaService atividadeService;
    private final RecadoService recadoService;

    @GetMapping("/perfil")
    public PerfilProfessor perfil(@AuthenticationPrincipal UserDetails usuario) {
        return professorService.perfil(usuario.getUsername());
    }

    @GetMapping("/turmas/{turmaId}")
    public TurmaComAlunos turma(@AuthenticationPrincipal UserDetails usuario, @PathVariable Long turmaId) {
        return professorService.turma(usuario.getUsername(), turmaId);
    }

    @GetMapping("/turmas/{turmaId}/alunos/{alunoId}")
    public DetalheDoAluno aluno(@AuthenticationPrincipal UserDetails usuario,
                                @PathVariable Long turmaId, @PathVariable Long alunoId) {
        return professorService.detalheDoAluno(usuario.getUsername(), turmaId, alunoId);
    }

    @GetMapping("/turmas/{turmaId}/atividades")
    public HistoricoDeAtividades atividades(@AuthenticationPrincipal UserDetails usuario,
                                            @PathVariable Long turmaId) {
        return atividadeService.historico(usuario.getUsername(), turmaId);
    }

    @PostMapping("/turmas/{turmaId}/atividades")
    @ResponseStatus(HttpStatus.CREATED)
    public AtividadeColetivaResponse lancarAtividade(@AuthenticationPrincipal UserDetails usuario,
                                                     @PathVariable Long turmaId,
                                                     @Valid @RequestBody LancarAtividadeRequest request) {
        return atividadeService.lancar(usuario.getUsername(), turmaId, request);
    }

    @GetMapping("/turmas/{turmaId}/alunos/{alunoId}/recados")
    public List<RecadoResponse> recados(@AuthenticationPrincipal UserDetails usuario,
                                        @PathVariable Long turmaId, @PathVariable Long alunoId) {
        return recadoService.doAlunoParaProfessor(usuario.getUsername(), turmaId, alunoId);
    }

    @PostMapping("/turmas/{turmaId}/alunos/{alunoId}/recados")
    @ResponseStatus(HttpStatus.CREATED)
    public RecadoResponse enviarRecado(@AuthenticationPrincipal UserDetails usuario,
                                       @PathVariable Long turmaId, @PathVariable Long alunoId,
                                       @Valid @RequestBody EnviarRecadoRequest request) {
        return recadoService.enviar(usuario.getUsername(), turmaId, alunoId, request);
    }
}
