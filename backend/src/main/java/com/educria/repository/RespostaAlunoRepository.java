package com.educria.repository;

import com.educria.domain.entity.RespostaAluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RespostaAlunoRepository extends JpaRepository<RespostaAluno, Long> {
    Optional<RespostaAluno> findBySessaoIdAndPasso(Long sessao, int passo);
    Optional<RespostaAluno> findFirstBySessaoIdAndQuestaoIdAndAlunoIdOrderByPasso(Long sessao, Long questao, Long aluno);
    Optional<RespostaAluno> findBySessaoIdAndPassoAndAlunoId(Long sessao, int passo, Long aluno);
    boolean existsBySessaoIdAndPasso(Long sessao, int passo);

    interface UltimaTentativa {
        boolean getCorreta();
    }

    Optional<UltimaTentativa> findFirstByAlunoIdAndQuestaoIdOrderByRespondidoEmDescIdDesc(Long aluno, Long questao);

    @Query("""
            select coalesce(sum(cast(r.pontosIndividuais as Long)), 0L) from RespostaAluno r
            where r.sessao.id = :sessao and r.aluno.id = :aluno and r.pontosIndividuais is not null
            """)
    long pontosDaSessao(@Param("sessao") Long sessao, @Param("aluno") Long aluno);

    @Query("""
            select count(r) from RespostaAluno r
            where r.aluno.id = :aluno and r.sessao.id = :sessao and r.questao.id = :questao
              and r.passo <= :passo and r.correta = false
            """)
    long contarErrosAtePasso(@Param("aluno") Long aluno, @Param("sessao") Long sessao,
                           @Param("questao") Long questao, @Param("passo") int passo);

    interface Pendencia {
        Long getQuestao();
        int getUltimoPasso();
    }

    @Query("""
            select r.questao.id as questao, max(r.passo) as ultimoPasso
            from RespostaAluno r where r.sessao.id = :sessao and r.aluno.id = :aluno
            group by r.questao.id
            having max(case when r.reaprender = true then 1 else 0 end) = 1
               and max(case when r.correta = true then 1 else 0 end) = 0
            order by max(r.passo)
            """)
    List<Pendencia> fila(@Param("sessao") Long sessao, @Param("aluno") Long aluno);

    List<RespostaAluno> findByAlunoId(Long alunoId);

    @Query("""
            select coalesce(sum(cast(r.pontosIndividuais as Long)), 0L) from RespostaAluno r
            where r.aluno.id = :aluno and r.pontosIndividuais is not null
            """)
    long pontosDoAluno(@Param("aluno") Long aluno);
}
