package com.educria.service;

import com.educria.domain.entity.Aluno;
import com.educria.dto.RelatorioEvolucao.Dia;
import com.educria.exception.ApiException;
import com.educria.repository.AlunoRepository;
import com.educria.repository.AssuntoRepository;
import com.educria.repository.EvolucaoAprendizagemRepository;
import com.educria.repository.EvolucaoAprendizagemRepository.Linha;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvolucaoAprendizagemServiceTest {
    private static final String EMAIL = "aluno@example.test";
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate INICIO = LocalDate.of(2026, 12, 2);
    private static final LocalDate FIM_EXCLUSIVO = LocalDate.of(2027, 1, 1);

    private final AlunoRepository alunos = mock(AlunoRepository.class);
    private final AssuntoRepository assuntos = mock(AssuntoRepository.class);
    private final EvolucaoAprendizagemRepository repositorio = mock(EvolucaoAprendizagemRepository.class);
    private final Clock relogio = Clock.fixed(Instant.parse("2027-01-01T02:30:00Z"), ZONA);
    private final EvolucaoAprendizagemService servico = new EvolucaoAprendizagemService(alunos, assuntos, repositorio, relogio);

    @BeforeEach
    void encontrarTitular() {
        when(alunos.findByEmail(EMAIL)).thenReturn(Optional.of(Aluno.builder().id(42L).build()));
    }

    @Test
    void preencheTrintaDiasLocaisComZeroQuandoNaoHaAtividade() {
        var geral = new Linha(true, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, null, null);
        when(repositorio.agregar(42L, null, INICIO, FIM_EXCLUSIVO, ZONA)).thenReturn(List.of(geral));

        var relatorio = servico.obter(EMAIL, null);

        assertEquals(INICIO, relatorio.dataInicial());
        assertEquals(FIM_EXCLUSIVO.minusDays(1), relatorio.dataFinal());
        assertEquals(INICIO.datesUntil(FIM_EXCLUSIVO).map(data -> new Dia(data, 0, 0, 0)).toList(),
                relatorio.evolucaoDiaria());
        assertEquals(0, relatorio.resumo().sessoesIniciadas());
        assertNull(relatorio.resumo().taxaAcertoPrimeiraTentativa());
        assertTrue(relatorio.assuntos().isEmpty());
    }

    @Test
    void usaSomentePrimeirasTentativasNaTaxaEPreservaNivelAusente() {
        var geral = new Linha(true, null, null, null, 2, 5, 2, 4, 1, 2, 3, 2, null, null);
        var assunto = new Linha(false, 20L, "Frações", null, 2, 5, 2, 4, 1, 2, 3, 2, null, 6);
        var dia = new Linha(false, null, null, INICIO, 0, 5, 2, 4, 1, 2, 3, 2, null, 6);
        when(repositorio.agregar(42L, null, INICIO, FIM_EXCLUSIVO, ZONA)).thenReturn(List.of(geral, assunto, dia));

        var relatorio = servico.obter(EMAIL, null);

        assertEquals(new BigDecimal("66.67"), relatorio.resumo().taxaAcertoPrimeiraTentativa());
        assertEquals(2, relatorio.resumo().tentativasReaprendizagem());
        assertEquals(new Dia(INICIO, 5, 4, 1), relatorio.evolucaoDiaria().getFirst());
        var detalhe = relatorio.assuntos().getFirst();
        assertEquals(new BigDecimal("66.67"), detalhe.taxaAcertoPrimeiraTentativa());
        assertNull(detalhe.nivelInicialObservado());
        assertEquals(6, detalhe.nivelFinalObservado());
    }

    @Test
    void assuntoInexistenteNaoAcionaAgregacao() {
        when(assuntos.findById(123L)).thenReturn(Optional.empty());

        var erro = assertThrows(ApiException.class, () -> servico.obter(EMAIL, 123L));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatus());
        verifyNoInteractions(repositorio);
    }
}
