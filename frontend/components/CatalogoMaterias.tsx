"use client";

import { useQuery } from "@tanstack/react-query";
import { BookOpen, LockKeyhole } from "lucide-react";
import { api } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

interface Materia {
  id: number;
  nome: string;
  totalAssuntos: number;
}

interface Assunto {
  id: number;
  nome: string;
  nivelAtual: number;
}

function AvisoDeErro({ mensagem, tentar }: { mensagem: string; tentar: () => void }) {
  return (
    <div role="alert" className="flex flex-wrap items-center gap-3 text-sm text-slate-600">
      {mensagem}
      <Button variant="outline" size="sm" onClick={tentar}>Tentar novamente</Button>
    </div>
  );
}

function AssuntosDaMateria({ materia }: { materia: Materia }) {
  const aluno = useAuthStore((state) => state.user);
  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ["catalogo", "assuntos", aluno?.id ?? aluno?.email, materia.id],
    queryFn: async () => (await api.get<Assunto[]>(`/api/aluno/materias/${materia.id}/assuntos`)).data,
  });

  return (
    <Card>
      <CardHeader className="flex-row items-center gap-3 space-y-0">
        <BookOpen aria-hidden="true" className="h-5 w-5 text-indigo-600" />
        <h3 className="text-xl font-bold text-slate-900">{materia.nome}</h3>
      </CardHeader>
      <CardContent>
        {isPending && <p role="status" className="text-sm text-slate-500">Carregando assuntos…</p>}
        {isError && <AvisoDeErro mensagem="Não foi possível carregar os níveis." tentar={refetch} />}
        {data && (
          <ul className="grid gap-x-8 sm:grid-cols-2">
            {data.map((assunto) => (
              <li key={assunto.id} className="flex items-center justify-between gap-3 border-t border-slate-100 py-3">
                <span className="text-sm font-medium text-slate-800 first-letter:uppercase">{assunto.nome}</span>
                <span className="whitespace-nowrap rounded-full bg-indigo-50 px-2.5 py-1 text-xs font-semibold text-indigo-700">
                  Nível {assunto.nivelAtual}/10
                </span>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

export function CatalogoMaterias() {
  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ["catalogo", "materias"],
    queryFn: async () => (await api.get<Materia[]>("/api/aluno/materias")).data,
  });

  return (
    <section aria-labelledby="titulo-materias" className="space-y-4">
      <h2 id="titulo-materias" className="text-2xl font-bold tracking-tight text-slate-900">Minhas matérias</h2>
      {isPending && <p role="status" className="text-sm text-slate-500">Carregando matérias…</p>}
      {isError && <AvisoDeErro mensagem="Não foi possível carregar as matérias." tentar={refetch} />}
      {data?.length === 0 && <p className="text-sm text-slate-500">Nenhuma matéria cadastrada.</p>}
      {data?.filter((materia) => materia.totalAssuntos > 0).map((materia) => (
        <AssuntosDaMateria key={materia.id} materia={materia} />
      ))}
      <div className="grid gap-3 sm:grid-cols-2">
        {data?.filter((materia) => materia.totalAssuntos === 0).map((materia) => (
          <div key={materia.id} className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 px-4 py-3">
            <h3 className="font-semibold text-slate-700">{materia.nome}</h3>
            <span className="inline-flex items-center gap-1.5 text-xs text-slate-500">
              <LockKeyhole aria-hidden="true" className="h-3.5 w-3.5" /> Indisponível
            </span>
          </div>
        ))}
      </div>
    </section>
  );
}
