package com.educria.domain.entity;

import com.educria.domain.enums.SituacaoSessao;
import com.educria.dto.ResumoSessao;
import com.educria.exception.ApiException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Guarda a sequência de estudo de um aluno sobre um assunto. A entidade controla os passos e as
 * transições da sessão, e o serviço coordena seleção, respostas e agenda.
 * O construtor protegido gerado por Lombok permite ao JPA reconstruir sessões já persistidas.
 */
@Entity
@Table(name = "sessoes_estudo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessaoEstudo {
    public static final int LIMITE = 10;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;
    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assunto_id", nullable = false)
    private Assunto assunto;
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SituacaoSessao situacao;
    private Instant iniciadaEm;
    private Instant atualizadaEm;

    @Getter
    @Column(name = "revisoes_apresentadas", nullable = false)
    private int revisoesApresentadas;

    // Cada linha de sessao_questoes guarda o ID e a posição de uma questão distinta na lista.
    @ElementCollection
    @CollectionTable(name = "sessao_questoes", joinColumns = @JoinColumn(name = "sessao_id"))
    @OrderColumn(name = "indice")
    @Column(name = "questao_id", nullable = false)
    private List<Long> apresentadas = new ArrayList<>();

    /** Abre uma sessão ativa com a primeira questão e contabiliza a revisão se ela veio da agenda. */
    public SessaoEstudo(Aluno aluno, Assunto assunto, Long primeiraQuestao, boolean revisao) {
        this.aluno = aluno;
        this.assunto = assunto;
        situacao = SituacaoSessao.ATIVA;
        iniciadaEm = Instant.now().truncatedTo(ChronoUnit.MICROS);
        atualizadaEm = iniciadaEm;
        apresentadas.add(primeiraQuestao);
        revisoesApresentadas = revisao ? 1 : 0;
    }

    public ResumoSessao resumo() {
        return new ResumoSessao(id, assunto.getId(), situacao, apresentadas.size(), iniciadaEm, atualizadaEm);
    }

    public Long alunoId() {
        return aluno.getId();
    }

    // O passo identifica a tentativa apresentada. Pode ultrapassar dez em sessões com reaprendizagem.
    @Getter
    private int passo = 1;
    private Long questaoRetomadaId;

    public boolean repetindo() {
        return questaoRetomadaId != null;
    }

    /** Avança uma tentativa para uma questão já apresentada, mantendo o total de questões distintas. */
    public void reapresentar(Long questaoId) {
        if (!apresentadas.contains(questaoId)) throw conflito("Questão fora da sessão");
        questaoRetomadaId = questaoId;
        avancarPasso();
    }

    private void avancarPasso() {
        passo++;
        atualizadaEm = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Set<Long> usadas() {
        return Set.copyOf(apresentadas);
    }

    public Long questaoAtual() {
        if (repetindo()) return questaoRetomadaId;
        return apresentadas.isEmpty() ? null : apresentadas.getLast();
    }

    /**
     * Autoriza somente o próximo passo em uma sessão ativa. Pedidos de passos já apresentados
     * retornam {@code false} para que o serviço devolva o estado atual sem avançar novamente.
     */
    public boolean precisaApresentar(int posicao) {
        if (situacao != SituacaoSessao.ATIVA) throw conflito("A sessão precisa estar ativa");
        if (posicao < 1 || posicao > passo + 1) throw conflito("Solicite a próxima posição em ordem");
        return posicao == passo + 1;
    }

    /** Acrescenta uma questão distinta, respeitando o limite de dez e encerrando a exibição da repetição anterior. */
    public void apresentar(Long questaoId, boolean revisao) {
        if (situacao != SituacaoSessao.ATIVA || apresentadas.size() >= LIMITE || apresentadas.contains(questaoId)) {
            throw conflito("Não é possível apresentar esta questão");
        }
        apresentadas.add(questaoId);
        if (revisao) revisoesApresentadas++;
        questaoRetomadaId = null;
        avancarPasso();
    }

    /** Preserva o passo ao pausar ou retomar. Repetir a mesma transição é seguro e o encerramento é definitivo. */
    public void mudarSituacao(SituacaoSessao destino) {
        if (situacao == destino) return;
        if (situacao == SituacaoSessao.ENCERRADA) throw conflito("A sessão já foi encerrada");
        situacao = destino;
        atualizadaEm = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private ApiException conflito(String mensagem) {
        return new ApiException(mensagem, HttpStatus.CONFLICT);
    }
}
