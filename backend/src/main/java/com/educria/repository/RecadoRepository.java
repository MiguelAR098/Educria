package com.educria.repository;

import com.educria.domain.entity.Recado;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RecadoRepository extends JpaRepository<Recado, Long> {

    @Query("""
            select r from Recado r
            left join fetch r.professor
            where r.aluno.id = :alunoId
            order by r.criadoEm desc, r.id desc
            """)
    List<Recado> doAluno(@Param("alunoId") Long alunoId, Pageable pagina);

    /** Limita a leitura do professor aos recados de sua autoria enviados ao destinatário. */
    @Query("""
            select r from Recado r
            join fetch r.professor p
            where r.aluno.id = :alunoId and p.id = :professorId
            order by r.criadoEm desc, r.id desc
            """)
    List<Recado> doAlunoPorProfessor(@Param("alunoId") Long alunoId, @Param("professorId") Long professorId,
                                     Pageable pagina);

    long countByAlunoIdAndLidoEmIsNull(Long alunoId);

    /** Atualização em lote pelo destinatário que preserva as datas das leituras anteriores. */
    @Modifying
    @Query("update Recado r set r.lidoEm = :agora where r.aluno.id = :alunoId and r.lidoEm is null")
    int marcarTodosComoLidos(@Param("alunoId") Long alunoId, @Param("agora") LocalDateTime agora);
}
