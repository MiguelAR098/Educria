package com.educria.service;

import com.educria.domain.entity.Assunto;
import com.educria.domain.entity.Professor;
import com.educria.dto.AssuntoSimples;
import com.educria.exception.ApiException;
import com.educria.repository.AssuntoRepository;
import com.educria.repository.CadastroDeAssuntoRepository;
import com.educria.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * O professor cadastra assuntos só na própria matéria. Alunos já cadastrados não precisam de preparo:
 * o desempenho no assunto novo nasce na primeira resposta.
 */
@Service
@RequiredArgsConstructor
public class AssuntoDoProfessorService {

    private final ProfessorRepository professorRepository;
    private final AssuntoRepository assuntoRepository;
    private final CadastroDeAssuntoRepository cadastroRepository;

    /**
     * Usa o e-mail autenticado para localizar a matéria e devolve o identificador e o nome do novo assunto.
     * A busca por duplicatas compara nomes sem distinção de acentos, caixa e espaços excedentes.
     */
    @Transactional
    public AssuntoSimples criar(String email, String nome) {
        Professor professor = professorRepository.buscarComTurmas(email)
                .orElseThrow(() -> new ApiException("Professor não encontrado", HttpStatus.NOT_FOUND));

        String limpo = nome.trim().replaceAll("\\s+", " ");
        if (limpo.isEmpty()) {
            throw new ApiException("Escreva o nome do assunto", HttpStatus.BAD_REQUEST);
        }

        String chave = LeitorDeQuestoes.normalizar(limpo);
        boolean existe = assuntoRepository.daMateria(professor.getMateria().getId()).stream()
                .anyMatch(a -> LeitorDeQuestoes.normalizar(a.getNome()).equals(chave));
        if (existe) {
            throw new ApiException("Já existe um assunto com esse nome nesta matéria", HttpStatus.CONFLICT);
        }

        Assunto assunto = cadastroRepository.save(new Assunto(professor.getMateria(), limpo));
        return new AssuntoSimples(assunto.getId(), assunto.getNome());
    }
}
