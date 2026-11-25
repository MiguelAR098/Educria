package com.educria.repository;

import com.educria.domain.entity.Materia;
import com.educria.dto.AssuntoDoAluno;
import com.educria.dto.MateriaResumo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MateriaRepository extends Repository<Materia, Long> {

    boolean existsById(Long id);

    @Query("""
            select new com.educria.dto.MateriaResumo(m.id, m.nome, count(s.id), coalesce((
                select sum(cast(r.pontosIndividuais as Long)) from RespostaAluno r join r.sessao historica
                where r.aluno.id = :alunoId and r.pontosIndividuais is not null and historica.assunto.materia = m
            ), 0L))
            from Materia m left join Assunto s on s.materia = m
            group by m.id, m.nome
            order by m.id
            """)
    List<MateriaResumo> listarCatalogo(@Param("alunoId") Long alunoId);

    @Query("""
            select new com.educria.dto.AssuntoDoAluno(s.id, s.nome, coalesce(d.nivelAtual, 1), coalesce((
                select sum(cast(r.pontosIndividuais as Long)) from RespostaAluno r join r.sessao historica
                where r.aluno.id = :alunoId and r.pontosIndividuais is not null and historica.assunto = s
            ), 0L))
            from Assunto s left join DesempenhoPorAssunto d
                on d.assunto = s and d.aluno.id = :alunoId
            where s.materia.id = :materiaId
            order by s.id
            """)
    List<AssuntoDoAluno> listarAssuntos(@Param("materiaId") Long materiaId, @Param("alunoId") Long alunoId);
}
