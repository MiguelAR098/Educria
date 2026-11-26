package com.educria.domain.entity;

import com.educria.domain.enums.IntervaloRepeticao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Agenda de repetição por aluno e questão. O vínculo por atividade representa registros legados.
 * O banco exige um único tipo de alvo por registro e uma agenda por aluno para cada alvo.
 * No fluxo atual, o serviço agenda acertos e remove a revisão pendente após um erro.
 */
@Entity
@Table(name = "repeticao_espacada",
       uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "atividade_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepeticaoEspacada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_id")
    private Atividade atividade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questao_id")
    private Questao questao;

    @Enumerated(EnumType.STRING)
    @Column(name = "intervalo_atual", nullable = false, length = 10)
    @Builder.Default
    private IntervaloRepeticao intervaloAtual = IntervaloRepeticao.D1;

    @Column(name = "proxima_revisao", nullable = false)
    private LocalDate proximaRevisao;

    @Column(name = "ultima_revisao")
    private LocalDate ultimaRevisao;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ===================== REGRAS DE NEGÓCIO =====================

    /**
     * Agenda a partir da data da resposta. O primeiro acerto usa um dia. Acertos seguintes avançam
     * pelos intervalos de 3, 7, 16, 35 e 70 dias, mantendo 70 dias no teto.
     */
    public void registrarAcerto(LocalDate hoje) {
        agendar(ultimaRevisao != null ? intervaloAtual.proximo() : IntervaloRepeticao.D1, hoje);
    }

    public void registrarAcerto() {
        agendar(intervaloAtual.proximo(), LocalDate.now());
    }

    /** Reinicia em um dia para o fluxo legado por atividade. */
    public void registrarErro() {
        agendar(IntervaloRepeticao.D1, LocalDate.now());
    }

    private void agendar(IntervaloRepeticao intervalo, LocalDate hoje) {
        intervaloAtual = intervalo;
        ultimaRevisao = hoje;
        proximaRevisao = hoje.plusDays(intervalo.getDias());
    }

    public boolean isPendente() {
        return !LocalDate.now().isBefore(proximaRevisao);
    }

    public static RepeticaoEspacada criarNova(Aluno aluno, Atividade atividade) {
        return RepeticaoEspacada.builder()
                .aluno(aluno)
                .atividade(atividade)
                .intervaloAtual(IntervaloRepeticao.D1)
                .proximaRevisao(LocalDate.now().plusDays(1))
                .build();
    }
}
