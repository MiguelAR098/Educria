package com.educria.service;

import com.educria.dto.ResultadoSessao;
import com.educria.dto.ResultadoSessao.Historico;
import com.educria.exception.ApiException;
import com.educria.repository.HistoricoSessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class HistoricoSessaoService {
    private static final int POR_PAGINA = 20;
    private final HistoricoSessaoRepository historico;

    public Historico listar(String email, Long antesDe) {
        var pagina = historico.listar(email, antesDe, POR_PAGINA + 1);
        if (pagina.size() <= POR_PAGINA) return new Historico(pagina, null);
        var itens = pagina.subList(0, POR_PAGINA);
        return new Historico(itens, itens.getLast().id());
    }

    public ResultadoSessao resultado(String email, long id) {
        var sessao = historico.obter(email, id)
                .orElseThrow(() -> new ApiException("Sessão não encontrada", HttpStatus.NOT_FOUND));
        var anterior = historico.anteriorComparavel(email, id).orElse(null);
        var pontos = historico.pontuacaoAcumulada(email, id);
        return new ResultadoSessao(sessao, anterior, historico.revisoes(email, id), historico.questoes(email, id),
                pontos.noAssunto(), pontos.naMateria());
    }
}
