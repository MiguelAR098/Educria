package com.educria.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;

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
