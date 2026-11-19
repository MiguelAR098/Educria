package com.educria.repository;

import com.educria.domain.entity.RespostaAluno;
import com.educria.dto.FeedbackResposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RespostaAlunoRepository extends JpaRepository<RespostaAluno, Long> {
    @Query("""
            select new com.educria.dto.FeedbackResposta(r.respostaDada, r.correta, r.questao.explicacao)
            from RespostaAluno r where r.sessao.id = :sessao and r.questao.id = :questao
            """)
    Optional<FeedbackResposta> feedback(@Param("sessao") Long sessao, @Param("questao") Long questao);

    List<RespostaAluno> findByAlunoId(Long alunoId);
}
