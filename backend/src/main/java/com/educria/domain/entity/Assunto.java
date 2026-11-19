package com.educria.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "assuntos", uniqueConstraints = @UniqueConstraint(columnNames = {"materia_id", "nome"}))
@Getter
public class Assunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @Column(nullable = false, length = 100)
    private String nome;
}
