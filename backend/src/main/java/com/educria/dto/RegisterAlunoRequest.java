package com.educria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/** Dados aceitos no cadastro público. O perfil é definido pelo serviço como aluno. */
@Data
public class RegisterAlunoRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100)
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    @ToString.Exclude
    private String senha;

    @Size(max = 30)
    private String matricula;

    private Long turmaId; // opcional no cadastro
}
