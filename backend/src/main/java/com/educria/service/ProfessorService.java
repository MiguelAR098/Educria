package com.educria.service;

import com.educria.domain.entity.Aluno;
import com.educria.domain.entity.DesempenhoAdaptativo;
import com.educria.domain.entity.DesempenhoPorAssunto;
import com.educria.domain.entity.Professor;
import com.educria.domain.entity.Turma;
import com.educria.domain.enums.SituacaoAluno;
import com.educria.dto.AlunoDaTurma;
import com.educria.dto.AssuntoNaMatriz;
import com.educria.dto.DetalheDoAluno;
import com.educria.dto.PerfilProfessor;
import com.educria.dto.ResumoDaTurma;
import com.educria.dto.TurmaComAlunos;
import com.educria.dto.TurmaDoProfessor;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.AssuntoRepository;
import com.educria.repository.DesempenhoPorAssuntoRepository;
import com.educria.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessorService {

    private final ProfessorRepository professorRepository;
    private final AlunoRepository alunoRepository;
    private final AssuntoRepository assuntoRepository;
    private final DesempenhoPorAssuntoRepository desempenhoPorAssuntoRepository;
    private final EvolucaoAprendizagemService evolucaoService;

    @Transactional(readOnly = true)
    public PerfilProfessor perfil(String email) {
        Professor professor = buscarProfessor(email);

        return new PerfilProfessor(
                professor.getId(),
                professor.getNome(),
                professor.getEmail(),
                professor.getMateria().getNome(),
                professor.getTurmas().stream()
                        .sorted(Comparator.comparing(Turma::getNome))
                        .map(t -> new TurmaDoProfessor(
                                t.getId(),
                                t.getNome(),
                                t.getCodigo(),
                                t.getAnoLetivo(),
                                alunoRepository.countByTurmaId(t.getId())))
                        .toList());
    }

    /** Alunos de uma turma com nível e situação. Só para professor que leciona na turma. */
    @Transactional(readOnly = true)
    public TurmaComAlunos turma(String email, Long turmaId) {
        Professor professor = buscarProfessor(email);

        Turma turma = professor.getTurmas().stream()
                .filter(t -> t.getId().equals(turmaId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Você não leciona nesta turma", HttpStatus.FORBIDDEN));

        List<AlunoDaTurma> alunos = alunoRepository.findByTurmaIdComDesempenho(turmaId).stream()
                .map(ProfessorService::paraAlunoDaTurma)
                .toList();

        return new TurmaComAlunos(turma.getId(), turma.getNome(), turma.getCodigo(), turma.getAnoLetivo(),
                resumir(alunos), alunos);
    }

    /**
     * Detalhe de um aluno da turma: assuntos da matéria do professor e evolução de 30 dias.
     * Sem @Transactional de propósito: cada consulta usa a própria transação e o relatório de
     * evolução mantém o isolamento REPEATABLE_READ definido em EvolucaoAprendizagemService.
     */
    public DetalheDoAluno detalheDoAluno(String email, Long turmaId, Long alunoId) {
        Professor professor = buscarProfessor(email);

        Turma turma = professor.getTurmas().stream()
                .filter(t -> t.getId().equals(turmaId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Você não leciona nesta turma", HttpStatus.FORBIDDEN));

        Aluno aluno = alunoRepository.findByIdAndTurmaIdComDesempenho(alunoId, turmaId)
                .orElseThrow(() -> new ApiException("Aluno não encontrado nesta turma", HttpStatus.NOT_FOUND));

        Map<Long, DesempenhoPorAssunto> praticados = desempenhoPorAssuntoRepository.doAluno(alunoId).stream()
                .collect(Collectors.toMap(d -> d.getAssunto().getId(), Function.identity()));

        List<AssuntoNaMatriz> assuntos = assuntoRepository.daMateria(professor.getMateria().getId()).stream()
                .map(assunto -> paraAssuntoNaMatriz(assunto.getId(), assunto.getNome(), praticados.get(assunto.getId())))
                .toList();

        return new DetalheDoAluno(
                paraAlunoDaTurma(aluno),
                turma.getId(),
                turma.getNome(),
                professor.getMateria().getNome(),
                assuntos,
                evolucaoService.obterDoAluno(alunoId, null));
    }

    private Professor buscarProfessor(String email) {
        return professorRepository.buscarComTurmas(email)
                .orElseThrow(() -> new ApiException("Professor não encontrado", HttpStatus.NOT_FOUND));
    }

    private static AlunoDaTurma paraAlunoDaTurma(Aluno aluno) {
        DesempenhoAdaptativo d = aluno.getDesempenhoAdaptativo();
        int acertos = d != null ? d.getTotalAcertos() : 0;
        int erros = d != null ? d.getTotalErros() : 0;
        int consecutivos = d != null ? d.getErrosConsecutivos() : 0;
        int respostas = acertos + erros;
        Double taxa = respostas > 0 ? Math.round(acertos * 1000.0 / respostas) / 10.0 : null;

        return new AlunoDaTurma(
                aluno.getId(),
                aluno.getNome(),
                aluno.getMatricula(),
                d != null ? d.getNivelAtual() : 1,
                consecutivos,
                acertos,
                erros,
                taxa,
                SituacaoAluno.calcular(respostas, taxa, consecutivos),
                respostas > 0 ? d.getAtualizadoEm() : null);
    }

    private static AssuntoNaMatriz paraAssuntoNaMatriz(Long id, String nome, DesempenhoPorAssunto d) {
        int acertos = d != null ? d.getTotalAcertos() : 0;
        int erros = d != null ? d.getTotalErros() : 0;
        int consecutivos = d != null ? d.getErrosConsecutivos() : 0;
        int respostas = acertos + erros;
        Double taxa = respostas > 0 ? Math.round(acertos * 1000.0 / respostas) / 10.0 : null;

        return new AssuntoNaMatriz(id, nome, d != null ? d.getNivelAtual() : 1, consecutivos, acertos, erros, taxa,
                SituacaoAluno.calcular(respostas, taxa, consecutivos));
    }

    private static ResumoDaTurma resumir(List<AlunoDaTurma> alunos) {
        Double nivelMedio = alunos.stream()
                .filter(a -> a.situacao() != SituacaoAluno.SEM_DADOS)
                .mapToInt(AlunoDaTurma::nivelAtual)
                .average()
                .stream()
                .mapToObj(media -> Math.round(media * 10.0) / 10.0)
                .findFirst()
                .orElse(null);

        return new ResumoDaTurma(
                alunos.size(),
                nivelMedio,
                contar(alunos, SituacaoAluno.EM_RISCO),
                contar(alunos, SituacaoAluno.ATENCAO),
                contar(alunos, SituacaoAluno.NO_CAMINHO),
                contar(alunos, SituacaoAluno.SEM_DADOS));
    }

    private static int contar(List<AlunoDaTurma> alunos, SituacaoAluno situacao) {
        Predicate<AlunoDaTurma> naSituacao = a -> a.situacao() == situacao;
        return (int) alunos.stream().filter(naSituacao).count();
    }
}
