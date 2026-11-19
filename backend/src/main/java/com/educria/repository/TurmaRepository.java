package com.educria.repository;

import com.educria.domain.entity.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

    Optional<Turma> findByCodigo(String codigo);

    @Query("SELECT t FROM Turma t LEFT JOIN FETCH t.pontuacaoColetiva WHERE t.id = :id")
    Optional<Turma> findByIdWithPontuacao(Long id);
}
