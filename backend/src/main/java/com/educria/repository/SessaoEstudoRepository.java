package com.educria.repository;

import com.educria.domain.entity.SessaoEstudo;
import com.educria.domain.entity.Questao;
import com.educria.domain.enums.SituacaoSessao;
import com.educria.dto.QuestaoParaAluno;
import com.educria.dto.ResumoSessao;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessaoEstudoRepository extends Repository<SessaoEstudo, Long> {
    @Query("""
            select q from SessaoEstudo s, Questao q where s.id = :sessaoId and q.id = :questaoId
            and q.id member of s.apresentadas
            """)
    Optional<Questao> questaoDaSessao(@Param("sessaoId") Long sessaoId, @Param("questaoId") Long questaoId);

    SessaoEstudo save(SessaoEstudo sessao);
    Optional<SessaoEstudo> findByAlunoIdAndSituacaoNot(Long alunoId, SituacaoSessao situacao);

    @Query("""
            select s from SessaoEstudo s left join fetch s.apresentadas
            where s.id = :id and s.aluno.email = :email
            """)
    Optional<SessaoEstudo> buscarDoAluno(@Param("id") Long id, @Param("email") String email);

    @Query("""
            select new com.educria.dto.ResumoSessao(s.id, s.assunto.id, s.situacao,
                   size(s.apresentadas), s.iniciadaEm, s.atualizadaEm)
            from SessaoEstudo s where s.aluno.email = :email order by s.id desc
            """)
    List<ResumoSessao> listarDoAluno(@Param("email") String email);

    @Query("""
            select new com.educria.dto.QuestaoParaAluno(q.id, q.assunto.id, q.dificuldade, q.enunciado,
                       q.alternativaA, q.alternativaB, q.alternativaC, q.alternativaD)
            from Questao q where q.id = :id
            """)
    QuestaoParaAluno questaoApresentada(@Param("id") Long id);
}
