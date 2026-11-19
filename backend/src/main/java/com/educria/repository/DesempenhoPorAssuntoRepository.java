package com.educria.repository;

import com.educria.domain.entity.DesempenhoPorAssunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DesempenhoPorAssuntoRepository extends JpaRepository<DesempenhoPorAssunto, Long> {
    Optional<DesempenhoPorAssunto> findByAlunoIdAndAssuntoId(Long alunoId, Long assuntoId);
}
