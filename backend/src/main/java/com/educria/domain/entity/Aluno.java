package com.educria.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Especialização de {@link Usuario} com matrícula, turma atual e diagnóstico privado.
 * {@code @PrimaryKeyJoinColumn} liga a chave de {@code alunos} à mesma identidade em {@code usuarios}.
 * {@code @SuperBuilder}, do Lombok, permite preencher campos próprios e herdados na mesma construção.
 */
@Entity
@Table(name = "alunos")
@PrimaryKeyJoinColumn(name = "usuario_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Aluno extends Usuario {

    @Column(name = "matricula", length = 30)
    private String matricula;

    // Vários alunos podem apontar para a mesma turma. LAZY adia sua leitura até o uso.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turma_id")
    private Turma turma;

    /**
     * Desempenho adaptativo é 1:1 e PRIVADO.
     * Não existe relacionamento com pontuação coletiva.
     */
    @OneToOne(mappedBy = "aluno", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DesempenhoAdaptativo desempenhoAdaptativo;

    /** Mantém os dois lados da associação coerentes. O diagnóstico é o lado que grava a chave do aluno. */
    public void vincularDesempenho(DesempenhoAdaptativo desempenho) {
        this.desempenhoAdaptativo = desempenho;
        if (desempenho != null) {
            desempenho.setAluno(this);
        }
    }
}
