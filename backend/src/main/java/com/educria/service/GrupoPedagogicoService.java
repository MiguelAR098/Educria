package com.educria.service;

import com.educria.domain.entity.GrupoPedagogico;
import com.educria.domain.entity.ParticipacaoGrupo;
import com.educria.domain.entity.Professor;
import com.educria.domain.entity.Turma;
import com.educria.domain.enums.TipoGrupoPedagogico;
import com.educria.dto.GrupoPedagogicoDto;
import com.educria.dto.GrupoPedagogicoDto.Atualizacao;
import com.educria.dto.GrupoPedagogicoDto.Criacao;
import com.educria.dto.GrupoPedagogicoDto.Detalhe;
import com.educria.dto.GrupoPedagogicoDto.Inclusao;
import com.educria.dto.GrupoPedagogicoDto.Participante;
import com.educria.dto.GrupoPedagogicoDto.Resumo;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.GrupoPedagogicoRepository;
import com.educria.repository.ParticipacaoGrupoRepository;
import com.educria.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Administra grupos e projetos de um professor ativo dentro de suas turmas.
 * Escritas carregam o grupo com bloqueio e mantêm a autorização e a alteração na mesma transação.
 */
@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class GrupoPedagogicoService {
    private final GrupoPedagogicoRepository grupos;
    private final ParticipacaoGrupoRepository participacoes;
    private final ProfessorRepository professores;
    private final AlunoRepository alunos;
    private final Clock relogio;

    public record Contexto(Long id, String nome, TipoGrupoPedagogico tipo, Long professorId,
                           Long materiaId, String materiaNome, List<Long> turmasIds) {}

    /** Cria um grupo nas turmas solicitadas depois de conferir cada vínculo com o professor autenticado. */
    public Resumo criar(String email, Criacao pedido) {
        Professor professor = professorAtivo(email);
        var ids = new HashSet<>(pedido.turmasIds());
        Set<Turma> turmas = professor.getTurmas().stream()
                .filter(turma -> ids.contains(turma.getId())).collect(Collectors.toSet());
        if (turmas.size() != ids.size()) throw new ApiException("Turma não autorizada", HttpStatus.FORBIDDEN);

        var grupo = new GrupoPedagogico(professor, pedido.nome().strip(), descricao(pedido.descricao()),
                pedido.tipo(), turmas, agora());
        return resumo(grupos.save(grupo));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<Resumo> listar(String email, TipoGrupoPedagogico tipo, Boolean arquivado) {
        Professor professor = professorAtivo(email);
        Set<Long> turmasAutorizadas = idsDasTurmas(professor);
        return grupos.listarDoProfessor(professor.getId(), tipo, arquivado).stream()
                .filter(grupo -> abrangeContexto(turmasAutorizadas, grupo))
                .map(GrupoPedagogicoService::resumo).toList();
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Detalhe detalhar(String email, Long id) {
        return detalhe(grupoAutorizado(professorAtivo(email), id, false));
    }

    public Resumo atualizar(String email, Long id, Atualizacao pedido) {
        GrupoPedagogico grupo = grupoAutorizado(professorAtivo(email), id, true);
        exigirAtivo(grupo);
        grupo.atualizar(pedido.nome().strip(), descricao(pedido.descricao()));
        return resumo(grupo);
    }

    /**
     * Confere se todos os alunos solicitados estão ativos nas turmas do grupo antes de gravar o lote.
     * Participações vigentes são reaproveitadas. Novos vínculos registram a turma de origem.
     */
    public Detalhe incluir(String email, Long id, Inclusao pedido) {
        GrupoPedagogico grupo = grupoAutorizado(professorAtivo(email), id, true);
        exigirAtivo(grupo);
        Map<Long, Turma> turmas = grupo.getTurmas().stream()
                .collect(Collectors.toMap(Turma::getId, Function.identity()));
        Set<Long> ids = Set.copyOf(pedido.alunosIds());
        var elegiveis = participacoes.alunosElegiveis(ids, turmas.keySet());
        if (elegiveis.size() != ids.size()) throw alunoNaoElegivel();

        Set<Long> existentes = new HashSet<>(participacoes.idsJaParticipantes(id, ids));
        Instant entrada = agora();
        for (var aluno : elegiveis) {
            if (!existentes.contains(aluno.getAlunoId())) {
                participacoes.save(new ParticipacaoGrupo(grupo, alunos.getReferenceById(aluno.getAlunoId()),
                        turmas.get(aluno.getTurmaId()), entrada));
            }
        }
        return detalhe(grupo);
    }

    /** Registra a saída de uma participação vigente, preservando o período em que o aluno integrou o grupo. */
    public void remover(String email, Long id, Long alunoId) {
        GrupoPedagogico grupo = grupoAutorizado(professorAtivo(email), id, true);
        exigirAtivo(grupo);
        if (participacoes.alunosElegiveis(List.of(alunoId), idsDasTurmas(grupo)).isEmpty()) {
            throw alunoNaoElegivel();
        }
        participacoes.findByGrupoIdAndAlunoIdAndSaidaEmIsNull(id, alunoId)
                .ifPresent(participacao -> participacao.sair(agora()));
    }

    /** Arquiva sem apagar o grupo nem seu histórico. Uma nova solicitação preserva a data já registrada. */
    public Resumo arquivar(String email, Long id) {
        GrupoPedagogico grupo = grupoAutorizado(professorAtivo(email), id, true);
        grupo.arquivar(agora());
        return resumo(grupo);
    }

    /** Entrega ao serviço de indicadores apenas o contexto do grupo validado para o professor autenticado. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Contexto contextoAutorizado(String email, Long id) {
        Professor professor = professorAtivo(email);
        GrupoPedagogico grupo = grupoAutorizado(professor, id, false);
        return new Contexto(id, grupo.getNome(), grupo.getTipo(), professor.getId(), professor.getMateria().getId(),
                professor.getMateria().getNome(), idsDasTurmas(grupo));
    }

    private Professor professorAtivo(String email) {
        return professores.buscarComTurmas(email).filter(Professor::isAtivo)
                .orElseThrow(() -> new ApiException("Professor não encontrado", HttpStatus.NOT_FOUND));
    }

    // Exige autoria e vínculo atual com todas as turmas. Ausência e falta de acesso recebem a mesma resposta.
    private GrupoPedagogico grupoAutorizado(Professor professor, Long id, boolean bloquear) {
        var encontrado = bloquear ? grupos.bloquearDoProfessor(id, professor.getId())
                : grupos.findByIdAndProfessorId(id, professor.getId());
        return encontrado.filter(grupo -> abrangeContexto(idsDasTurmas(professor), grupo))
                .orElseThrow(() -> new ApiException("Grupo não encontrado", HttpStatus.NOT_FOUND));
    }

    private static boolean abrangeContexto(Set<Long> turmasAutorizadas, GrupoPedagogico grupo) {
        return !grupo.getTurmas().isEmpty() && turmasAutorizadas.containsAll(idsDasTurmas(grupo));
    }

    private static Set<Long> idsDasTurmas(Professor professor) {
        return professor.getTurmas().stream().map(Turma::getId).collect(Collectors.toSet());
    }

    private static List<Long> idsDasTurmas(GrupoPedagogico grupo) {
        return grupo.getTurmas().stream().map(Turma::getId).sorted().toList();
    }

    private static void exigirAtivo(GrupoPedagogico grupo) {
        if (grupo.getArquivadoEm() != null) throw new ApiException("Grupo arquivado", HttpStatus.CONFLICT);
    }

    private static ApiException alunoNaoElegivel() {
        return new ApiException("Aluno não encontrado no contexto do grupo", HttpStatus.NOT_FOUND);
    }

    private Detalhe detalhe(GrupoPedagogico grupo) {
        var visiveis = participacoes.participantesVisiveis(grupo.getId(), idsDasTurmas(grupo)).stream()
                .map(p -> new Participante(p.getAlunoId(), p.getNome(), p.getTurmaOrigemId(), p.getTurmaAtualId(),
                        p.getEntradaEm())).toList();
        return new Detalhe(resumo(grupo), visiveis);
    }

    private static Resumo resumo(GrupoPedagogico grupo) {
        var turmas = grupo.getTurmas().stream().sorted(Comparator.comparing(Turma::getId))
                .map(turma -> new GrupoPedagogicoDto.Turma(turma.getId(), turma.getNome())).toList();
        return new Resumo(grupo.getId(), grupo.getNome(), grupo.getDescricao(), grupo.getTipo(),
                grupo.getCriadoEm(), grupo.getArquivadoEm(), turmas);
    }

    private static String descricao(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private Instant agora() {
        return relogio.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
