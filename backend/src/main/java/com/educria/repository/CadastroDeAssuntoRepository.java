package com.educria.repository;

import com.educria.domain.entity.Assunto;
import org.springframework.data.jpa.repository.JpaRepository;

/** Gravação de assuntos. A leitura do catálogo fica em AssuntoRepository. */
public interface CadastroDeAssuntoRepository extends JpaRepository<Assunto, Long> {}
