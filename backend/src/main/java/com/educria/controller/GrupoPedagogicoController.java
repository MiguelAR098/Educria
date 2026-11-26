package com.educria.controller;

import com.educria.domain.enums.TipoGrupoPedagogico;
import com.educria.dto.GrupoPedagogicoDto.Atualizacao;
import com.educria.dto.GrupoPedagogicoDto.Criacao;
import com.educria.dto.GrupoPedagogicoDto.Detalhe;
import com.educria.dto.GrupoPedagogicoDto.Inclusao;
import com.educria.dto.GrupoPedagogicoDto.Resumo;
import com.educria.service.GrupoPedagogicoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
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

@RestController
@RequestMapping("/api/professor/grupos")
@RequiredArgsConstructor
public class GrupoPedagogicoController {
    private final GrupoPedagogicoService grupos;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Resumo criar(@AuthenticationPrincipal UserDetails usuario, @Valid @RequestBody Criacao pedido) {
        return grupos.criar(usuario.getUsername(), pedido);
    }

    @GetMapping
    public List<Resumo> listar(@AuthenticationPrincipal UserDetails usuario,
                               @RequestParam(required = false) TipoGrupoPedagogico tipo,
                               @RequestParam(required = false) Boolean arquivado) {
        return grupos.listar(usuario.getUsername(), tipo, arquivado);
    }

    @GetMapping("/{id}")
    public Detalhe detalhar(@AuthenticationPrincipal UserDetails usuario, @PathVariable @Positive Long id) {
        return grupos.detalhar(usuario.getUsername(), id);
    }

    @PutMapping("/{id}")
    public Resumo atualizar(@AuthenticationPrincipal UserDetails usuario, @PathVariable @Positive Long id,
                            @Valid @RequestBody Atualizacao pedido) {
        return grupos.atualizar(usuario.getUsername(), id, pedido);
    }

    @PostMapping("/{id}/participantes")
    public Detalhe incluir(@AuthenticationPrincipal UserDetails usuario, @PathVariable @Positive Long id,
                           @Valid @RequestBody Inclusao pedido) {
        return grupos.incluir(usuario.getUsername(), id, pedido);
    }

    @DeleteMapping("/{id}/participantes/{alunoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal UserDetails usuario, @PathVariable @Positive Long id,
                        @PathVariable @Positive Long alunoId) {
        grupos.remover(usuario.getUsername(), id, alunoId);
    }

    @PatchMapping("/{id}/arquivar")
    public Resumo arquivar(@AuthenticationPrincipal UserDetails usuario, @PathVariable @Positive Long id) {
        return grupos.arquivar(usuario.getUsername(), id);
    }
}
