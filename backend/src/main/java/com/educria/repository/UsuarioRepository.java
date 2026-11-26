package com.educria.repository;

import com.educria.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    // Protege a chave do e-mail, mesmo quando ela ainda não existe, até concluir o cadastro.
    @Query(value = """
            select cast(case when exists (
                select 1 from dbo.usuarios with (UPDLOCK, HOLDLOCK) where email = :email
            ) then 1 else 0 end as bit)
            """, nativeQuery = true)
    boolean existeEmailParaCadastro(@Param("email") String email);
}
