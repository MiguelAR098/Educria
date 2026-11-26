package com.educria.controller;

import com.educria.domain.enums.SituacaoSessao;
import com.educria.dto.*;
import com.educria.service.SessaoEstudoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/aluno/sessoes")
@RequiredArgsConstructor
public class SessaoEstudoController {
    private final SessaoEstudoService service;

    @PostMapping
    public ResponseEntity<EstadoSessao> iniciar(Principal titular,
                                                @Valid @RequestBody(required = false) IniciarSessaoRequest pedido) {
        var inicio = service.iniciar(titular.getName(), pedido == null ? null : pedido.assuntoId());
        return ResponseEntity.status(inicio.criada() ? HttpStatus.CREATED : HttpStatus.OK).body(inicio.estado());
    }

    @GetMapping
    public List<ResumoSessao> listar(Principal titular) {
        return service.listar(titular.getName());
    }

    @GetMapping("/{id}")
    public EstadoSessao consultar(Principal titular, @PathVariable Long id) {
        return service.consultar(titular.getName(), id);
    }

    @PutMapping("/{id}/questoes/{posicao}")
    public EstadoSessao apresentar(Principal titular, @PathVariable Long id, @PathVariable int posicao) {
        return service.apresentar(titular.getName(), id, posicao);
    }

    @PutMapping("/{id}/interromper")
    public EstadoSessao interromper(Principal titular, @PathVariable Long id) {
        return service.mudarSituacao(titular.getName(), id, SituacaoSessao.INTERROMPIDA);
    }

    @PutMapping("/{id}/retomar")
    public EstadoSessao retomar(Principal titular, @PathVariable Long id) {
        return service.mudarSituacao(titular.getName(), id, SituacaoSessao.ATIVA);
    }

    @PutMapping("/{id}/encerrar")
    public EstadoSessao encerrar(Principal titular, @PathVariable Long id) {
        return service.mudarSituacao(titular.getName(), id, SituacaoSessao.ENCERRADA);
    }
}
