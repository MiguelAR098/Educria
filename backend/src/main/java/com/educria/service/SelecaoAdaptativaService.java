package com.educria.service;

import com.educria.domain.NivelAdaptativo;
import com.educria.dto.QuestaoParaAluno;
import com.educria.repository.QuestaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SelecaoAdaptativaService {

    private static final int MAX_REVISOES = 3;

    private final QuestaoRepository questaoRepository;

    /**
     * As usadas são as questões já apresentadas nesta sequência. O mapa contém
     * as datas da próxima revisão das questões já vistas pelo aluno. Uma questão
     * sem entrada é nova. O chamador fornece o estado e a data de referência.
     */
    public record Contexto(Set<Long> usadas, Map<Long, LocalDate> proximasRevisoes,
                           int revisoesUsadas, LocalDate hoje) {
        public Contexto {
            usadas = Set.copyOf(usadas);
            proximasRevisoes = Map.copyOf(proximasRevisoes);
            Objects.requireNonNull(hoje, "Data de referência obrigatória");
            if (revisoesUsadas < 0) {
                throw new IllegalArgumentException("Quantidade de revisões usadas deve ser não negativa");
            }
        }

        private boolean elegivel(QuestaoParaAluno questao) {
            LocalDate data = proximasRevisoes.get(questao.id());
            return !usadas.contains(questao.id())
                    && (data == null || (revisoesUsadas < MAX_REVISOES && !data.isAfter(hoje)));
        }
    }

    /** Retorna vazio ao esgotar o assunto. A seleção não consome nem agenda a questão. */
    @Transactional(readOnly = true)
    public Optional<QuestaoParaAluno> selecionar(Long assuntoId, int nivel, Contexto contexto) {
        Objects.requireNonNull(assuntoId, "Assunto obrigatório");
        Objects.requireNonNull(contexto, "Contexto obrigatório");
        if (nivel < NivelAdaptativo.MINIMO || nivel > NivelAdaptativo.MAXIMO) {
            throw new IllegalArgumentException("O nível deve estar entre 1 e 10");
        }

        // Entre as elegíveis, uma questão com revisão tem prioridade sobre uma nova.
        Comparator<QuestaoParaAluno> prioridade = Comparator
                .comparing((QuestaoParaAluno q) -> !contexto.proximasRevisoes().containsKey(q.id()))
                .thenComparingInt(q -> Math.abs(q.dificuldade() - nivel))
                .thenComparingInt(QuestaoParaAluno::dificuldade)
                .thenComparingLong(QuestaoParaAluno::id);

        return questaoRepository.listarParaSelecao(assuntoId).stream()
                .filter(contexto::elegivel)
                .min(prioridade);
    }
}
