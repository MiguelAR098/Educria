package com.educria.controller;

import com.educria.dto.ResultadoSessao;
import com.educria.dto.ResultadoSessao.Historico;
import com.educria.service.HistoricoSessaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.security.Principal;

@RestController
@RequestMapping("/api/aluno/sessoes")
@RequiredArgsConstructor
public class HistoricoSessaoController {
    private final HistoricoSessaoService historico;

    @GetMapping("/historico")
    public Historico historico(Principal titular, @RequestParam(required = false) Long antesDe) {
        return historico.listar(titular.getName(), antesDe);
    }

    @GetMapping("/{id}/resultado")
    public ResultadoSessao resultado(Principal titular, @PathVariable long id) {
        return historico.resultado(titular.getName(), id);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void parametroInvalido() {}
}
