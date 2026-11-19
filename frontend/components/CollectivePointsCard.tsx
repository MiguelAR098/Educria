"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { PontosTurma } from "@/lib/types";
import { Users, Sparkles, Award } from "lucide-react";

interface CollectivePointsCardProps {
  pontosTurma?: PontosTurma;
  isLoading: boolean;
}

export function CollectivePointsCard({
  pontosTurma,
  isLoading,
}: CollectivePointsCardProps) {
  if (isLoading) {
    return (
      <Card className="shadow-md">
        <CardHeader>
          <Skeleton className="h-6 w-48" />
        </CardHeader>
        <CardContent className="space-y-4">
          <Skeleton className="h-14 w-32" />
          <Skeleton className="h-3 w-full" />
        </CardContent>
      </Card>
    );
  }

  const pontos = pontosTurma?.pontosTotais ?? 0;
  const meta = pontosTurma?.metaTurma ?? 1000;
  const progressoMeta = Math.min(Math.round((pontos / meta) * 100), 100);

  return (
    <Card className="border-indigo-100 bg-gradient-to-br from-white via-indigo-50/30 to-violet-50/50 shadow-md hover:shadow-lg transition-all duration-300 flex flex-col justify-between">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-violet-100 text-violet-700">
              <Users className="h-5 w-5" />
            </div>
            <div>
              <CardTitle className="text-lg">Pontos da Turma</CardTitle>
              <p className="text-xs text-slate-500">
                {pontosTurma?.nomeTurma || "Conquista Coletiva"}
              </p>
            </div>
          </div>
          <Badge variant="success" className="gap-1">
            <Sparkles className="h-3 w-3" />
            Colaborativo
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-5">
        <div className="rounded-2xl bg-white p-4 border border-indigo-100/80 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Pontuação Acumulada
            </span>
            <div className="flex items-baseline gap-1.5 mt-0.5">
              <span className="text-3xl sm:text-4xl font-extrabold bg-gradient-to-r from-indigo-600 to-violet-600 bg-clip-text text-transparent">
                {pontos.toLocaleString("pt-BR")}
              </span>
              <span className="text-sm font-bold text-slate-500">pts</span>
            </div>
          </div>
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-amber-100 text-amber-600">
            <Award className="h-6 w-6" />
          </div>
        </div>

        <div className="space-y-2">
          <div className="flex justify-between text-xs font-medium">
            <span className="text-slate-600">Meta coletiva da turma</span>
            <span className="text-emerald-600 font-bold">
              {pontos.toLocaleString("pt-BR")} / {meta.toLocaleString("pt-BR")} pts
            </span>
          </div>
          <Progress
            value={progressoMeta}
            className="h-3 bg-indigo-100/70"
            indicatorClassName="bg-gradient-to-r from-emerald-500 to-teal-500"
          />
          <p className="text-xs text-slate-500">
            Cada atividade concluída por você soma pontos para toda a turma!
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
