import { AxiosError } from "axios";

export type ErroApi = { message?: string; errors?: Record<string, string> };

/** Primeira mensagem de validação, ou a mensagem geral da API, ou o texto padrão. */
export function mensagemDeErro(error: AxiosError<ErroApi>, padrao: string): string {
  const dados = error.response?.data;
  const primeiroCampo = dados?.errors ? Object.values(dados.errors)[0] : undefined;
  return primeiroCampo || dados?.message || padrao;
}
