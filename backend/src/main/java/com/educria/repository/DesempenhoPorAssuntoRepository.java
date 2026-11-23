package com.educria.repository;

import com.educria.domain.entity.DesempenhoPorAssunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface DesempenhoPorAssuntoRepository extends JpaRepository<DesempenhoPorAssunto, Long> {
    Optional<DesempenhoPorAssunto> findByAlunoIdAndAssuntoId(Long alunoId, Long assuntoId);

    @Query("select d from DesempenhoPorAssunto d join fetch d.assunto where d.aluno.id = :alunoId")
    List<DesempenhoPorAssunto> doAluno(@Param("alunoId") Long alunoId);

    @Query("""
            select d from DesempenhoPorAssunto d
            join fetch d.assunto a
            join fetch d.aluno al
            where al.turma.id = :turmaId and a.materia.id = :materiaId
            """)
    List<DesempenhoPorAssunto> daTurma(@Param("turmaId") Long turmaId, @Param("materiaId") Long materiaId);
}
