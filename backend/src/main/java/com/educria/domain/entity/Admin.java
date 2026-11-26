package com.educria.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Subtipo de {@link Usuario} cuja existência é registrada pela chave compartilhada em {@code admins}. */
@Entity
@Table(name = "admins")
@PrimaryKeyJoinColumn(name = "usuario_id")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Admin extends Usuario {
    // Campos específicos de admin podem ser adicionados depois
}
