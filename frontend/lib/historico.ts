import type { ResumoSessao } from "./estudo";

export interface Indicadores {
  id: number;
  assunto: string;
  situacao: ResumoSessao["situacao"];
  iniciadaEm: string;
  apresentadas: number;
  acertos: number;
  erros: number;
  nivelAntes: number | null;
  nivelDepois: number | null;
}

export interface Historico {
  sessoes: Indicadores[];
  proximoCursor: number | null;
}

export interface Resultado {
  sessao: Indicadores;
  anterior: Indicadores | null;
  questoes: { numero: number; enunciado: string; alternativa: string | null; correta: boolean | null }[];
  revisoes: { questaoId: number; enunciado: string; proximaRevisao: string }[];
}

export const situacoes = { ATIVA: "Em andamento", INTERROMPIDA: "Interrompida", ENCERRADA: "Encerrada" };
const formato = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short", timeStyle: "short", timeZone: "America/Sao_Paulo",
});
export function inicio(sessao: Indicadores) {
  return formato.format(new Date(sessao.iniciadaEm));
}
