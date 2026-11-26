package com.educria.repository;

import com.educria.domain.entity.ParticipacaoGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParticipacaoGrupoRepository extends JpaRepository<ParticipacaoGrupo, Long> {
    interface AlunoElegivel {
        Long getAlunoId();
        Long getTurmaId();
    }

    interface ParticipanteVisivel {
        Long getAlunoId();
        String getNome();
        Long getTurmaOrigemId();
        Long getTurmaAtualId();
        Instant getEntradaEm();
    }

    @Query("""
            select a.id as alunoId, a.turma.id as turmaId from Aluno a
            where a.id in :alunosIds and a.ativo = true and a.turma.id in :turmasIds
            """)
    List<AlunoElegivel> alunosElegiveis(@Param("alunosIds") Collection<Long> alunosIds,
                                      @Param("turmasIds") Collection<Long> turmasIds);

    @Query("""
            select a.id as alunoId, a.nome as nome, p.turmaOrigem.id as turmaOrigemId,
                   a.turma.id as turmaAtualId, p.entradaEm as entradaEm
            from ParticipacaoGrupo p join p.aluno a
            where p.grupo.id = :grupoId and p.saidaEm is null and a.ativo = true
              and a.turma.id in :turmasIds
            order by a.nome, a.id
            """)
    List<ParticipanteVisivel> participantesVisiveis(@Param("grupoId") Long grupoId,
                                                  @Param("turmasIds") Collection<Long> turmasIds);

    @Query("""
            select p.aluno.id from ParticipacaoGrupo p
            where p.grupo.id = :grupoId and p.saidaEm is null and p.aluno.id in :alunosIds
            """)
    List<Long> idsJaParticipantes(@Param("grupoId") Long grupoId, @Param("alunosIds") Collection<Long> alunosIds);

    Optional<ParticipacaoGrupo> findByGrupoIdAndAlunoIdAndSaidaEmIsNull(Long grupoId, Long alunoId);
}
