package com.educria.repository;

import com.educria.domain.entity.Professor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {

    interface LinhaDoPerfil {
        Long getProfessorId();
        String getProfessorNome();
        String getProfessorEmail();
        String getMateriaNome();
        Long getTurmaId();
        String getTurmaNome();
        String getTurmaCodigo();
        Integer getTurmaAnoLetivo();
        long getTotalAlunos();
    }

    @Query("""
            select p.id as professorId, p.nome as professorNome, p.email as professorEmail, m.nome as materiaNome,
                   t.id as turmaId, t.nome as turmaNome, t.codigo as turmaCodigo, t.anoLetivo as turmaAnoLetivo,
                   count(a) as totalAlunos
            from Professor p join p.materia m left join p.turmas t
            left join Aluno a on a.turma.id = t.id
            where p.email = :email
            group by p.id, p.nome, p.email, m.nome, t.id, t.nome, t.codigo, t.anoLetivo
            """)
    List<LinhaDoPerfil> linhasDoPerfil(@Param("email") String email);

    @Query("""
            select p from Professor p
            join fetch p.materia
            left join fetch p.turmas
            where p.email = :email
            """)
    Optional<Professor> buscarComTurmas(@Param("email") String email);
}
