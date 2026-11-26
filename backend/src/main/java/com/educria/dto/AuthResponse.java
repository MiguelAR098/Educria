package com.educria.dto;

import com.educria.domain.enums.Perfil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
