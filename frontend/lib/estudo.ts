import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { api } from "./api";
import { useAuthStore } from "./auth-store";

export const rotaSessoes = "/api/aluno/sessoes";
export const limiteQuestoes = 10;

export interface ResumoSessao {
  id: number;
  assuntoId: number;
  situacao: "ATIVA" | "INTERROMPIDA" | "ENCERRADA";
  questoesApresentadas: number;
}

export interface QuestaoAtual {
  id: number;
  dificuldade: number;
  enunciado: string;
  alternativaA: string | null;
  alternativaB: string | null;
  alternativaC: string | null;
  alternativaD: string | null;
}

export type Alternativa = "A" | "B" | "C" | "D";

export interface FeedbackResposta {
  reaprender?: boolean;
  nivelAnterior?: number;
  nivelAtual?: number;
  alternativa: Alternativa;
  correta: boolean;
  explicacao: string | null;
  tipoOrientacao?: "EXPLICACAO" | "DICA_INICIAL" | "DICA_APRIMORADA" | "RESOLUCAO";
  errosDaQuestao?: number;
  alternativaCorreta?: Alternativa;
  textoAlternativaCorreta?: string;
}

export interface EstadoSessao {
  passo?: number;
  repetindo?: boolean;
  pendencias?: number;
  feedback?: FeedbackResposta;
  sessao: ResumoSessao;
  questaoAtual: QuestaoAtual | null;
}

interface Assunto {
  id: number;
  nome: string;
  nivelAtual: number;
}

export function useChaveAluno() {
  return useAuthStore((state) => state.user?.id ?? state.user?.email);
}

export function useAssuntosDeEstudo() {
  const aluno = useChaveAluno();
  return useQuery({
    queryKey: ["assuntos-de-estudo", aluno],
    queryFn: async () => {
      const { data } = await api.get<{ id: number; nome: string; totalAssuntos: number }[]>("/api/aluno/materias");
      const grupos = await Promise.all(data.filter((m) => m.totalAssuntos > 0).map(async (materia) => {
        const { data: assuntos } = await api.get<Assunto[]>(`/api/aluno/materias/${materia.id}/assuntos`);
        return assuntos.map((a) => ({ ...a, materia: materia.nome }));
      }));
      return grupos.flat();
    },
  });
}

export function semQuestoes(erro: unknown) {
  return axios.isAxiosError(erro) && erro.response?.status === 409 &&
    erro.response.data?.message === "Nenhuma questão elegível disponível";
}

export function mensagemErro(erro: unknown) {
  if (axios.isAxiosError(erro)) {
    if (erro.response?.status === 404) return "Esta sessão não está disponível para a sua conta.";
    if (erro.response?.status === 403) return "Sua conta não tem acesso a esta atividade.";
    if (erro.response?.status === 409) return "O estado da sessão mudou. Confira a posição atual.";
  }
  return "Não foi possível conectar à atividade. Tente novamente.";
}

export function useSessoes() {
  const aluno = useChaveAluno();
  return useQuery({
    queryKey: ["sessoes", aluno],
    staleTime: 0,
    queryFn: async () => (await api.get<ResumoSessao[]>(rotaSessoes)).data,
  });
}
