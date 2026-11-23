import { TipoAtividadeColetiva } from "./types";

export const TIPOS_ATIVIDADE: { id: TipoAtividadeColetiva; nome: string; descricao: string }[] = [
  { id: "ROBOTICA", nome: "Robótica com ESP32", descricao: "Estação física com botões e display, a turma discute e responde em conjunto." },
  { id: "LEITURA", nome: "Leitura coletiva", descricao: "A turma lê um texto e debate ao final. Sem competição individual." },
  { id: "PROJETO", nome: "Projeto em grupo", descricao: "Produção coletiva de um artefato: cartaz, vídeo, jornal, maquete." },
  { id: "DESAFIO", nome: "Desafio em sala", descricao: "Rodada curta valendo pontos coletivos, sem lançar nota individual." },
  { id: "OUTRO", nome: "Outro tipo", descricao: "Registre uma atividade que não se encaixa nas anteriores." },
];

export const PONTOS_ATIVIDADE = [
  { valor: 20, rotulo: "20, participação" },
  { valor: 40, rotulo: "40, conclusão" },
  { valor: 60, rotulo: "60, conclusão com destaque coletivo" },
  { valor: 80, rotulo: "80, entrega além do combinado" },
];

export function nomeDoTipo(tipo: TipoAtividadeColetiva): string {
  return TIPOS_ATIVIDADE.find((t) => t.id === tipo)?.nome ?? tipo;
}
