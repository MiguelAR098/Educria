package com.educria.repository;

import com.educria.domain.entity.Assunto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Consulta o catálogo de assuntos e oferece a recomendação inicial de estudo. */
public interface AssuntoRepository extends Repository<Assunto, Long> {
    Optional<Assunto> findById(Long id);

    @Query("select s from Assunto s where s.materia.id = :materiaId order by s.nome")
    List<Assunto> daMateria(@Param("materiaId") Long materiaId);

    /**
     * Prioriza o menor nível entre assuntos com pelo menos uma questão elegível e sem revisão futura.
     * Assuntos sem diagnóstico começam no nível 1 para a recomendação. O ID desempata a ordem.
     * O chamador solicita uma página com um item quando precisa somente da primeira recomendação.
     */
    @Query("""
            select s from Assunto s left join DesempenhoPorAssunto d on d.assunto = s and d.aluno.id = :alunoId
            where exists (select q.id from Questao q where q.assunto = s and
            """ + QuestaoRepository.QUESTAO_ELEGIVEL + """
                          and not exists (
                              select r.id from RepeticaoEspacada r where r.aluno.id = :alunoId
                              and r.questao = q and r.proximaRevisao > :hoje))
            order by coalesce(d.nivelAtual, 1), s.id
            """)
    List<Assunto> recomendar(@Param("alunoId") Long alunoId, @Param("hoje") LocalDate hoje, Pageable pagina);
}
