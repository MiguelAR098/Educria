import { useQuery } from "@tanstack/react-query";
import { api } from "./api";
import { useChaveAluno } from "./estudo";

export interface Revisao {
  questaoId: number;
  enunciado: string;
  assunto: string;
  proximaRevisao: string;
  intervaloDias: number;
}

interface Agenda {
  hoje: string;
  proximaVirada: string;
  pendentes: Revisao[];
  futuras: Revisao[];
}

export function dataBrasileira(data: string) {
  return data.split("-").reverse().join("/");
}

export function useAgenda() {
  const aluno = useChaveAluno();
  return useQuery({
    queryKey: ["revisoes", aluno],
    queryFn: async ({ signal }) => (await api.get<Agenda>("/api/aluno/revisoes", { signal })).data,
    staleTime: 0,
    refetchInterval: ({ state }) => {
      if (!state.data) return false;
      const restante = Date.parse(state.data.proximaVirada) - Date.now();
      return restante > 0 ? restante + 100 : 60_000;
    },
  });
}
