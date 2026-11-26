package com.educria.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Representa uma turma identificada pelo banco. Código e ano letivo são atributos do cadastro. */
@Entity
@Table(name = "turmas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 20)
    private String codigo; // ex: 8A, 9B

    @Column(name = "ano_letivo")
    private Integer anoLetivo;

    /** A chave estrangeira pertence ao cadastro do aluno. A coleção é carregada sob demanda. */
    @OneToMany(mappedBy = "turma", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Aluno> alunos = new ArrayList<>();

    /**
     * Saldo coletivo da turma, separado do desempenho individual.
     * A cascata mantém o ciclo de persistência do saldo associado ao ciclo da turma.
     */
    @OneToOne(mappedBy = "turma", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PontuacaoColetiva pontuacaoColetiva;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    public void adicionarAluno(Aluno aluno) {
        alunos.add(aluno);
        aluno.setTurma(this);
    }

    /** Sincroniza os dois lados da associação, inclusive o lado que grava a chave estrangeira. */
    public void vincularPontuacao(PontuacaoColetiva pontuacao) {
        this.pontuacaoColetiva = pontuacao;
        if (pontuacao != null) {
            pontuacao.setTurma(this);
        }
    }
}
