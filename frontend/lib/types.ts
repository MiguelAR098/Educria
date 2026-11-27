import type { RelatorioEvolucao } from "./evolucao";

export type UserRole = "ALUNO" | "PROFESSOR" | "ADMIN";

export interface User {
  id?: string | number;
  nome: string;
  email: string;
  role: UserRole;
  matricula?: string;
}

export interface AuthResponse {
  token: string;
  id?: number;
  user?: User;
  nome?: string;
  email?: string;
  perfil?: UserRole;
  role?: UserRole;
  matricula?: string;
}

export interface LoginPayload {
  email: string;
  senha: string;
}

export interface RegisterPayload {
  nome: string;
  email: string;
  senha: string;
  matricula?: string;
  turmaId?: number;
}

export interface DesempenhoAluno {
  nivelAtual: number; // 1 a 10
  errosConsecutivos?: number;
  nivelMaximo?: boolean;
  totalAcertos: number;
  totalErros: number;
}

export interface PontosTurma {
  turmaId: number | null;
  nomeTurma: string;
  pontosTotais: number;
}

export interface CriarTurmaPayload {
  nome: string;
  codigo: string;
  anoLetivo: number;
}

export interface LancarPontosPayload {
  turmaId: string | number;
  pontos: number;
}

export interface TurmaDoProfessor {
  id: number;
  nome: string;
  codigo: string;
  anoLetivo: number;
  totalAlunos: number;
}

export interface PerfilProfessor {
  id: number;
  nome: string;
  email: string;
  materia: string;
  turmas: TurmaDoProfessor[];
}

export type SituacaoAluno = "SEM_DADOS" | "EM_RISCO" | "ATENCAO" | "NO_CAMINHO";

export interface AlunoDaTurma {
  id: number;
  nome: string;
  matricula?: string;
  nivelAtual: number;
  errosConsecutivos: number;
  acertos: number;
  erros: number;
  taxaAcerto: number | null;
  situacao: SituacaoAluno;
  ultimaAtividade: string | null;
}

export interface ResumoDaTurma {
  totalAlunos: number;
  nivelMedio: number | null;
  emRisco: number;
  atencao: number;
  noCaminho: number;
  semDados: number;
}

export interface TurmaComAlunos {
  id: number;
  nome: string;
  codigo: string;
  anoLetivo: number;
  resumo: ResumoDaTurma;
  alunos: AlunoDaTurma[];
}

export interface AssuntoNaMatriz {
  id: number;
  nome: string;
  nivelAtual: number;
  errosConsecutivos: number;
  acertos: number;
  erros: number;
  taxaAcerto: number | null;
  situacao: SituacaoAluno;
}

export interface DetalheDoAluno {
  aluno: AlunoDaTurma;
  turmaId: number;
  turmaNome: string;
  materia: string;
  assuntos: AssuntoNaMatriz[];
  evolucao: RelatorioEvolucao;
}

export type TipoAtividadeColetiva = "ROBOTICA" | "LEITURA" | "PROJETO" | "DESAFIO" | "OUTRO";

export interface AtividadeColetiva {
  id: number;
  tipo: TipoAtividadeColetiva;
  titulo: string;
  pontos: number;
  resumo: string | null;
  lancadaPor: string | null;
  criadoEm: string;
}

export interface HistoricoDeAtividades {
  pontosTotais: number;
  atividades: AtividadeColetiva[];
}

export interface LancarAtividadePayload {
  tipo: TipoAtividadeColetiva;
  titulo: string;
  pontos: number;
  resumo?: string;
}

export interface Recado {
  id: number;
  texto: string;
  de: string | null;
  criadoEm: string;
  lido: boolean;
}

export interface RecadosDoAluno {
  naoLidos: number;
  recados: Recado[];
}

export interface AssuntoSimples {
  id: number;
  nome: string;
}

export interface QuestaoDoProfessor {
  id: number;
  assuntoId: number;
  assunto: string;
  dificuldade: number | null;
  enunciado: string;
  alternativaA: string;
  alternativaB: string;
  alternativaC: string;
  alternativaD: string;
  respostaCorreta: string;
  explicacao: string | null;
  ativa: boolean;
  autor: string | null;
  totalRespostas: number;
  taxaAcerto: number | null;
}

export interface BancoDeQuestoes {
  materia: string;
  assuntos: AssuntoSimples[];
  questoes: QuestaoDoProfessor[];
}

export interface CoberturaBancoQuestoes {
  assuntos: (AssuntoSimples & {
    contagens: number[];
    total: number;
  })[];
  total: number;
  combinacoesVazias: number;
}

export interface QuestaoPayload {
  assuntoId: number;
  dificuldade: number;
  enunciado: string;
  alternativaA: string;
  alternativaB: string;
  alternativaC: string;
  alternativaD: string;
  respostaCorreta: string;
  explicacao?: string;
}

export interface ResultadoImportacao {
  importadas: number;
  repetidas: number;
  recusadas: number;
  erros: string[];
}

export interface CelulaDoMapa {
  nivel: number;
  praticou: boolean;
}

export interface LinhaDoMapa {
  alunoId: number;
  nome: string;
  celulas: CelulaDoMapa[];
}

export interface AssuntoDaTurma {
  assuntoId: number;
  nome: string;
  nivelMedio: number | null;
  noNivelBaixo: number;
  alunosQuePraticaram: number;
}

export interface MapaDaTurma {
  turmaId: number;
  turmaNome: string;
  materia: string;
  assuntos: AssuntoSimples[];
  alunos: LinhaDoMapa[];
  medias: AssuntoDaTurma[];
  maisFrageis: AssuntoDaTurma[];
}

export interface AlertaDaTurma {
  titulo: string;
  texto: string;
}

export interface PontoDaTendencia {
  dia: string;
  nivelMedio: number | null;
}

export interface IndicadoresDoAluno {
  alunoId: number;
  evolucao30d: number | null;
  revisoesVencidas: number;
  erraMaisEm: string[];
}

export interface PanoramaDaTurma {
  turmaId: number;
  alertas: AlertaDaTurma[];
  tendencia: PontoDaTendencia[];
  alunos: IndicadoresDoAluno[];
}

