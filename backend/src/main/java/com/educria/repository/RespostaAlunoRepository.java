package com.educria.repository;

import com.educria.domain.entity.RespostaAluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RespostaAlunoRepository extends JpaRepository<RespostaAluno, Long> {
    Optional<RespostaAluno> findBySessaoIdAndPasso(Long sessao, int passo);
    Optional<RespostaAluno> findFirstBySessaoIdAndQuestaoIdOrderByPasso(Long sessao, Long questao);

    interface Pendencia {
        Long getQuestao();
        int getUltimoPasso();
    }

    @Query(value = """
            select questao_id as questao, max(passo) as "ultimoPasso"
            from respostas_aluno where sessao_id = :sessao
            group by questao_id having bool_or(reaprender) and not bool_or(correta)
            order by max(passo)
            """, nativeQuery = true)
    List<Pendencia> fila(@Param("sessao") Long sessao);

    List<RespostaAluno> findByAlunoId(Long alunoId);
}
