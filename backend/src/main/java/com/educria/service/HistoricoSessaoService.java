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

/** Organiza o histórico privado de sessões e seus resultados, sempre no recorte do aluno autenticado. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class HistoricoSessaoService {
    private static final int POR_PAGINA = 20;
    private final HistoricoSessaoRepository historico;

    /**
     * Lista até vinte sessões por página, da mais nova para a mais antiga.
     * A consulta pede um item extra para saber se é preciso devolver um cursor.
     *
     * @param email identidade autenticada do titular
     * @param antesDe cursor exclusivo ou {@code null} para a primeira página
     * @return sessões da página e cursor da próxima, quando houver
     */
    public Historico listar(String email, Long antesDe) {
        var pagina = historico.listar(email, antesDe, POR_PAGINA + 1);
        if (pagina.size() <= POR_PAGINA) return new Historico(pagina, null);
        var itens = pagina.subList(0, POR_PAGINA);
        return new Historico(itens, itens.getLast().id());
    }

    /**
     * Combina as tentativas da sessão, a agenda das questões respondidas e os pontos acumulados no assunto e na matéria.
     * A referência para uma sessão encerrada é a sessão encerrada anterior do mesmo assunto. As consultas
     * compõem uma transação de leitura repetível. Uma sessão alheia tem o mesmo retorno que uma sessão inexistente.
     */
    public ResultadoSessao resultado(String email, long id) {
        var sessao = historico.obter(email, id)
                .orElseThrow(() -> new ApiException("Sessão não encontrada", HttpStatus.NOT_FOUND));
        var anterior = historico.anteriorComparavel(email, id).orElse(null);
        var pontos = historico.pontuacaoAcumulada(email, id);
        return new ResultadoSessao(sessao, anterior, historico.revisoes(email, id), historico.questoes(email, id),
                pontos.noAssunto(), pontos.naMateria());
    }
}
