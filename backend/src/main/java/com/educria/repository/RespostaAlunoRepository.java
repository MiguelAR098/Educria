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

    @Query(value = """
            select questao_id as questao, max(passo) as "ultimoPasso"
            from respostas_aluno where sessao_id = :sessao and aluno_id = :aluno
            group by questao_id having bool_or(reaprender) and not bool_or(correta)
            order by max(passo)
            """, nativeQuery = true)
    List<Pendencia> fila(@Param("sessao") Long sessao, @Param("aluno") Long aluno);

    List<RespostaAluno> findByAlunoId(Long alunoId);
}
