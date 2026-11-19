package com.educria.repository;

import com.educria.domain.entity.DesempenhoAdaptativo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DesempenhoAdaptativoRepository extends JpaRepository<DesempenhoAdaptativo, Long> {
    Optional<DesempenhoAdaptativo> findByAlunoId(Long alunoId);
}
