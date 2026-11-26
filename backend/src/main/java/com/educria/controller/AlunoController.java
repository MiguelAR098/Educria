package com.educria.controller;

import com.educria.dto.AgendaRevisoes;
import com.educria.dto.AssuntoDoAluno;
import com.educria.dto.DesempenhoResponse;
import com.educria.dto.MateriaResumo;
import com.educria.dto.PontuacaoColetivaResponse;
import com.educria.service.AlunoService;
import com.educria.service.AgendaRevisoesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aluno")
@RequiredArgsConstructor
public class AlunoController {

    private final AlunoService alunoService;
    private final com.educria.repository.AlunoRepository alunoRepository;

    private final AgendaRevisoesService revisoes;

    private Long getAlunoId(UserDetails userDetails) {
        return alunoRepository.findByEmail(userDetails.getUsername())
                .orElseThrow()
                .getId();
    }

    @GetMapping("/revisoes")
    public AgendaRevisoes revisoes(@AuthenticationPrincipal UserDetails usuario) {
        return revisoes.listar(getAlunoId(usuario));
    }

    @GetMapping("/materias")
    public List<MateriaResumo> materias() {
        return alunoService.listarMaterias();
    }

    @GetMapping("/materias/{materiaId}/assuntos")
    public List<AssuntoDoAluno> assuntos(@PathVariable Long materiaId,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        return alunoService.listarAssuntos(materiaId, getAlunoId(userDetails));
    }

    @GetMapping("/desempenho")
    public ResponseEntity<DesempenhoResponse> meuDesempenho(@AuthenticationPrincipal UserDetails userDetails) {
        Long alunoId = getAlunoId(userDetails);
        return ResponseEntity.ok(alunoService.getMeuDesempenho(alunoId));
    }

    @GetMapping("/turma/pontos")
    public ResponseEntity<PontuacaoColetivaResponse> pontosDaTurma(@AuthenticationPrincipal UserDetails userDetails) {
        Long alunoId = getAlunoId(userDetails);
        return ResponseEntity.ok(alunoService.getPontuacaoDaMinhaTurma(alunoId));
    }
}
