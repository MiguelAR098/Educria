package com.educria.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "respostas_aluno")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaAluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questao_id", nullable = false)
    private Questao questao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id")
    private SessaoEstudo sessao;

    @Column(name = "resposta_dada", length = 1)
    private String respostaDada;

    @Column(nullable = false)
    private boolean correta;

    @Column(name = "pontos_individuais", updatable = false)
    private Integer pontosIndividuais;

    @Column(name = "nivel_anterior", updatable = false)
    private Integer nivelAnterior;

    @Column(name = "nivel_atual", updatable = false)
    private Integer nivelAtual;

    private Integer passo;
    private boolean reaprender;

    // datetime2 conserva a hora local de estudo para as contagens por dia.
    @JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "respondido_em", nullable = false, updatable = false)
    private LocalDateTime respondidoEm;
}
