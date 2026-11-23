"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { toast } from "sonner";
import { Loader2, MessageSquare } from "lucide-react";
import { api } from "@/lib/api";
import { ErroApi, mensagemDeErro } from "@/lib/erros";
import { Recado } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

function quando(iso: string): string {
  return new Date(iso).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

/** Envio e histórico de recados do professor para um aluno da turma. */
export function RecadosParaAluno({
  turmaId,
  alunoId,
  nomeAluno,
}: {
  turmaId: string;
  alunoId: string;
  nomeAluno: string;
}) {
  const queryClient = useQueryClient();
  const chave = ["professor", "turma", turmaId, "aluno", alunoId, "recados"];
  const [texto, setTexto] = useState("");

  const recados = useQuery({
    queryKey: chave,
    queryFn: async () => {
      const { data } = await api.get<Recado[]>(
        `/api/professor/turmas/${turmaId}/alunos/${alunoId}/recados`
      );
      return data;
    },
  });

  const enviar = useMutation({
    mutationFn: async (conteudo: string) => {
      const { data } = await api.post<Recado>(
        `/api/professor/turmas/${turmaId}/alunos/${alunoId}/recados`,
        { texto: conteudo }
      );
      return data;
    },
    onSuccess: () => {
      toast.success(`Recado enviado para ${nomeAluno}.`);
      setTexto("");
      queryClient.invalidateQueries({ queryKey: chave });
    },
    onError: (error: AxiosError<ErroApi>) =>
      toast.error(mensagemDeErro(error, "Não foi possível enviar o recado. Tente novamente.")),
  });

  function submeter(evento: React.FormEvent) {
    evento.preventDefault();
    if (!texto.trim()) {
      toast.error("Escreva o recado.");
      return;
    }
    enviar.mutate(texto.trim());
  }

  return (
    <section className="space-y-3" aria-labelledby="titulo-recados">
      <h2 id="titulo-recados" className="flex items-center gap-2 text-xl font-bold text-slate-900">
        <MessageSquare className="h-5 w-5 text-indigo-600" />
        Recados para {nomeAluno}
      </h2>

      <Card className="shadow-sm border-slate-200/80">
        <CardHeader className="pb-3">
          <CardTitle className="text-base">Novo recado</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <form onSubmit={submeter} className="space-y-3">
            <Label htmlFor="recado">Recado</Label>
            <textarea
              id="recado"
              value={texto}
              maxLength={500}
              rows={3}
              placeholder="Ex.: Vi que você subiu dois níveis em frações. Passa na minha mesa na quarta."
              onChange={(e) => setTexto(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus-visible:outline focus-visible:outline-2 focus-visible:outline-indigo-600"
            />
            <div className="flex items-center justify-between gap-3">
              <span className="text-xs text-slate-500">{texto.length}/500</span>
              <Button type="submit" disabled={enviar.isPending}>
                {enviar.isPending ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Enviando…
                  </>
                ) : (
                  "Enviar recado"
                )}
              </Button>
            </div>
          </form>

          {recados.data && recados.data.length > 0 && (
            <ul className="space-y-3 border-t border-slate-100 pt-4">
              {recados.data.map((r) => (
                <li key={r.id} className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm">
                  <div className="mb-1 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500">
                    <span>
                      {r.de ?? "Professor removido"} · {quando(r.criadoEm)}
                    </span>
                    <Badge variant={r.lido ? "success" : "outline"}>{r.lido ? "Lido" : "Não lido"}</Badge>
                  </div>
                  <p className="text-slate-800">{r.texto}</p>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </section>
  );
}
