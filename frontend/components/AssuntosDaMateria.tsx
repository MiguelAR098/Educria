"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import { api } from "@/lib/api";
import { ErroApi, mensagemDeErro } from "@/lib/erros";
import { AssuntoSimples } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

/** Lista os assuntos da matéria do professor e permite cadastrar novos. */
export function AssuntosDaMateria({
  materia,
  assuntos,
  aoCriar,
}: {
  materia: string;
  assuntos: AssuntoSimples[];
  aoCriar: () => void;
}) {
  const [nome, setNome] = useState("");

  const criar = useMutation({
    mutationFn: async (novo: string) =>
      (await api.post<AssuntoSimples>("/api/professor/assuntos", { nome: novo })).data,
    onSuccess: (assunto) => {
      toast.success(`Assunto "${assunto.nome}" cadastrado.`);
      setNome("");
      aoCriar();
    },
    onError: (error: AxiosError<ErroApi>) =>
      toast.error(mensagemDeErro(error, "Não foi possível cadastrar o assunto. Tente novamente.")),
  });

  function submeter(evento: React.FormEvent) {
    evento.preventDefault();
    if (!nome.trim()) {
      toast.error("Escreva o nome do assunto.");
      return;
    }
    criar.mutate(nome.trim());
  }

  return (
    <Card className={`shadow-sm ${assuntos.length === 0 ? "border-amber-200 bg-amber-50/40" : "border-slate-200/80"}`}>
      <CardHeader className="pb-3">
        <CardTitle className="text-base">Assuntos de {materia}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 text-sm">
        {assuntos.length === 0 ? (
          <p className="text-slate-800">
            {materia} ainda não tem assuntos cadastrados. Cadastre o primeiro abaixo para poder criar questões,
            importar JSON e acompanhar o mapa da turma.
          </p>
        ) : (
          <ul className="flex flex-wrap gap-2" aria-label="Assuntos cadastrados">
            {assuntos.map((a) => (
              <li key={a.id}>
                <Badge variant="secondary" className="first-letter:uppercase">{a.nome}</Badge>
              </li>
            ))}
          </ul>
        )}

        <form onSubmit={submeter} className="flex flex-wrap items-end gap-3">
          <div className="min-w-0 flex-[1_1_14rem] space-y-2">
            <Label htmlFor="novo-assunto">Novo assunto</Label>
            <Input
              id="novo-assunto"
              value={nome}
              maxLength={100}
              placeholder="Ex.: interpretação de texto"
              onChange={(e) => setNome(e.target.value)}
            />
          </div>
          <Button type="submit" variant="outline" disabled={criar.isPending}>
            {criar.isPending ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Salvando…
              </>
            ) : (
              "Adicionar assunto"
            )}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
