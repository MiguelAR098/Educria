package com.educria.service;

import com.educria.domain.entity.Aluno;
import com.educria.domain.entity.DesempenhoAdaptativo;
import com.educria.domain.entity.Turma;
import com.educria.domain.entity.Usuario;
import com.educria.domain.enums.Perfil;
import com.educria.dto.AuthResponse;
import com.educria.dto.LoginRequest;
import com.educria.dto.RegisterAlunoRequest;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.TurmaRepository;
import com.educria.repository.UsuarioRepository;
import com.educria.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final AlunoRepository alunoRepository;
    private final TurmaRepository turmaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse registerAluno(RegisterAlunoRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("E-mail já cadastrado", HttpStatus.CONFLICT);
        }

        Turma turma = null;
        if (request.getTurmaId() != null) {
            turma = turmaRepository.findById(request.getTurmaId())
                    .orElseThrow(() -> new ApiException("Turma não encontrada", HttpStatus.NOT_FOUND));
        }

        Aluno aluno = Aluno.builder()
                .nome(request.getNome())
                .email(request.getEmail())
                .senha(passwordEncoder.encode(request.getSenha()))
                .perfil(Perfil.ALUNO)
                .ativo(true)
                .matricula(request.getMatricula())
                .turma(turma)
                .build();

        // Cria desempenho adaptativo inicial (nível 1)
        DesempenhoAdaptativo desempenho = DesempenhoAdaptativo.builder()
                .nivelAtual(1)
                .errosConsecutivos(0)
                .build();

        aluno.vincularDesempenho(desempenho);
        aluno = alunoRepository.save(aluno);

        String token = jwtService.generateToken(aluno);

        return AuthResponse.builder()
                .token(token)
                .id(aluno.getId())
                .nome(aluno.getNome())
                .email(aluno.getEmail())
                .perfil(aluno.getPerfil())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        String token = jwtService.generateToken(usuario);

        return AuthResponse.builder()
                .token(token)
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .perfil(usuario.getPerfil())
                .build();
    }
}
