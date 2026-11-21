"use client";

import Link from "next/link";
import { BarChart3, CalendarDays, CheckCheck, ChevronRight, Clock3, History, Play, RefreshCw } from "lucide-react";
import { useIsFetching, useQuery, useQueryClient } from "@tanstack/react-query";
import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { CatalogoMaterias } from "@/components/CatalogoMaterias";
import { LevelCard } from "@/components/LevelCard";
import { CollectivePointsCard } from "@/components/CollectivePointsCard";
import { EstadoConsulta } from "@/components/EstadoConsulta";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { api } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import { useChaveAluno } from "@/lib/estudo";
import { inicio, situacoes, useHistorico } from "@/lib/historico";
import { dataBrasileira, useAgenda } from "@/lib/revisoes";
import type { DesempenhoAluno, PontosTurma } from "@/lib/types";

function Painel() {
  const aluno = useChaveAluno();
  const nome = useAuthStore((s) => s.user?.nome.split(" ")[0]);
  const cache = useQueryClient();
  const atualizando = useIsFetching() > 0;
  const nivel = useQuery({
    queryKey: ["aluno-desempenho", aluno],
    queryFn: async ({ signal }) => (await api.get<DesempenhoAluno>("/api/aluno/desempenho", { signal })).data,
  });
  const turma = useQuery({
    queryKey: ["aluno-turma-pontos", aluno],
    queryFn: async ({ signal }) => (await api.get<PontosTurma>("/api/aluno/turma/pontos", { signal })).data,
  });
  const agenda = useAgenda();
  const historico = useHistorico();
  const recentes = historico.data?.pages[0].sessoes.slice(0, 3);
  return (
    <main className="mx-auto w-full max-w-5xl space-y-6 px-4 pb-12 pt-6 sm:px-6 sm:pt-9">
      <header className="rounded-[1.25rem] border border-indigo-100 bg-white px-5 py-5 shadow-sm sm:px-7">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="min-w-0">
            <h1 className="break-words text-3xl font-extrabold tracking-tight text-slate-900">Olá, {nome || "estudante"}!</h1>
            <p className="mt-1 text-sm text-slate-600">Pronto para a próxima atividade?</p>
          </div>
          <Button asChild size="lg" className="gap-2 rounded-xl bg-gradient-to-r from-indigo-700 to-violet-600 text-white shadow-md shadow-indigo-200 hover:brightness-110">
            <Link href="/aluno/atividade"><Play aria-hidden size={16} fill="currentColor" /> Fazer atividade</Link>
          </Button>
        </div>
        <nav aria-label="Atalhos do aluno" className="mt-4 flex flex-wrap items-center gap-x-5 gap-y-2 border-t border-slate-100 pt-3 text-sm font-semibold">
          <Link className="inline-flex items-center gap-2 rounded py-1.5 text-slate-700 hover:text-indigo-700" href="/aluno/revisoes"><CalendarDays aria-hidden size={16} /> Minhas revisões</Link>
          <Link className="inline-flex items-center gap-2 rounded py-1.5 text-slate-700 hover:text-indigo-700" href="/aluno/historico"><History aria-hidden size={16} /> Histórico de sessões</Link>
          <Link className="inline-flex items-center gap-2 rounded py-1.5 text-slate-700 hover:text-indigo-700" href="/aluno/evolucao"><BarChart3 aria-hidden size={16} /> Evolução da aprendizagem</Link>
          <button type="button" aria-label="Atualizar dados" title="Atualizar dados" disabled={atualizando} onClick={() => cache.refetchQueries({ type: "active" })}
            className="ml-auto grid h-9 w-9 place-items-center rounded-lg border border-slate-200 text-slate-600 hover:bg-indigo-50 disabled:opacity-60">
            <RefreshCw aria-hidden size={16} className={atualizando ? "motion-safe:animate-spin" : ""} />
          </button>
        </nav>
      </header>

      <div className="grid gap-4 md:grid-cols-[1.58fr_1fr]">
        <div className="min-w-0"><EstadoConsulta consulta={nivel} recurso="o nível global" />
          {nivel.data && <LevelCard desempenho={nivel.data} />}</div>
        <div className="min-w-0"><EstadoConsulta consulta={turma} recurso="os pontos da turma" />
          {turma.data && <CollectivePointsCard pontosTurma={turma.data} />}</div>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <Card className="overflow-hidden rounded-[1.25rem]">
          <CardHeader className="flex-row items-center gap-3 space-y-0 p-5 pb-3">
            <span className="grid h-10 w-10 place-items-center rounded-xl bg-emerald-100 text-emerald-800"><CalendarDays aria-hidden size={20} /></span>
            <CardTitle className="text-lg">Revisões pendentes</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 px-5 pb-5 text-sm">
            <EstadoConsulta consulta={agenda} recurso="as revisões" />
            {agenda.data && <>
              <p className="flex items-center gap-2 rounded-xl bg-emerald-50 px-3 py-3 font-semibold text-emerald-900">
                {agenda.data.pendentes.length === 0 && <CheckCheck aria-hidden size={17} />}
                {agenda.data.pendentes.length === 0 ? "Você está em dia com as revisões."
                : `Para revisar hoje: ${agenda.data.pendentes.length}`}</p>
              {!!agenda.data.pendentes.length && <ul className="divide-y divide-slate-100">{agenda.data.pendentes.slice(0, 3).map((r) => (
                <li key={r.questaoId} className="break-words py-2"><strong className="capitalize">{r.assunto}</strong> · {r.enunciado}</li>
              ))}</ul>}
              {agenda.data.futuras[0] && <p className="text-sm text-slate-600">
                Próxima revisão agendada para {dataBrasileira(agenda.data.futuras[0].proximaRevisao)}.
              </p>}
            </>}
          </CardContent>
        </Card>
        <Card className="overflow-hidden rounded-[1.25rem]">
          <CardHeader className="flex-row items-center gap-3 space-y-0 p-5 pb-3">
            <span className="grid h-10 w-10 place-items-center rounded-xl bg-indigo-100 text-indigo-700"><Clock3 aria-hidden size={20} /></span>
            <CardTitle className="text-lg">Sessões recentes</CardTitle>
          </CardHeader>
          <CardContent className="px-5 pb-5">
            <EstadoConsulta consulta={historico} recurso="o histórico" />
            {recentes?.length === 0 && <p className="rounded-xl bg-slate-50 px-3 py-3 text-sm text-slate-600">Você ainda não iniciou uma sessão.</p>}
            <ul className="divide-y divide-slate-100">{recentes?.map((s) => (
              <li key={s.id}><Link href={`/aluno/historico/${s.id}`} className="group flex items-center justify-between gap-2 rounded-lg py-2 hover:bg-indigo-50/60">
                <div className="min-w-0">
                  <p className="flex flex-wrap items-center gap-2 text-sm"><strong className="capitalize text-slate-900">{s.assunto}</strong>
                    <span className="rounded-full bg-indigo-50 px-2 py-0.5 text-xs font-semibold text-indigo-800">{situacoes[s.situacao]}</span></p>
                  <p className="mt-1 text-xs leading-relaxed text-slate-600">
                    {inicio(s)} · Acertos: {s.acertos} · Erros contabilizados: {s.erros}
                  </p>
                </div><ChevronRight aria-hidden size={16} className="shrink-0 text-indigo-600" />
              </Link></li>
            ))}</ul>
          </CardContent>
        </Card>
      </div>
      <CatalogoMaterias />
    </main>
  );
}

export default function AlunoDashboardPage() {
  return <AuthGuard allowedRole="ALUNO"><div className="min-h-screen bg-gradient-to-b from-indigo-50/70 via-slate-50 to-slate-50">
    <Navbar /><Painel />
  </div></AuthGuard>;
}
