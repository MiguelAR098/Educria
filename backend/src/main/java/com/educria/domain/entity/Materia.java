package com.educria.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;

/** Item do catálogo que agrupa assuntos e define a área de atuação do professor. */
@Entity
@Table(name = "materias")
@Getter
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;
}
