package com.educria.repository;

import com.educria.domain.entity.RepeticaoEspacada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RepeticaoEspacadaRepository extends JpaRepository<RepeticaoEspacada, Long> {

    Optional<RepeticaoEspacada> findByAlunoIdAndAtividadeId(Long alunoId, Long atividadeId);

    @Query("SELECT r FROM RepeticaoEspacada r WHERE r.aluno.id = :alunoId AND r.proximaRevisao <= :hoje")
    List<RepeticaoEspacada> findPendentesByAluno(Long alunoId, LocalDate hoje);

    List<RepeticaoEspacada> findByAlunoId(Long alunoId);
}
