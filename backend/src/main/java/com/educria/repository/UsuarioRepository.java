package com.educria.repository;

import com.educria.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Consultas da identidade comum aos perfis. O Spring Data implementa a busca derivada pelo campo e-mail. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    /**
     * Verifica a existência da conta no SQL Server. {@code UPDLOCK} reserva o acesso para atualização e
     * {@code HOLDLOCK} mantém a proteção da faixa da chave, mesmo sem linha existente, até o fim do cadastro.
     */
    @Query(value = """
            select cast(case when exists (
                select 1 from dbo.usuarios with (UPDLOCK, HOLDLOCK) where email = :email
            ) then 1 else 0 end as bit)
            """, nativeQuery = true)
    boolean existeEmailParaCadastro(@Param("email") String email);
}
