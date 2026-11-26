package com.educria.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Guarda um intervalo de participação de um aluno em um grupo.
 * A entidade de vínculo permite registrar a turma de origem e as datas de entrada e saída.
 */
@Entity
@Table(name = "grupo_participacoes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipacaoGrupo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false, updatable = false)
    private GrupoPedagogico grupo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false, updatable = false)
    private Aluno aluno;

    /** Preserva a turma registrada na entrada mesmo que o aluno mude de turma posteriormente. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turma_origem_id", nullable = false, updatable = false)
    private Turma turmaOrigem;

    @Column(nullable = false, updatable = false)
    private Instant entradaEm;

    private Instant saidaEm;

    public ParticipacaoGrupo(GrupoPedagogico grupo, Aluno aluno, Turma turmaOrigem, Instant entradaEm) {
        this.grupo = grupo;
        this.aluno = aluno;
        this.turmaOrigem = turmaOrigem;
        this.entradaEm = entradaEm;
    }

    /** Encerra um vínculo vigente sem reescrever a data de um vínculo já encerrado. */
    public void sair(Instant agora) {
        if (saidaEm == null) saidaEm = agora.isBefore(entradaEm) ? entradaEm : agora;
    }
}
