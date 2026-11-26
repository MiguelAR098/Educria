package com.educria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

/** Corpo do login. O controller aciona as restrições de Bean Validation com {@code @Valid}. */
@Data
public class LoginRequest {

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @ToString.Exclude
    private String senha;
}
