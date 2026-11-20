package com.educria.controller;

import com.educria.dto.EnviarResposta;
import com.educria.dto.FeedbackResposta;
import com.educria.service.SessaoEstudoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/aluno/sessoes")
@RequiredArgsConstructor
public class RespostaSessaoController {
    private final SessaoEstudoService service;

    @PutMapping("/{id}/questoes/{questaoId}/resposta")
    public FeedbackResposta responder(Principal titular, @PathVariable Long id, @PathVariable Long questaoId,
                                      @Valid @RequestBody EnviarResposta pedido) {
        return service.responder(titular.getName(), id, questaoId, pedido.alternativa(), pedido.passo());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> pedidoInvalido() {
        return ResponseEntity.badRequest().body(Map.of("message", "Confira os IDs da sessão e da questão e envie a alternativa em JSON"));
    }
}
