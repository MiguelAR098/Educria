package com.educria.repository;

import com.educria.domain.entity.Professor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {

    @Query("""
            select p from Professor p
            join fetch p.materia
            left join fetch p.turmas
            where p.email = :email
            """)
    Optional<Professor> buscarComTurmas(@Param("email") String email);
}
