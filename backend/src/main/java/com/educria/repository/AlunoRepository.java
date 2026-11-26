package com.educria.repository;

import com.educria.domain.entity.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    interface Identificacao {
        Long getId();
        String getNome();
    }

    interface CadastroNaTurma extends Identificacao {
        String getMatricula();
    }

    @Query("select a.id as id, a.nome as nome from Aluno a where a.turma.id = :turmaId")
    List<Identificacao> identificarAlunosDaTurma(@Param("turmaId") Long turmaId);

    Optional<Aluno> findByEmail(String email);

    @Query("SELECT a FROM Aluno a LEFT JOIN FETCH a.turma WHERE a.id = :id")
    Optional<Aluno> findByIdWithTurma(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Aluno a where a.email = :email")
    Optional<Aluno> bloquearPorEmail(@Param("email") String email);

    List<Aluno> findByTurmaId(Long turmaId);

    @Query("""
            select a.id as id, a.nome as nome, a.matricula as matricula
            from Aluno a where a.turma.id = :turmaId and (:alunoId is null or a.id = :alunoId)
            order by a.nome
            """)
    List<CadastroNaTurma> cadastrosNaTurma(@Param("turmaId") Long turmaId, @Param("alunoId") Long alunoId);

    Optional<Aluno> findByIdAndTurmaId(Long id, Long turmaId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "update dbo.alunos set turma_id = null where usuario_id = :alunoId and turma_id = :turmaId", nativeQuery = true)
    int desvincularDaTurma(@Param("alunoId") Long alunoId, @Param("turmaId") Long turmaId);
}
