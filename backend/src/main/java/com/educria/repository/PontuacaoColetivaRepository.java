package com.educria.repository;

import com.educria.domain.entity.PontuacaoColetiva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PontuacaoColetivaRepository extends JpaRepository<PontuacaoColetiva, Long> {
    Optional<PontuacaoColetiva> findByTurmaId(Long turmaId);
}
