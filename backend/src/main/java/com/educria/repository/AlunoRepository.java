package com.educria.repository;

import com.educria.domain.entity.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    Optional<Aluno> findByEmail(String email);

    @Query("SELECT a FROM Aluno a LEFT JOIN FETCH a.turma WHERE a.id = :id")
    Optional<Aluno> findByIdWithTurma(Long id);

    List<Aluno> findByTurmaId(Long turmaId);
}
