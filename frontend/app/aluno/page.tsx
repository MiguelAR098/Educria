"use client";

import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import { DesempenhoAluno, PontosTurma } from "@/lib/types";
import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { CatalogoMaterias } from "@/components/CatalogoMaterias";
import { LevelCard } from "@/components/LevelCard";
import { CollectivePointsCard } from "@/components/CollectivePointsCard";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Play,
  Sparkles,
  BookOpen,
  Compass,
  RefreshCw,
} from "lucide-react";
import { toast } from "sonner";

export default function AlunoDashboardPage() {
  const { user, token, _hasHydrated } = useAuthStore();
  const queryClient = useQueryClient();
  const alunoAtivo = _hasHydrated && !!token && user?.role === "ALUNO";

  const desempenhoQuery = useQuery<DesempenhoAluno>({
    queryKey: ["aluno-desempenho", user?.id ?? user?.email],
    enabled: alunoAtivo,
    queryFn: async () => {
      const { data } = await api.get<DesempenhoAluno>("/api/aluno/desempenho");
      return data;
    },
  });

  const pontosTurmaQuery = useQuery<PontosTurma>({
    queryKey: ["aluno-turma-pontos", user?.id ?? user?.email],
    enabled: alunoAtivo,
    queryFn: async () => {
      try {
        const { data } = await api.get<PontosTurma>("/api/aluno/turma/pontos");
        return data;
      } catch (error: any) {
        if (error?.response?.status === 400) {
          return {
            turmaId: null,
            nomeTurma: "Sem turma vinculada",
            pontosTotais: 0,
            metaTurma: 1000,
          };
        }
        throw error;
      }
    },
  });

  const primeiroNome = user?.nome?.split(" ")[0] || "Estudante";

  return (
    <AuthGuard allowedRole="ALUNO">
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />

        <main className="flex-1 mx-auto w-full max-w-5xl px-4 sm:px-6 py-8 space-y-8">
          {/* Saudação e CTA Principal */}
          <section className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-sm">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <Badge variant="secondary" className="gap-1">
                  <Compass className="h-3.5 w-3.5" />
                  Ensino Fundamental II
                </Badge>
              </div>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
                Olá, {primeiroNome}! Pronto para evoluir?
              </h1>
              <p className="text-sm text-slate-500">
                Acompanhe seu nível, conquiste medalhas e ajude sua turma a
                subir no placar.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <Button asChild variant="outline"><Link href="/aluno/historico">Histórico de sessões</Link></Button>
              <Button asChild variant="outline"><Link href="/aluno/revisoes">Minhas revisões</Link></Button>
              <Button
                variant="outline"
                size="icon"
                title="Atualizar dados"
                onClick={() => {
                  desempenhoQuery.refetch();
                  pontosTurmaQuery.refetch();
                  void queryClient.invalidateQueries({ queryKey: ["catalogo"] });
                  toast.success("Dados atualizados!");
                }}
                disabled={
                  desempenhoQuery.isFetching || pontosTurmaQuery.isFetching
                }
              >
                <RefreshCw
                  className={`h-4 w-4 ${
                    desempenhoQuery.isFetching || pontosTurmaQuery.isFetching
                      ? "animate-spin text-indigo-600"
                      : ""
                  }`}
                />
              </Button>

              <Button
                size="lg"
                asChild
                className="bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-700 hover:to-violet-700 text-white shadow-md shadow-indigo-200 gap-2.5 px-6"
              >
                <Link href="/aluno/atividade">
                  <Play className="h-5 w-5 fill-current" /> Fazer atividade
                </Link>
              </Button>
            </div>
          </section>

          {/* Alerta amigável caso backend não esteja respondendo */}
          {(desempenhoQuery.isError || pontosTurmaQuery.isError) && (
            <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800 flex items-center justify-between gap-4">
              <span>
                Não foi possível carregar alguns dados do servidor.
                Verifique se a API está ativa.
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  desempenhoQuery.refetch();
                  pontosTurmaQuery.refetch();
                }}
              >
                Tentar novamente
              </Button>
            </div>
          )}

          {/* Grid Principal: Nível + Pontos Coletivos */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2">
              <LevelCard
                desempenho={desempenhoQuery.data}
                isLoading={desempenhoQuery.isLoading}
              />
            </div>

            <div className="lg:col-span-1 flex flex-col gap-6">
              <CollectivePointsCard
                pontosTurma={pontosTurmaQuery.data}
                isLoading={pontosTurmaQuery.isLoading}
              />

              {/* Card Motivacional Extra */}
              <Card className="border-emerald-200/80 bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-md">
                <CardContent className="p-5 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-white/20 px-3 py-1 text-xs font-semibold">
                      <Sparkles className="h-3.5 w-3.5" />
                      Desafio Diário
                    </span>
                    <BookOpen className="h-5 w-5 text-emerald-100" />
                  </div>
                  <h3 className="text-lg font-bold leading-snug">
                    Trilha Adaptativa Personalizada
                  </h3>
                  <p className="text-xs text-emerald-50 leading-relaxed">
                    As questões se ajustam ao seu ritmo! Quanto mais você
                    pratica, mais rápido desbloqueia novos níveis.
                  </p>
                </CardContent>
              </Card>
            </div>
          </div>
          <CatalogoMaterias />
        </main>
      </div>
    </AuthGuard>
  );
}
