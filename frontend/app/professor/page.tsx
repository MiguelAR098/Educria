"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { PerfilProfessor } from "@/lib/types";
import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { BookOpen, ChevronRight, School, Users } from "lucide-react";

export default function ProfessorPage() {
  const perfil = useQuery({
    queryKey: ["professor", "perfil"],
    queryFn: async () => {
      const { data } = await api.get<PerfilProfessor>("/api/professor/perfil");
      return data;
    },
  });

  return (
    <AuthGuard allowedRole="PROFESSOR">
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />

        <main className="flex-1 mx-auto w-full max-w-5xl px-4 sm:px-6 py-8 space-y-8">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div className="space-y-1">
              <Badge variant="secondary" className="gap-1">
                <BookOpen className="h-3.5 w-3.5" />
                Painel do Professor
              </Badge>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
                {perfil.data ? `Olá, ${perfil.data.nome}` : "Painel do Professor"}
              </h1>
              <p className="text-sm text-slate-500">
                {perfil.data
                  ? `${perfil.data.materia} · acompanhe suas turmas.`
                  : "Acompanhe suas turmas."}
              </p>
            </div>
            <Link
              href="/professor/questoes"
              className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-700"
            >
              <BookOpen className="h-4 w-4" />
              Banco de questões
            </Link>
          </div>

          {perfil.isLoading && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <Skeleton className="h-32 w-full" />
              <Skeleton className="h-32 w-full" />
            </div>
          )}

          {perfil.isError && (
            <p className="text-sm text-red-600">
              Não foi possível carregar seus dados. Tente novamente.
            </p>
          )}

          {perfil.data && perfil.data.turmas.length === 0 && (
            <p className="text-sm text-slate-500">
              Você ainda não está vinculado a nenhuma turma.
            </p>
          )}

          {perfil.data && perfil.data.turmas.length > 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              {perfil.data.turmas.map((turma) => (
                <Link
                  key={turma.id}
                  href={`/professor/turma/${turma.id}`}
                  className="block rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500"
                >
                <Card className="h-full shadow-md border-slate-200/80 transition-shadow hover:shadow-lg">
                  <CardHeader>
                    <div className="flex items-center gap-3">
                      <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-indigo-100 text-indigo-600">
                        <School className="h-5 w-5" />
                      </div>
                      <div>
                        <CardTitle className="text-lg">{turma.nome}</CardTitle>
                        <p className="text-xs text-slate-500">
                          {turma.codigo} · {turma.anoLetivo}
                        </p>
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="flex items-center justify-between text-sm text-slate-600">
                      <div className="flex items-center gap-2">
                        <Users className="h-4 w-4 text-slate-400" />
                        <span>
                          {turma.totalAlunos}{" "}
                          {turma.totalAlunos === 1 ? "aluno" : "alunos"}
                        </span>
                      </div>
                      <span className="flex items-center gap-1 font-medium text-indigo-700">
                        Abrir turma
                        <ChevronRight className="h-4 w-4" />
                      </span>
                    </div>
                  </CardContent>
                </Card>
                </Link>
              ))}
            </div>
          )}
        </main>
      </div>
    </AuthGuard>
  );
}
