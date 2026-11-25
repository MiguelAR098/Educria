"use client";

import { useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { toast } from "sonner";
import { ArrowLeft, BookOpen, FileJson, Plus } from "lucide-react";
import { api } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import { ErroApi, mensagemDeErro } from "@/lib/erros";
import { BancoDeQuestoes, CoberturaBancoQuestoes, QuestaoDoProfessor, QuestaoPayload } from "@/lib/types";
import { AuthGuard } from "@/components/AuthGuard";
import { AssuntosDaMateria } from "@/components/AssuntosDaMateria";
import { CoberturaDoBanco } from "@/components/CoberturaDoBanco";
import { EstadoConsulta } from "@/components/EstadoConsulta";
import { FormularioQuestao } from "@/components/FormularioQuestao";
import { ImportarExportarQuestoes } from "@/components/ImportarExportarQuestoes";
import { Navbar } from "@/components/Navbar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const LETRAS = ["A", "B", "C", "D"] as const;
const dificuldades = Array.from({ length: 10 }, (_, indice) => indice + 1);
const campoFiltro = "min-h-11 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus-visible:outline-indigo-700";

function textoDaAlternativa(q: QuestaoDoProfessor, letra: (typeof LETRAS)[number]): string {
  return { A: q.alternativaA, B: q.alternativaB, C: q.alternativaC, D: q.alternativaD }[letra];
}

export default function BancoDeQuestoesPage() {
  const queryClient = useQueryClient();
  const professor = useAuthStore(({ user, token, _hasHydrated }) =>
    _hasHydrated && token && user?.role === "PROFESSOR" ? user.email : null);
  const [assuntoId, setAssuntoId] = useState<number | null>(null);
  const [dificuldade, setDificuldade] = useState<number | null>(null);
  const [criando, setCriando] = useState(false);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [abrirJson, setAbrirJson] = useState(false);

  const chaveBanco = ["professor", "questoes", professor] as const;
  const banco = useQuery({
    queryKey: [...chaveBanco, "lista", assuntoId],
    enabled: professor !== null,
    queryFn: async ({ signal }) => {
      const { data } = await api.get<BancoDeQuestoes>("/api/professor/questoes", {
        params: assuntoId === null ? undefined : { assuntoId }, signal,
      });
      return data;
    },
  });
  const cobertura = useQuery({
    queryKey: [...chaveBanco, "cobertura"],
    enabled: professor !== null,
    queryFn: async ({ signal }) =>
      (await api.get<CoberturaBancoQuestoes>("/api/professor/questoes/cobertura", { signal })).data,
  });

  const atualizarBanco = () => queryClient.invalidateQueries({ queryKey: chaveBanco });
  const aoFalhar = (error: AxiosError<ErroApi>) =>
    toast.error(mensagemDeErro(error, "Não foi possível salvar. Tente novamente."));

  const criar = useMutation({
    mutationFn: async (payload: QuestaoPayload) => (await api.post("/api/professor/questoes", payload)).data,
    onSuccess: () => {
      toast.success("Questão cadastrada. Ela já pode ser sorteada para os alunos.");
      setCriando(false);
      atualizarBanco();
    },
    onError: aoFalhar,
  });

  const editar = useMutation({
    mutationFn: async ({ id, payload }: { id: number; payload: QuestaoPayload }) =>
      (await api.put(`/api/professor/questoes/${id}`, payload)).data,
    onSuccess: () => {
      toast.success("Questão atualizada.");
      setEditandoId(null);
      atualizarBanco();
    },
    onError: aoFalhar,
  });

  const alternarAtiva = useMutation({
    mutationFn: async ({ id, ativa }: { id: number; ativa: boolean }) =>
      (await api.patch(`/api/professor/questoes/${id}/ativa`, { ativa })).data,
    onSuccess: (_dados, { ativa }) => {
      toast.success(ativa ? "Questão reativada." : "Questão fora de circulação.");
      atualizarBanco();
    },
    onError: aoFalhar,
  });

  const assuntos = banco.data?.assuntos ?? cobertura.data?.assuntos ?? [];
  const semAssuntos = assuntos.length === 0;
  const filtrando = assuntoId !== null || dificuldade !== null;
  const questoes = banco.data?.questoes.filter((q) => dificuldade === null || q.dificuldade === dificuldade) ?? [];

  function selecionarCelula(assunto: number, nivel: number) {
    setAssuntoId(assunto);
    setDificuldade(nivel);
  }

  function limparFiltros() {
    setAssuntoId(null);
    setDificuldade(null);
  }

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
            {banco.data && (
              <div className="flex flex-wrap gap-2">
                <Button variant="outline" disabled={semAssuntos} onClick={() => setAbrirJson((aberto) => !aberto)}>
                  <FileJson className="mr-2 h-4 w-4" /> Importar / exportar JSON
                </Button>
                {!criando && (
                  <Button disabled={semAssuntos} onClick={() => { setCriando(true); setEditandoId(null); }}>
                    <Plus className="mr-2 h-4 w-4" /> Nova questão
                  </Button>
                )}
              </div>
            )}
          </div>

          <section aria-labelledby="titulo-cobertura-banco" className="min-w-0">
            <Card>
              <CardHeader className="px-4 sm:px-6">
                <h2 id="titulo-cobertura-banco" className="text-xl font-bold text-slate-900">Cobertura do banco</h2>
              </CardHeader>
              <CardContent className="px-4 sm:px-6">
                <EstadoConsulta consulta={cobertura} recurso="a cobertura do banco" />
                {cobertura.isSuccess && (
                  <CoberturaDoBanco
                    cobertura={cobertura.data}
                    assuntoId={assuntoId}
                    dificuldade={dificuldade}
                    onSelecionar={selecionarCelula}
                  />
                )}
              </CardContent>
            </Card>
          </section>

          {banco.data && (
            <>
              <AssuntosDaMateria materia={banco.data.materia} assuntos={assuntos} aoCriar={atualizarBanco} />

              {abrirJson && (
                <Card className="shadow-md border-indigo-200">
                  <CardHeader>
                    <CardTitle className="text-base">Importar e exportar em JSON</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <ImportarExportarQuestoes
                      materia={banco.data.materia}
                      assuntos={assuntos}
                      aoImportar={atualizarBanco}
                    />
                  </CardContent>
                </Card>
              )}

              {criando && (
                <Card className="shadow-md border-indigo-200">
                  <CardHeader>
                    <CardTitle className="text-base">Nova questão</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <FormularioQuestao
                      assuntos={assuntos}
                      assuntoPadrao={assuntoId ?? undefined}
                      dificuldadePadrao={dificuldade ?? undefined}
                      enviando={criar.isPending}
                      rotuloBotao="Cadastrar questão"
                      onSalvar={(payload) => criar.mutate(payload)}
                      onCancelar={() => setCriando(false)}
                    />
                  </CardContent>
                </Card>
              )}
            </>
          )}

          {(banco.data || filtrando) && (
            <div className="flex flex-wrap items-end gap-3" role="group" aria-label="Filtros da listagem">
              <div className="w-full min-w-0 space-y-1 sm:w-52">
                <label htmlFor="filtro-assunto" className="text-sm font-medium text-slate-700">Assunto</label>
                <select id="filtro-assunto" value={assuntoId ?? ""} className={campoFiltro}
                  onChange={(e) => setAssuntoId(e.target.value ? Number(e.target.value) : null)}>
                  <option value="">Todos</option>
                  {assuntos.map((a) => <option key={a.id} value={a.id}>{a.nome}</option>)}
                </select>
              </div>
              <div className="w-full min-w-0 space-y-1 sm:w-40">
                <label htmlFor="filtro-dificuldade" className="text-sm font-medium text-slate-700">Dificuldade</label>
                <select id="filtro-dificuldade" value={dificuldade ?? ""} className={campoFiltro}
                  onChange={(e) => setDificuldade(e.target.value ? Number(e.target.value) : null)}>
                  <option value="">Todas</option>
                  {dificuldades.map((nivel) => <option key={nivel} value={nivel}>{nivel}</option>)}
                </select>
              </div>
              <Button variant="outline" className="min-h-11" disabled={!filtrando} onClick={limparFiltros}>
                Limpar filtros
              </Button>
              <p aria-live="polite" className="py-2 text-sm text-slate-600">
                {banco.data && `${questoes.length} ${questoes.length === 1 ? "questão" : "questões"}`}
              </p>
            </div>
          )}

          {banco.isLoading && <Skeleton className="h-64 w-full" />}
          {banco.isError && <EstadoConsulta consulta={banco} recurso="o banco de questões" />}
          {banco.data && (
            <>
              {questoes.length === 0 && (
                <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-700">
                  {filtrando ? "Nenhuma questão corresponde aos filtros." : "Nenhuma questão cadastrada."}
                </p>
              )}

              <ul className="space-y-4">
                {questoes.map((q) => (
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
