"use client";

import { useEffect, useRef } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, MessageSquare } from "lucide-react";
import { api } from "@/lib/api";
import { useChaveAluno } from "@/lib/estudo";
import { RecadosDoAluno } from "@/lib/types";
import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";

function quando(iso: string): string {
  return new Date(iso).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export default function RecadosDoAlunoPage() {
  const aluno = useChaveAluno();
  const cache = useQueryClient();

  const recados = useQuery({
    queryKey: ["aluno-recados", aluno],
    queryFn: async () => {
      const { data } = await api.get<RecadosDoAluno>("/api/aluno/recados");
      return data;
    },
  });

  const marcarLidos = useMutation({
    mutationFn: async () => {
      await api.post("/api/aluno/recados/lidos");
    },
    // Atualiza só o contador da página inicial; a lista daqui continua mostrando quais eram novos.
    onSuccess: () => cache.invalidateQueries({ queryKey: ["aluno-recados-resumo", aluno] }),
  });

  // Marca como lidos uma única vez, depois de mostrar quais eram novos.
  const jaMarcou = useRef(false);
  useEffect(() => {
    if (recados.data && recados.data.naoLidos > 0 && !jaMarcou.current) {
      jaMarcou.current = true;
      marcarLidos.mutate();
    }
  }, [recados.data, marcarLidos]);

  return (
    <AuthGuard allowedRole="ALUNO">
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />

        <main className="flex-1 mx-auto w-full max-w-3xl px-4 sm:px-6 py-8 space-y-6">
          <Link href="/aluno" className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-700">
            <ArrowLeft className="h-4 w-4" /> Voltar
          </Link>

          <h1 className="flex items-center gap-2 text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
            <MessageSquare className="h-6 w-6 text-indigo-600" />
            Recados do professor
          </h1>

          {recados.isLoading && <Skeleton className="h-32 w-full" />}

          {recados.isError && (
            <p className="text-sm text-red-600">Não foi possível carregar seus recados. Tente novamente.</p>
          )}

          {recados.data && recados.data.recados.length === 0 && (
            <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-700">
              Você ainda não recebeu nenhum recado.
            </p>
          )}

          {recados.data && recados.data.recados.length > 0 && (
            <ul className="space-y-3">
              {recados.data.recados.map((r) => (
                <li key={r.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                  <div className="mb-2 flex flex-wrap items-center justify-between gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500">
                    <span>
                      {r.de ?? "Professor"} · {quando(r.criadoEm)}
                    </span>
                    {!r.lido && <Badge variant="default">Novo</Badge>}
                  </div>
                  <p className="text-slate-800">{r.texto}</p>
                </li>
              ))}
            </ul>
          )}
        </main>
      </div>
    </AuthGuard>
  );
}
