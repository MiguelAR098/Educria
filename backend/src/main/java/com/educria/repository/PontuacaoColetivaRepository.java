package com.educria.repository;

import com.educria.domain.entity.PontuacaoColetiva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PontuacaoColetivaRepository extends JpaRepository<PontuacaoColetiva, Long> {
    Optional<PontuacaoColetiva> findByTurmaId(Long turmaId);

    /** Trava a linha para que dois lançamentos simultâneos não percam pontos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PontuacaoColetiva p where p.turma.id = :turmaId")
    Optional<PontuacaoColetiva> bloquearPorTurma(@Param("turmaId") Long turmaId);
}
