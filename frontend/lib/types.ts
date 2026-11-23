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

