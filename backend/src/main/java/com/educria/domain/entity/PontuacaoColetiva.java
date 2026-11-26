package com.educria.domain.entity;

import com.educria.exception.ApiException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Guarda o saldo compartilhado pelos integrantes da turma, separado do desempenho individual.
 * A chave estrangeira única permite um único saldo por turma.
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

    /** Soma valores positivos e rejeita lançamentos que excederiam o limite do inteiro armazenado. */
    public void adicionarPontos(int pontos) {
        if (pontos > 0) {
            if (pontosTotais > Integer.MAX_VALUE - pontos) {
                throw new ApiException("O limite de pontos coletivos da turma seria excedido", HttpStatus.CONFLICT);
            }
            this.pontosTotais += pontos;
        }
    }

    /** Desconta um valor positivo sem deixar o saldo abaixo de zero. */
    public void removerPontos(int pontos) {
        if (pontos > 0) {
            this.pontosTotais = Math.max(0, this.pontosTotais - pontos);
        }
    }
}
