package com.educria.domain.entity;

import com.educria.domain.NivelAdaptativo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Diagnóstico global legado de um estudante. Serve de apoio à exibição privada quando ainda não
 * existe prática por assunto. O fluxo de sessões atual mantém o diagnóstico em {@link DesempenhoPorAssunto}.
 * A pontuação coletiva tem persistência própria em {@link PontuacaoColetiva}.
 */
@Entity
@Table(name = "desempenho_adaptativo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesempenhoAdaptativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id", nullable = false, unique = true)
    private Aluno aluno;

    @Column(name = "nivel_atual", nullable = false)
    @Builder.Default
    private int nivelAtual = NivelAdaptativo.MINIMO;

    @Column(name = "erros_consecutivos", nullable = false)
    @Builder.Default
    private int errosConsecutivos = 0;

    @Column(name = "total_acertos")
    @Builder.Default
    private int totalAcertos = 0;

    @Column(name = "total_erros")
    @Builder.Default
    private int totalErros = 0;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ===================== REGRAS DE NEGÓCIO ENCAPSULADAS =====================

    /** Aplica a regra compartilhada de {@link NivelAdaptativo} e acumula o resultado neste diagnóstico. */
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

    public boolean isNivelMaximo() {
        return nivelAtual >= NivelAdaptativo.MAXIMO;
    }

    public boolean isNivelMinimo() {
        return nivelAtual <= NivelAdaptativo.MINIMO;
    }
}
