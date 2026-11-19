export type UserRole = "ALUNO" | "ADMIN";

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
