"use client";

import { useState } from "react";
import { EvolucaoAssunto } from "@/components/EvolucaoAssunto";
import { useQuery } from "@tanstack/react-query";
import { BookOpen, ChevronDown, LockKeyhole } from "lucide-react";
import { api } from "@/lib/api";
import { useChaveAluno } from "@/lib/estudo";
import { formatarPontos } from "@/lib/pontuacao";
import type { AssuntoDoAluno, MateriaDoAluno } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

function AvisoDeErro({ mensagem, tentar }: { mensagem: string; tentar: () => void }) {
  return (
    <div role="alert" className="flex flex-wrap items-center gap-3 text-sm text-slate-600">
      {mensagem}
      <Button variant="outline" size="sm" onClick={tentar}>Tentar novamente</Button>
    </div>
  );
}

function AssuntosDaMateria({ materia }: { materia: MateriaDoAluno }) {
  const [aberto, setAberto] = useState<number | null>(null);
  const aluno = useChaveAluno();
  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ["catalogo", "assuntos", aluno, materia.id],
    queryFn: async ({ signal }) => (await api.get<AssuntoDoAluno[]>(`/api/aluno/materias/${materia.id}/assuntos`, { signal })).data,
  });

  return (
    <Card className="overflow-hidden rounded-[1.25rem]">
      <CardHeader className="flex-row flex-wrap items-center justify-between gap-3 space-y-0 border-b border-slate-100 bg-gradient-to-r from-indigo-50/80 to-white px-5 py-4">
        <div className="flex min-w-0 max-w-full items-center gap-3">
          <span className="grid h-[40px] w-[40px] shrink-0 place-items-center rounded-xl bg-indigo-100 text-indigo-700">
            <BookOpen aria-hidden size={20} />
          </span>
          <h3 className="min-w-0 [overflow-wrap:anywhere] text-lg font-bold text-slate-900">{materia.nome}</h3>
        </div>
        <span className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs font-semibold text-slate-600">
          <span>{materia.totalAssuntos} {materia.totalAssuntos === 1 ? "assunto" : "assuntos"}</span>
          <span className="text-indigo-700">{formatarPontos(materia.pontosIndividuais)}</span>
        </span>
      </CardHeader>
      <CardContent className="px-5 pb-2 pt-2">
        {isPending && <p role="status" className="text-sm text-slate-500">Carregando assuntos…</p>}
        {isError && <AvisoDeErro mensagem="Não foi possível carregar os níveis." tentar={refetch} />}
        {data && (
          <ul className="grid gap-x-8 sm:grid-cols-2">
            {data.map((assunto) => (
              <li key={assunto.id} className="min-w-0 border-b border-slate-100 py-3">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <span className="text-sm font-medium text-slate-800 first-letter:uppercase">{assunto.nome}</span>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="whitespace-nowrap rounded-full bg-indigo-50 px-2.5 py-1 text-xs font-semibold text-indigo-700">
                      Nível {assunto.nivelAtual}/10
                    </span>
                    <span className="text-xs font-semibold text-slate-600">{formatarPontos(assunto.pontosIndividuais)}</span>
                  </div>
                </div>
                <button type="button" aria-expanded={aberto === assunto.id}
                  aria-label={`Evolução de ${assunto.nome}`}
                  onClick={() => setAberto(aberto === assunto.id ? null : assunto.id)}
                  className="mt-1 inline-flex items-center gap-1.5 rounded py-2 text-xs font-bold text-indigo-700 hover:underline">
                  {aberto === assunto.id ? "Ocultar evolução" : "Ver evolução"}
                  <ChevronDown aria-hidden size={14} className={aberto === assunto.id ? "rotate-180" : ""} />
                </button>
                {aberto === assunto.id && <EvolucaoAssunto assuntoId={assunto.id} />}
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

export function CatalogoMaterias({ pontosIndividuais }: { pontosIndividuais?: number }) {
  const aluno = useChaveAluno();
  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ["catalogo", "materias", aluno],
    queryFn: async ({ signal }) => (await api.get<MateriaDoAluno[]>("/api/aluno/materias", { signal })).data,
  });

  return (
    <section aria-labelledby="titulo-materias" className="space-y-4">
      <h2 id="titulo-materias" tabIndex={-1} className="scroll-mt-20 text-2xl font-bold tracking-tight text-slate-900">Minhas matérias</h2>
      <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-slate-600">
        <p className="flex items-center gap-2">
          <LockKeyhole aria-hidden="true" className="h-4 w-4 shrink-0" /> Sua pontuação individual é privada.
        </p>
        {pontosIndividuais != null && <p className="font-semibold text-indigo-700">
          {formatarPontos(pontosIndividuais)} no total
        </p>}
      </div>
      {isPending && <p role="status" className="text-sm text-slate-500">Carregando matérias…</p>}
      {isError && <AvisoDeErro mensagem="Não foi possível carregar as matérias." tentar={refetch} />}
      {data?.length === 0 && <p className="text-sm text-slate-500">Nenhuma matéria cadastrada.</p>}
      {data?.filter((materia) => materia.totalAssuntos > 0).map((materia) => (
        <AssuntosDaMateria key={materia.id} materia={materia} />
      ))}
      <div className="grid gap-3 sm:grid-cols-2">
        {data?.filter((materia) => materia.totalAssuntos === 0).map((materia) => (
          <div key={materia.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 px-4 py-3">
            <h3 className="font-semibold text-slate-700">{materia.nome}</h3>
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
              <span>{formatarPontos(materia.pontosIndividuais)}</span>
              <span className="inline-flex items-center gap-1.5">
                <LockKeyhole aria-hidden="true" className="h-3.5 w-3.5" /> Indisponível
              </span>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
