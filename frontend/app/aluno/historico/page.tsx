"use client";

import Link from "next/link";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useHistorico, inicio, situacoes } from "@/lib/historico";

export default function HistoricoPage() {
  const consulta = useHistorico();
  const sessoes = consulta.data?.pages.flatMap((pagina) => pagina.sessoes);
  return (
    <>
      <Link href="/aluno" className="inline-flex items-center gap-2 font-semibold text-indigo-700">
        <ArrowLeft size={16} /> Voltar ao painel
      </Link>
      <h1 className="text-3xl font-bold text-slate-900">Histórico de sessões</h1>
      {consulta.isPending && <p role="status">Carregando histórico…</p>}
      {consulta.isError && <div role="alert" className="space-y-2"><p>Não foi possível carregar o histórico.</p>
        <Button variant="outline" onClick={() => consulta.isFetchNextPageError ? consulta.fetchNextPage() : consulta.refetch()}>
          Tentar novamente
        </Button></div>}
      {sessoes?.length === 0 && <Card><CardContent className="space-y-4 pt-6">
        <p>Você ainda não iniciou uma sessão.</p><Button asChild><Link href="/aluno/atividade">Fazer atividade</Link></Button>
      </CardContent></Card>}
      {!!sessoes?.length && <ul className="space-y-3">{sessoes.map((s) => (
        <li key={s.id}><Link href={`/aluno/historico/${s.id}`}
          className="flex items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm hover:border-indigo-400 focus-visible:ring-2 focus-visible:ring-indigo-600">
          <div className="min-w-0 space-y-1">
            <p className="font-semibold text-slate-900 first-letter:uppercase">{s.assunto}</p>
            <p className="text-sm text-slate-600">{inicio(s)} · {situacoes[s.situacao]}</p>
            <p className="text-sm">{s.apresentadas} questões apresentadas · {s.tentativas} tentativas</p>
            <p className="text-sm">Acertos: {s.acertos} · Tentativas incorretas: {s.tentativasIncorretas} · Erros contabilizados: {s.erros}</p>
          </div><ArrowRight size={18} className="shrink-0 text-indigo-600" aria-hidden="true" />
        </Link></li>))}</ul>}
      {consulta.hasNextPage && !consulta.isFetchNextPageError &&
        <Button variant="outline" disabled={consulta.isFetching} onClick={() => consulta.fetchNextPage()}>
          {consulta.isFetchingNextPage ? "Carregando…" : "Carregar anteriores"}
        </Button>}
    </>
  );
}
