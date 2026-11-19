package com.educria.domain.entity;

import com.educria.domain.enums.IntervaloRepeticao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Controle de repetição espaçada por aluno + atividade.
 * Progressão forçada: 1d → 3d → 7d → 16d → 35d → 70d no acerto.
 * No erro: volta para 1 dia.
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
    @JoinColumn(name = "atividade_id", nullable = false)
    private Atividade atividade;

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

    public void registrarAcerto() {
        this.intervaloAtual = this.intervaloAtual.proximo();
        this.ultimaRevisao = LocalDate.now();
        this.proximaRevisao = LocalDate.now().plusDays(this.intervaloAtual.getDias());
    }

    public void registrarErro() {
        this.intervaloAtual = IntervaloRepeticao.doPrimeiro(); // volta para 1 dia
        this.ultimaRevisao = LocalDate.now();
        this.proximaRevisao = LocalDate.now().plusDays(1);
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
