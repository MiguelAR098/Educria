package com.educria.dto;

import com.educria.domain.enums.Perfil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Contrato entregue no cadastro e no login. Contém os dados da própria conta necessários à interface.
 * {@code @ToString.Exclude} mantém o JWT fora da representação textual gerada pelo Lombok.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    @ToString.Exclude
    private String token;
    private Long id;
    private String nome;
    private String email;
    private Perfil perfil;

    public static class AuthResponseBuilder {
        @Override
        public String toString() {
            return "AuthResponseBuilder";
        }
    }
}
