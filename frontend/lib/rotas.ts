import { UserRole } from "./types";

/** Página inicial de cada perfil após o login. */
export function rotaInicial(role: UserRole): string {
  if (role === "ADMIN") return "/admin";
  if (role === "PROFESSOR") return "/professor";
  return "/aluno";
}
