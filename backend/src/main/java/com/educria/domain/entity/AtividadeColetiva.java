package com.educria.domain.entity;

import com.educria.domain.enums.TipoAtividadeColetiva;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Atividade feita pela turma junto, registrada por um professor.
 * Os pontos vão para a PontuacaoColetiva da turma; nunca para o desempenho individual.
 */
@Entity
@Table(name = "atividades_coletivas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtividadeColetiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turma_id", nullable = false)
    private Turma turma;

    /** Nulo se o professor foi removido depois de lançar a atividade. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private Professor professor;

    /** Guarda o nome da opção do enum para preservar o significado se a ordem das opções mudar. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAtividadeColetiva tipo;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false)
    private int pontos;

    @Column(length = 300)
    private String resumo;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
