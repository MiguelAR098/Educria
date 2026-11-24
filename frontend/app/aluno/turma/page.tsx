"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Trophy, Users } from "lucide-react";
import { api } from "@/lib/api";
import { nomeDoTipo } from "@/lib/atividades";
import { useChaveAluno } from "@/lib/estudo";
import { HistoricoDeAtividades } from "@/lib/types";
import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";

function quando(iso: string): string {
  return new Date(iso).toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" });
}

export default function AtividadesDaTurmaPage() {
  const aluno = useChaveAluno();

  const historico = useQuery({
    queryKey: ["aluno-atividades-da-turma", aluno],
    queryFn: async ({ signal }) =>
      (await api.get<HistoricoDeAtividades>("/api/aluno/atividades-da-turma", { signal })).data,
  });

  return (
    <AuthGuard allowedRole="ALUNO">
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />

        <main className="flex-1 mx-auto w-full max-w-3xl px-4 sm:px-6 py-8 space-y-6">
          <Link href="/aluno" className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-700">
            <ArrowLeft className="h-4 w-4" /> Voltar
          </Link>

          <div className="space-y-2">
            <h1 className="flex items-center gap-2 text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
              <Users className="h-6 w-6 text-indigo-600" />
              O que a sua turma fez junto
            </h1>
            <p className="text-sm text-slate-600">
              Estes pontos são da turma inteira, por atividades feitas em conjunto. Eles não mudam o seu nível
              individual, e o seu nível não muda os pontos da turma.
            </p>
          </div>

          {historico.isLoading && <Skeleton className="h-40 w-full" />}

          {historico.isError && (
            <p className="text-sm text-red-600">Não foi possível carregar as atividades. Tente novamente.</p>
          )}

          {historico.data && (
            <>
              <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <Trophy aria-hidden className="h-7 w-7 text-amber-500" />
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Pontos da turma</p>
                  <p className="text-3xl font-extrabold tabular-nums text-slate-900">{historico.data.pontosTotais}</p>
                </div>
              </div>

              {historico.data.atividades.length === 0 ? (
                <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-700">
                  Sua turma ainda não registrou nenhuma atividade em conjunto.
                </p>
              ) : (
                <ul className="space-y-3">
                  {historico.data.atividades.map((a) => (
                    <li key={a.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <div>
                          <p className="font-bold text-slate-900">{a.titulo}</p>
                          <p className="text-xs text-slate-500">
                            {nomeDoTipo(a.tipo)} · {quando(a.criadoEm)}
                            {a.lancadaPor && ` · registrada por ${a.lancadaPor}`}
                          </p>
                        </div>
                        <Badge variant="success">+{a.pontos} pontos</Badge>
                      </div>
                      {a.resumo && <p className="mt-2 text-sm text-slate-700">{a.resumo}</p>}
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </main>
      </div>
    </AuthGuard>
  );
}
