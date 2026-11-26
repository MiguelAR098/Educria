package com.educria.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turma_id")
    private Turma turma;

    /**
     * Desempenho adaptativo é 1:1 e PRIVADO.
     * Não existe relacionamento com pontuação coletiva.
     */
    @OneToOne(mappedBy = "aluno", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DesempenhoAdaptativo desempenhoAdaptativo;

    public void vincularDesempenho(DesempenhoAdaptativo desempenho) {
        this.desempenhoAdaptativo = desempenho;
        if (desempenho != null) {
            desempenho.setAluno(this);
        }
    }
}
