// A etapa 15 já gravava a escolha nesta chave. Reutilizá-la preserva a preferência.
export const chaveTamanho = "educria.atividade.tamanhoTexto";
export const tamanhos = ["Normal", "Grande", "Muito grande"] as const;
export type Tamanho = typeof tamanhos[number];

export function tamanhoValido(valor: string | null | undefined): Tamanho {
  return tamanhos.find((opcao) => opcao === valor) ?? "Normal";
}

// Executado no head antes da primeira pintura. Apenas valores da lista chegam ao HTML.
export const iniciarTamanho = `try {
  var tamanho = localStorage.getItem(${JSON.stringify(chaveTamanho)});
  if (${JSON.stringify(tamanhos)}.includes(tamanho)) document.documentElement.dataset.tamanhoTexto = tamanho;
} catch {}`;
