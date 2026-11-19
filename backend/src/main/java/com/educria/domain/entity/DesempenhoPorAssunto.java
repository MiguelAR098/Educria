package com.educria.domain.entity;

import com.educria.domain.NivelAdaptativo;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "desempenho_por_assunto",
        uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "assunto_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DesempenhoPorAssunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false, updatable = false)
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assunto_id", nullable = false, updatable = false)
    private Assunto assunto;

    @Column(name = "nivel_atual", nullable = false)
    private int nivelAtual = NivelAdaptativo.MINIMO;

    @Column(name = "erros_consecutivos", nullable = false)
    private int errosConsecutivos;

    @Column(name = "total_acertos", nullable = false)
    private int totalAcertos;

    @Column(name = "total_erros", nullable = false)
    private int totalErros;

    @Version
    @Column(nullable = false)
    private long versao;

    public void registrarResposta(boolean acertou) {
        NivelAdaptativo proximo = new NivelAdaptativo(nivelAtual, errosConsecutivos).aposResposta(acertou);
        nivelAtual = proximo.nivel();
        errosConsecutivos = proximo.errosConsecutivos();
        if (acertou) {
            totalAcertos++;
        } else {
            totalErros++;
        }
    }
}
