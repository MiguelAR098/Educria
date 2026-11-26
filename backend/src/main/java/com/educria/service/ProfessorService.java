package com.educria.service;

import com.educria.domain.NivelAdaptativo;
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
import com.educria.repository.PanoramaRepository;
import com.educria.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final PanoramaRepository panoramaRepository;
    private final EvolucaoAprendizagemService evolucaoService;

    private record LinhaDaTurma(AlunoDaTurma aluno, Double nivelMedio) {}

    @Transactional(readOnly = true)
    public PerfilProfessor perfil(String email) {
        List<ProfessorRepository.LinhaDoPerfil> linhas = professorRepository.linhasDoPerfil(email);
        if (linhas.isEmpty()) throw new ApiException("Professor não encontrado", HttpStatus.NOT_FOUND);
        var professor = linhas.getFirst();

        return new PerfilProfessor(
                professor.getProfessorId(),
                professor.getProfessorNome(),
                professor.getProfessorEmail(),
                professor.getMateriaNome(),
                linhas.stream()
                        .filter(linha -> linha.getTurmaId() != null)
                        .sorted(Comparator.comparing(ProfessorRepository.LinhaDoPerfil::getTurmaNome))
                        .map(t -> new TurmaDoProfessor(
                                t.getTurmaId(),
                                t.getTurmaNome(),
                                t.getTurmaCodigo(),
                                t.getTurmaAnoLetivo(),
                                t.getTotalAlunos()))
                        .toList());
    }

    /** Alunos de uma turma com nível e situação. Só para professor que leciona na turma. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public TurmaComAlunos turma(String email, Long turmaId) {
        Professor professor = buscarProfessor(email);

        Turma turma = professor.getTurmas().stream()
                .filter(t -> t.getId().equals(turmaId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Você não leciona nesta turma", HttpStatus.FORBIDDEN));

        long materiaId = professor.getMateria().getId();
        var desempenhos = desempenhoPorAssuntoRepository.daTurma(turmaId, materiaId).stream()
                .collect(Collectors.groupingBy(d -> d.getAluno().getId()));
        var atividades = panoramaRepository.ultimasAtividadesNaMateria(turmaId, materiaId, null).stream()
                .collect(Collectors.toMap(PanoramaRepository.AtividadeDoAluno::getAlunoId, PanoramaRepository.AtividadeDoAluno::data));
        var linhas = alunoRepository.cadastrosNaTurma(turmaId, null).stream()
                .map(aluno -> {
                    var resumo = ResumoDoDesempenho.dosAssuntos(desempenhos.getOrDefault(aluno.getId(), List.of()));
                    return new LinhaDaTurma(paraAlunoDaTurma(aluno, resumo, atividades.get(aluno.getId())), resumo.nivelMedio());
                })
                .toList();

        return new TurmaComAlunos(turma.getId(), turma.getNome(), turma.getCodigo(), turma.getAnoLetivo(),
                resumir(linhas), linhas.stream().map(LinhaDaTurma::aluno).toList());
    }

    /** Mantém vínculo e relatório na mesma leitura, restrita à matéria do professor. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DetalheDoAluno detalheDoAluno(String email, Long turmaId, Long alunoId) {
        Professor professor = buscarProfessor(email);

        Turma turma = professor.getTurmas().stream()
                .filter(t -> t.getId().equals(turmaId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Você não leciona nesta turma", HttpStatus.FORBIDDEN));

        var aluno = alunoRepository.cadastrosNaTurma(turmaId, alunoId).stream().findFirst()
                .orElseThrow(() -> new ApiException("Aluno não encontrado nesta turma", HttpStatus.NOT_FOUND));

        long materiaId = professor.getMateria().getId();
        Map<Long, DesempenhoPorAssunto> praticados = desempenhoPorAssuntoRepository.doAlunoNaMateria(alunoId, materiaId).stream()
                .collect(Collectors.toMap(d -> d.getAssunto().getId(), Function.identity()));

        List<AssuntoNaMatriz> assuntos = assuntoRepository.daMateria(materiaId).stream()
                .map(assunto -> paraAssuntoNaMatriz(assunto.getId(), assunto.getNome(), praticados.get(assunto.getId())))
                .toList();
        var resumo = ResumoDoDesempenho.dosAssuntos(praticados.values());
        var ultimaAtividade = panoramaRepository.ultimasAtividadesNaMateria(turmaId, materiaId, alunoId).stream()
                .map(PanoramaRepository.AtividadeDoAluno::data).findFirst().orElse(null);

        return new DetalheDoAluno(
                paraAlunoDaTurma(aluno, resumo, ultimaAtividade),
                turma.getId(),
                turma.getNome(),
                professor.getMateria().getNome(),
                assuntos,
                evolucaoService.obterPorMateria(alunoId, materiaId));
    }

    private Professor buscarProfessor(String email) {
        return professorRepository.buscarComTurmas(email)
                .orElseThrow(() -> new ApiException("Professor não encontrado", HttpStatus.NOT_FOUND));
    }

    private static AlunoDaTurma paraAlunoDaTurma(AlunoRepository.CadastroNaTurma aluno, ResumoDoDesempenho resumo,
                                                LocalDateTime ultimaAtividade) {
        return new AlunoDaTurma(
                aluno.getId(),
                aluno.getNome(),
                aluno.getMatricula(),
                resumo.nivelAtual(),
                resumo.errosConsecutivos(),
                resumo.acertos(),
                resumo.erros(),
                resumo.taxaAcerto(),
                resumo.situacao(),
                ultimaAtividade);
    }

    private static AssuntoNaMatriz paraAssuntoNaMatriz(Long id, String nome, DesempenhoPorAssunto d) {
        int acertos = d != null ? d.getTotalAcertos() : 0;
        int erros = d != null ? d.getTotalErros() : 0;
        int consecutivos = d != null ? d.getErrosConsecutivos() : 0;
        int respostas = acertos + erros;
        Double taxa = ResumoDoDesempenho.taxaAcerto(acertos, erros);

        return new AssuntoNaMatriz(id, nome, d != null ? d.getNivelAtual() : NivelAdaptativo.MINIMO, consecutivos, acertos, erros, taxa,
                SituacaoAluno.calcular(respostas, taxa, consecutivos));
    }

    private static ResumoDaTurma resumir(List<LinhaDaTurma> linhas) {
        Double nivelMedio = linhas.stream()
                .map(LinhaDaTurma::nivelMedio)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .stream()
                .mapToObj(media -> Math.round(media * 10.0) / 10.0)
                .findFirst()
                .orElse(null);
        var alunos = linhas.stream().map(LinhaDaTurma::aluno).toList();

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
