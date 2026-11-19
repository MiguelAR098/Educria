package com.educria.repository;

import com.educria.domain.entity.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    Optional<Aluno> findByEmail(String email);

    @Query("SELECT a FROM Aluno a LEFT JOIN FETCH a.turma WHERE a.id = :id")
    Optional<Aluno> findByIdWithTurma(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Aluno a where a.email = :email")
    Optional<Aluno> bloquearPorEmail(@Param("email") String email);

    List<Aluno> findByTurmaId(Long turmaId);
}
