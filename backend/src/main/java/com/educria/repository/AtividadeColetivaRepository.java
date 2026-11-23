package com.educria.repository;

import com.educria.domain.entity.AtividadeColetiva;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AtividadeColetivaRepository extends JpaRepository<AtividadeColetiva, Long> {

    @Query("""
            select a from AtividadeColetiva a
            left join fetch a.professor
            where a.turma.id = :turmaId
            order by a.criadoEm desc, a.id desc
            """)
    List<AtividadeColetiva> historico(@Param("turmaId") Long turmaId, Pageable pagina);
}
