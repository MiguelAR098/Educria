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

    @ElementCollection
    @CollectionTable(name = "sessao_questoes", joinColumns = @JoinColumn(name = "sessao_id"))
    @OrderColumn(name = "indice")
    @Column(name = "questao_id", nullable = false)
    private List<Long> apresentadas = new ArrayList<>();

    public SessaoEstudo(Aluno aluno, Assunto assunto, Long primeiraQuestao) {
        this.aluno = aluno;
        this.assunto = assunto;
        situacao = SituacaoSessao.ATIVA;
        iniciadaEm = Instant.now().truncatedTo(ChronoUnit.MICROS);
        atualizadaEm = iniciadaEm;
        apresentadas.add(primeiraQuestao);
    }

    public ResumoSessao resumo() {
        return new ResumoSessao(id, assunto.getId(), situacao, apresentadas.size(), iniciadaEm, atualizadaEm);
    }

    public Set<Long> usadas() {
        return Set.copyOf(apresentadas);
    }

    public Long questaoAtual() {
        return apresentadas.getLast();
    }

    public boolean precisaApresentar(int posicao) {
        if (situacao != SituacaoSessao.ATIVA) throw conflito("A sessão precisa estar ativa");
        if (posicao < 1 || posicao > LIMITE) throw conflito("A posição deve estar entre 1 e 10");
        if (posicao > apresentadas.size() + 1) throw conflito("Solicite a próxima posição em ordem");
        return posicao == apresentadas.size() + 1;
    }

    public void apresentar(Long questaoId) {
        if (situacao != SituacaoSessao.ATIVA || apresentadas.size() >= LIMITE || apresentadas.contains(questaoId)) {
            throw conflito("Não é possível apresentar esta questão");
        }
        apresentadas.add(questaoId);
        atualizadaEm = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

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
