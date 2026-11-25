const numero = new Intl.NumberFormat("pt-BR");

export function formatarPontos(pontos: number | null) {
  return pontos == null ? "Pontuação não registrada" : `${numero.format(pontos)} ${pontos === 1 ? "ponto" : "pontos"}`;
}
