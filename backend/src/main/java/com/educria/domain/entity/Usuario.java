package com.educria.domain.entity;

import com.educria.domain.enums.Perfil;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Classe abstrata que reúne a identidade e os dados comuns a aluno, professor e administrador.
 * Cada conta é um objeto de uma dessas subclasses. Com a estratégia {@code JOINED}, o JPA carrega
 * a linha de {@code usuarios} junto da tabela do subtipo pela chave compartilhada.
 * Uma consulta pelo tipo {@code Usuario}, como a de login, pode retornar qualquer um desses subtipos.
 */
@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Usuario {

    // @Id mapeia a chave primária. IDENTITY delega a geração ao SQL Server no INSERT.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false)
    private String senha; // O valor persistido é o hash BCrypt.

    // Persiste o nome do perfil em vez da posição do enum, que pode mudar com a ordem das constantes.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    // O Hibernate preenche os horários de criação e atualização ao persistir a entidade.
    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
