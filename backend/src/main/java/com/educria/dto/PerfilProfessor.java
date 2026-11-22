package com.educria.dto;

import java.util.List;

public record PerfilProfessor(Long id, String nome, String email, String materia, List<TurmaDoProfessor> turmas) {}
