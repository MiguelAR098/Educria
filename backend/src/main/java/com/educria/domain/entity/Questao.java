package com.educria.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Item persistido do banco de questões. A atividade é obrigatória e o assunto pode faltar em
 * registros antigos. O estudo exibe {@link com.educria.dto.QuestaoParaAluno}, que omite o gabarito
 * e a explicação. {@code @JsonIgnore} também restringe a serialização direta desses campos.
 */
@Entity
@Table(name = "questoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Questao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_id", nullable = false)
    private Atividade atividade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assunto_id")
    private Assunto assunto;

    @Column
    private Integer dificuldade;

    @JsonIgnore
    @Column(length = org.hibernate.Length.LONG32)
    private String explicacao;

    @Column(nullable = false, length = org.hibernate.Length.LONG32)
    private String enunciado;

    @Column(name = "alternativa_a", length = 500)
    private String alternativaA;

    @Column(name = "alternativa_b", length = 500)
    private String alternativaB;

    @Column(name = "alternativa_c", length = 500)
    private String alternativaC;

    @Column(name = "alternativa_d", length = 500)
    private String alternativaD;

    @JsonIgnore
    @Column(name = "resposta_correta", nullable = false, length = 1)
    private String respostaCorreta; // "A", "B", "C" ou "D"

    @Column(name = "ordem")
    private Integer ordem;

    /** Questões desativadas deixam de ser sorteadas, mas mantêm o histórico dos alunos. */
    @Column(nullable = false)
    @Builder.Default
    private boolean ativa = true;

    /** Professor que cadastrou a questão; nulo nas questões da carga inicial. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Professor autor;

    /** Resolve a letra para o texto correspondente, retornando {@code null} para uma letra desconhecida ou sem texto. */
    public String textoAlternativa(String letra) {
        return switch (letra) {
            case "A" -> alternativaA;
            case "B" -> alternativaB;
            case "C" -> alternativaC;
            case "D" -> alternativaD;
            case null, default -> null;
        };
    }

    public boolean isRespostaCorreta(String respostaAluno) {
        if (respostaAluno == null) return false;
        return respostaCorreta.equalsIgnoreCase(respostaAluno.trim());
    }
}
