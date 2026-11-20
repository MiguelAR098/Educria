import { useQuery } from "@tanstack/react-query";
import { api } from "./api";
import { useChaveAluno } from "./estudo";

interface IndicadoresEvolucao {
  sessoesIniciadas: number;
  tentativas: number;
  acertos: number;
  erros: number;
  questoesUnicas: number;
  taxaAcertoPrimeiraTentativa: number | null;
}

export interface DiaEvolucao {
  data: string;
  tentativas: number;
  acertos: number;
  erros: number;
}

export interface AssuntoEvolucao extends IndicadoresEvolucao {
  assuntoId: number;
  nome: string;
  nivelInicialObservado: number | null;
  nivelFinalObservado: number | null;
}

export interface RelatorioEvolucao {
  dataInicial: string;
  dataFinal: string;
  resumo: IndicadoresEvolucao & { tentativasReaprendizagem: number };
  evolucaoDiaria: DiaEvolucao[];
  assuntos: AssuntoEvolucao[];
}

export function useEvolucao(assuntoId: number | null = null, enabled = true) {
  const aluno = useChaveAluno();
  return useQuery({
    queryKey: ["evolucao-aprendizagem", aluno, assuntoId],
    queryFn: async ({ signal }) => (await api.get<RelatorioEvolucao>("/api/aluno/evolucao", {
      params: { assuntoId }, signal,
    })).data,
    enabled,
    staleTime: 0,
    refetchOnWindowFocus: true,
  });
}
