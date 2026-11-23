"use client";

import { useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { toast } from "sonner";
import { ArrowLeft, BookOpen, Plus } from "lucide-react";
import { api } from "@/lib/api";
import { ErroApi, mensagemDeErro } from "@/lib/erros";
import { BancoDeQuestoes, QuestaoDoProfessor, QuestaoPayload } from "@/lib/types";
import { AuthGuard } from "@/components/AuthGuard";
import { FormularioQuestao } from "@/components/FormularioQuestao";
import { Navbar } from "@/components/Navbar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const LETRAS = ["A", "B", "C", "D"] as const;

function textoDaAlternativa(q: QuestaoDoProfessor, letra: (typeof LETRAS)[number]): string {
  return { A: q.alternativaA, B: q.alternativaB, C: q.alternativaC, D: q.alternativaD }[letra];
}

export default function BancoDeQuestoesPage() {
  const queryClient = useQueryClient();
  const [filtro, setFiltro] = useState<string>("");
  const [criando, setCriando] = useState(false);
  const [editandoId, setEditandoId] = useState<number | null>(null);

  const banco = useQuery({
    queryKey: ["professor", "questoes", filtro],
    queryFn: async () => {
      const { data } = await api.get<BancoDeQuestoes>("/api/professor/questoes", {
        params: filtro ? { assuntoId: filtro } : undefined,
      });
      return data;
    },
  });

  const atualizarLista = () => queryClient.invalidateQueries({ queryKey: ["professor", "questoes"] });
  const aoFalhar = (error: AxiosError<ErroApi>) =>
    toast.error(mensagemDeErro(error, "Não foi possível salvar. Tente novamente."));

  const criar = useMutation({
    mutationFn: async (payload: QuestaoPayload) => (await api.post("/api/professor/questoes", payload)).data,
    onSuccess: () => {
      toast.success("Questão cadastrada. Ela já pode ser sorteada para os alunos.");
      setCriando(false);
      atualizarLista();
    },
    onError: aoFalhar,
  });

  const editar = useMutation({
    mutationFn: async ({ id, payload }: { id: number; payload: QuestaoPayload }) =>
      (await api.put(`/api/professor/questoes/${id}`, payload)).data,
    onSuccess: () => {
      toast.success("Questão atualizada.");
      setEditandoId(null);
      atualizarLista();
    },
    onError: aoFalhar,
  });

  const alternarAtiva = useMutation({
    mutationFn: async ({ id, ativa }: { id: number; ativa: boolean }) =>
      (await api.patch(`/api/professor/questoes/${id}/ativa`, { ativa })).data,
    onSuccess: (_dados, { ativa }) => {
      toast.success(ativa ? "Questão reativada." : "Questão fora de circulação.");
      atualizarLista();
    },
    onError: aoFalhar,
  });

  const assuntos = banco.data?.assuntos ?? [];

  return (
    <AuthGuard allowedRole="PROFESSOR">
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />

        <main className="flex-1 mx-auto w-full max-w-5xl px-4 sm:px-6 py-8 space-y-6">
          <Link href="/professor" className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-700">
            <ArrowLeft className="h-4 w-4" /> Minhas turmas
          </Link>

          <div className="flex flex-wrap items-end justify-between gap-4">
            <div className="space-y-1">
              <Badge variant="secondary" className="gap-1">
                <BookOpen className="h-3.5 w-3.5" />
                {banco.data?.materia ?? "Matéria"}
              </Badge>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
                Banco de questões
              </h1>
              <p className="max-w-xl text-sm text-slate-500">
                Questões ativas entram no sorteio adaptativo dos alunos. Questões não são apagadas: ao
                desativar, o histórico dos alunos é preservado.
              </p>
            </div>
            {!criando && banco.data && (
              <Button onClick={() => { setCriando(true); setEditandoId(null); }}>
                <Plus className="mr-2 h-4 w-4" /> Nova questão
              </Button>
            )}
          </div>

          {banco.isLoading && <Skeleton className="h-64 w-full" />}
          {banco.isError && (
            <p className="text-sm text-red-600">Não foi possível carregar o banco de questões. Tente novamente.</p>
          )}

          {banco.data && (
            <>
              {criando && (
                <Card className="shadow-md border-indigo-200">
                  <CardHeader>
                    <CardTitle className="text-base">Nova questão</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <FormularioQuestao
                      assuntos={assuntos}
                      assuntoPadrao={filtro ? Number(filtro) : undefined}
                      enviando={criar.isPending}
                      rotuloBotao="Cadastrar questão"
                      onSalvar={(payload) => criar.mutate(payload)}
                      onCancelar={() => setCriando(false)}
                    />
                  </CardContent>
                </Card>
              )}

              <div className="flex flex-wrap items-center gap-3">
                <label htmlFor="filtro-assunto" className="text-sm font-medium text-slate-700">
                  Assunto
                </label>
                <select
                  id="filtro-assunto"
                  value={filtro}
                  onChange={(e) => setFiltro(e.target.value)}
                  className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900"
                >
                  <option value="">Todos</option>
                  {assuntos.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.nome}
                    </option>
                  ))}
                </select>
                <span className="text-sm text-slate-500">{banco.data.questoes.length} questões</span>
              </div>

              {banco.data.questoes.length === 0 && (
                <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-700">
                  Nenhuma questão neste filtro ainda.
                </p>
              )}

              <ul className="space-y-4">
                {banco.data.questoes.map((q) => (
                  <li key={q.id}>
                    {editandoId === q.id ? (
                      <Card className="shadow-md border-indigo-200">
                        <CardHeader>
                          <CardTitle className="text-base">Editar questão</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-3">
                          {q.totalRespostas > 0 && (
                            <p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-900">
                              Esta questão já foi respondida {q.totalRespostas} vez(es). Mudar o gabarito
                              ou o sentido dela pode distorcer o histórico; prefira criar outra.
                            </p>
                          )}
                          <FormularioQuestao
                            assuntos={assuntos}
                            inicial={q}
                            enviando={editar.isPending}
                            rotuloBotao="Salvar alterações"
                            onSalvar={(payload) => editar.mutate({ id: q.id, payload })}
                            onCancelar={() => setEditandoId(null)}
                          />
                        </CardContent>
                      </Card>
                    ) : (
                      <Card className={`shadow-sm border-slate-200/80 ${q.ativa ? "" : "opacity-70"}`}>
                        <CardHeader className="pb-3">
                          <div className="flex flex-wrap items-center gap-2">
                            <Badge variant="secondary" className="first-letter:uppercase">{q.assunto}</Badge>
                            {q.dificuldade !== null && <Badge variant="default">Dificuldade {q.dificuldade}/10</Badge>}
                            {!q.ativa && <Badge variant="outline">Fora de circulação</Badge>}
                          </div>
                          <CardTitle className="pt-2 text-base font-semibold leading-snug">{q.enunciado}</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-3 text-sm">
                          <ul className="space-y-1.5">
                            {LETRAS.map((letra) => (
                              <li
                                key={letra}
                                className={`flex items-center gap-2 rounded-lg px-3 py-1.5 ${
                                  q.respostaCorreta === letra ? "bg-emerald-50 text-emerald-900" : "bg-slate-50 text-slate-700"
                                }`}
                              >
                                <span className="font-bold">{letra}</span>
                                <span className="flex-1">{textoDaAlternativa(q, letra)}</span>
                                {q.respostaCorreta === letra && <Badge variant="success">Correta</Badge>}
                              </li>
                            ))}
                          </ul>
                          {q.explicacao && <p className="text-slate-600">Explicação: {q.explicacao}</p>}
                          <div className="flex flex-wrap items-center justify-between gap-3">
                            <p className="text-xs text-slate-500">
                              {q.totalRespostas === 0
                                ? "Ainda não respondida"
                                : `Respondida ${q.totalRespostas} vez(es) · ${q.taxaAcerto}% de acerto`}
                              {q.autor && ` · cadastrada por ${q.autor}`}
                            </p>
                            <div className="flex gap-2">
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => { setEditandoId(q.id); setCriando(false); }}
                              >
                                Editar
                              </Button>
                              <Button
                                variant="outline"
                                size="sm"
                                disabled={alternarAtiva.isPending}
                                onClick={() => alternarAtiva.mutate({ id: q.id, ativa: !q.ativa })}
                              >
                                {q.ativa ? "Desativar" : "Reativar"}
                              </Button>
                            </div>
                          </div>
                        </CardContent>
                      </Card>
                    )}
                  </li>
                ))}
              </ul>
            </>
          )}
        </main>
      </div>
    </AuthGuard>
  );
}
