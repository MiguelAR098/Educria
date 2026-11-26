package com.educria.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "turma", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Aluno> alunos = new ArrayList<>();

    /**
     * Pontuação coletiva da turma (pública).
     * Isolada do desempenho individual.
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

    public void vincularPontuacao(PontuacaoColetiva pontuacao) {
        this.pontuacaoColetiva = pontuacao;
        if (pontuacao != null) {
            pontuacao.setTurma(this);
        }
    }
}
