"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ArrowLeft, ArrowRight, BookOpen } from "lucide-react";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api";
import { EstadoSessao, limiteQuestoes, mensagemErro, ResumoSessao, rotaSessoes, semQuestoes,
  useAssuntosDeEstudo, useChaveAluno } from "@/lib/estudo";

export default function PrepararAtividade() {
  const router = useRouter();
  const aluno = useChaveAluno();
  const assuntos = useAssuntosDeEstudo();
  const [escolha, setEscolha] = useState("auto");
  const sessoes = useQuery({
    queryKey: ["sessoes", aluno],
    staleTime: 0,
    queryFn: async () => (await api.get<ResumoSessao[]>(rotaSessoes)).data,
  });
  const aberta = sessoes.data?.find((s) => s.situacao !== "ENCERRADA");
  const nomeAssunto = assuntos.data?.find((assunto) => assunto.id === aberta?.assuntoId)?.nome;
  const iniciar = useMutation({
    mutationFn: async () => {
      const pedido = escolha === "auto" ? {} : { assuntoId: Number(escolha) };
      return (await api.post<EstadoSessao>(rotaSessoes, pedido)).data;
    },
    onSuccess: (estado) => router.push(`/aluno/atividade/${estado.sessao.id}`),
    onError: () => { void sessoes.refetch(); },
  });

  return (
    <>
      <Link href="/aluno" className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-700">
        <ArrowLeft size={16} aria-hidden="true" /> Voltar ao painel
      </Link>
      <Card>
        <CardHeader>
          <BookOpen className="mb-2 text-indigo-600" aria-hidden="true" />
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Sessão de estudo</h1>
        </CardHeader>
        <CardContent className="space-y-5">
          {sessoes.isPending ? <p role="status">Buscando sua sessão…</p> : sessoes.isError ? (
            <div role="alert" className="space-y-3"><p>{mensagemErro(sessoes.error)}</p>
              <Button variant="outline" onClick={() => sessoes.refetch()}>Tentar novamente</Button>
            </div>
          ) : aberta ? (
            <>
              <div>
                <h2 className="font-semibold text-slate-900">{aberta.situacao === "INTERROMPIDA" ? "Sessão pausada" : "Sessão em andamento"}</h2>
                <p className="mt-1 text-sm text-slate-600">
                  {nomeAssunto && `${nomeAssunto} · `}Questão {aberta.questoesApresentadas} de até {limiteQuestoes}
                </p>
              </div>
              <Button asChild><Link href={`/aluno/atividade/${aberta.id}`}>Abrir sessão <ArrowRight size={16} /></Link></Button>
            </>
          ) : (
            <form className="space-y-5" onSubmit={(e) => { e.preventDefault(); iniciar.mutate(); }}>
              <div>
                <label htmlFor="assunto" className="block text-sm font-semibold text-slate-800">Assunto da sessão</label>
                <select id="assunto" value={escolha} disabled={iniciar.isPending}
                  onChange={(e) => { setEscolha(e.target.value); iniciar.reset(); }}
                  className="mt-2 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-base focus-visible:ring-2 focus-visible:ring-indigo-500">
                  <option value="auto">Recomendação automática</option>
                  {assuntos.data?.map((a) => <option key={a.id} value={a.id}>{a.materia} · {a.nome}</option>)}
                </select>
                <p className="mt-2 text-sm text-slate-600">Até {limiteQuestoes} questões. Você pode pausar e continuar depois.</p>
              </div>
              {assuntos.isPending && <p role="status" className="text-sm">Carregando assuntos…</p>}
              {assuntos.isError && <div role="alert" className="space-y-2"><p>Falha ao carregar a lista de assuntos.</p>
                <Button type="button" variant="outline" onClick={() => assuntos.refetch()}>Recarregar assuntos</Button></div>}
              {iniciar.isError && <p role="alert" className="rounded-xl bg-amber-50 p-3 text-sm text-amber-900">
                {semQuestoes(iniciar.error) ? (escolha === "auto" ? "Nenhuma questão disponível para iniciar uma sessão." :
                  "Não há questões disponíveis nesse assunto. Escolha outro ou use a recomendação.") : mensagemErro(iniciar.error)}
              </p>}
              <Button type="submit" disabled={iniciar.isPending} className="w-full sm:w-auto">
                {iniciar.isPending ? "Iniciando…" : "Iniciar sessão"} <ArrowRight size={16} />
              </Button>
            </form>
          )}
        </CardContent>
      </Card>
    </>
  );
}
