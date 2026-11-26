package com.educria.security;

import com.educria.domain.entity.Usuario;
import com.educria.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Adapta a entidade de domínio ao contrato {@link UserDetailsService} do Spring Security.
 * O provedor de login e o filtro JWT dependem da interface, e o Spring injeta esta implementação.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        // hasRole consulta a autoridade ROLE_<perfil>. O hash fica disponível para a comparação interna do login.
        return new User(
                usuario.getEmail(),
                usuario.getSenha(),
                usuario.isAtivo(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()))
        );
    }
}
