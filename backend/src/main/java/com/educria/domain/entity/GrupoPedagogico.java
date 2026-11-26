package com.educria.domain.entity;

import com.educria.domain.enums.TipoGrupoPedagogico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** Representa um grupo ou projeto criado por um professor para uma ou mais de suas turmas. */
@Entity
@Table(name = "grupos_pedagogicos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GrupoPedagogico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Vários grupos podem pertencer ao mesmo professor. A chave estrangeira fica na tabela do grupo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false, updatable = false)
    private Professor professor;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private TipoGrupoPedagogico tipo;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    private Instant arquivadoEm;

    /** A tabela de junção registra onde o grupo atua. Os alunos participantes têm vínculos próprios. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "grupo_turmas", joinColumns = @JoinColumn(name = "grupo_id"),
            inverseJoinColumns = @JoinColumn(name = "turma_id"))
    private Set<Turma> turmas = new HashSet<>();

    public GrupoPedagogico(Professor professor, String nome, String descricao, TipoGrupoPedagogico tipo,
                            Set<Turma> turmas, Instant criadoEm) {
        this.professor = professor;
        this.nome = nome;
        this.descricao = descricao;
        this.tipo = tipo;
        this.turmas.addAll(turmas);
        this.criadoEm = criadoEm;
    }

    public void atualizar(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    /** Registra o arquivamento uma única vez, na data de criação ou em data posterior. */
    public void arquivar(Instant agora) {
        if (arquivadoEm == null) arquivadoEm = agora.isBefore(criadoEm) ? criadoEm : agora;
    }
}
