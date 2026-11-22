package com.educria.domain.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "professores")
@PrimaryKeyJoinColumn(name = "usuario_id")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Professor extends Usuario {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    /**
     * Turmas em que o professor leciona.
     * O professor só enxerga alunos e dados dessas turmas.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "professor_turmas",
            joinColumns = @JoinColumn(name = "professor_id"),
            inverseJoinColumns = @JoinColumn(name = "turma_id")
    )
    @Builder.Default
    private Set<Turma> turmas = new HashSet<>();

    public boolean lecionaNaTurma(Long turmaId) {
        return turmas.stream().anyMatch(t -> t.getId().equals(turmaId));
    }
}
