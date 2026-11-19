package com.educria.repository;

import com.educria.domain.entity.Assunto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssuntoRepository extends Repository<Assunto, Long> {
    Optional<Assunto> findById(Long id);

    @Query("""
            select s from Assunto s left join DesempenhoPorAssunto d on d.assunto = s and d.aluno.id = :alunoId
            where exists (select q.id from Questao q where q.assunto = s and q.atividade.ativa = true
                          and q.dificuldade is not null)
            order by coalesce(d.nivelAtual, 1), s.id
            """)
    List<Assunto> recomendar(@Param("alunoId") Long alunoId, Pageable pagina);
}
