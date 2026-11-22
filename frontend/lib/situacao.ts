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
