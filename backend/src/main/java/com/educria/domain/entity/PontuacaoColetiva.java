package com.educria.domain.entity;

import com.educria.exception.ApiException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Pontuação pública por turma (economia coletiva).
 * Isolada completamente do DesempenhoAdaptativo.
 * Não existe FK nem serviço que una os dois mundos.
 */
@Entity
@Table(name = "pontuacao_coletiva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PontuacaoColetiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turma_id", nullable = false, unique = true)
    private Turma turma;

    @Column(name = "pontos_totais", nullable = false)
    @Builder.Default
    private int pontosTotais = 0;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public void adicionarPontos(int pontos) {
        if (pontos > 0) {
            if (pontosTotais > Integer.MAX_VALUE - pontos) {
                throw new ApiException("O limite de pontos coletivos da turma seria excedido", HttpStatus.CONFLICT);
            }
            this.pontosTotais += pontos;
        }
    }

    public void removerPontos(int pontos) {
        if (pontos > 0) {
            this.pontosTotais = Math.max(0, this.pontosTotais - pontos);
        }
    }
}
