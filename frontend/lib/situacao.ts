import { SituacaoAluno } from "./types";

export const SITUACAO: Record<
  SituacaoAluno,
  { rotulo: string; variante: "outline" | "destructive" | "warning" | "success"; ordem: number }
> = {
  EM_RISCO: { rotulo: "Em risco", variante: "destructive", ordem: 0 },
  ATENCAO: { rotulo: "Atenção", variante: "warning", ordem: 1 },
  NO_CAMINHO: { rotulo: "No caminho", variante: "success", ordem: 2 },
  SEM_DADOS: { rotulo: "Sem dados", variante: "outline", ordem: 3 },
};

/** Cor da célula do mapa por nível (1 a 10). O número sempre aparece: a cor não é a única informação. */
export function classeDoNivel(nivel: number): string {
  if (nivel <= 2) return "bg-red-100 text-red-900";
  if (nivel <= 4) return "bg-amber-100 text-amber-900";
  if (nivel <= 6) return "bg-lime-100 text-lime-900";
  if (nivel <= 8) return "bg-emerald-100 text-emerald-900";
  return "bg-emerald-200 text-emerald-950";
}

