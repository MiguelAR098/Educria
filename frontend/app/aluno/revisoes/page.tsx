"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { api } from "@/lib/api";
import { useChaveAluno } from "@/lib/estudo";

interface Revisao {
  questaoId: number;
  enunciado: string;
  assunto: string;
  proximaRevisao: string;
  intervaloDias: number;
}

interface Agenda {
  hoje: string;
  proximaVirada: string;
  pendentes: Revisao[];
  futuras: Revisao[];
}

function dataBrasileira(data: string) {
  return data.split("-").reverse().join("/");
}

function Grupo({ titulo, itens, vazio }: { titulo: string; itens: Revisao[]; vazio: string }) {
  return (
    <Card>
      <CardHeader><h2 className="text-xl font-bold">{titulo} ({itens.length})</h2></CardHeader>
      <CardContent>
        {itens.length === 0 ? <p className="text-slate-600">{vazio}</p> : (
          <ul className="divide-y divide-slate-200">
            {itens.map((item) => (
              <li key={item.questaoId} className="space-y-1 py-3 first:pt-0 last:pb-0">
                <div className="flex flex-wrap items-baseline justify-between gap-2 text-sm">
                  <span className="font-semibold text-indigo-700 first-letter:uppercase">{item.assunto}</span>
                  <time dateTime={item.proximaRevisao}>{dataBrasileira(item.proximaRevisao)}</time>
                </div>
                <p className="break-words font-medium">{item.enunciado}</p>
                <p className="text-sm text-slate-600">Intervalo de {item.intervaloDias} {item.intervaloDias === 1 ? "dia" : "dias"}</p>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

function AgendaDoAluno() {
  const aluno = useChaveAluno();
  const consulta = useQuery({
    queryKey: ["revisoes", aluno],
    queryFn: async ({ signal }) => (await api.get<Agenda>("/api/aluno/revisoes", { signal })).data,
    staleTime: 0,
    refetchInterval: ({ state }) => {
      if (!state.data) return false;
      const restante = Date.parse(state.data.proximaVirada) - Date.now();
      return restante > 0 ? restante + 100 : 60_000;
    },
  });
  return (
    <main className="mx-auto max-w-3xl space-y-5 px-4 py-6 sm:py-10">
      <Link href="/aluno" className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-700">
        <ArrowLeft size={16} /> Voltar ao painel
      </Link>
      <h1 className="text-3xl font-bold text-slate-900">Minhas revisões</h1>
      {consulta.isPending && <p role="status">Carregando revisões…</p>}
      {consulta.isError && <div role="alert" className="space-y-3">
        <p>Não foi possível atualizar suas revisões.</p>
        <Button variant="outline" onClick={() => consulta.refetch()}>Tentar novamente</Button>
      </div>}
      {consulta.data && <>
        <p className="text-sm text-slate-600">Agenda de {dataBrasileira(consulta.data.hoje)} · Horário de São Paulo</p>
        <Grupo titulo="Pendentes" itens={consulta.data.pendentes} vazio="Você está em dia com as revisões." />
        <Grupo titulo="Futuras" itens={consulta.data.futuras} vazio="Você ainda não tem revisões agendadas." />
        {consulta.data.pendentes.length > 0 && <Button asChild><Link href="/aluno/atividade">Escolher assunto para estudar</Link></Button>}
      </>}
    </main>
  );
}

export default function RevisoesPage() {
  return <AuthGuard allowedRole="ALUNO"><div className="min-h-screen bg-slate-50"><Navbar /><AgendaDoAluno /></div></AuthGuard>;
}
