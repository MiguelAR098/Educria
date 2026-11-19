package com.educria.repository;

import com.educria.domain.entity.Atividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

    List<Atividade> findByAtivaTrue();

    @Query("SELECT a FROM Atividade a LEFT JOIN FETCH a.questoes WHERE a.id = :id")
    Optional<Atividade> findByIdWithQuestoes(Long id);
}
