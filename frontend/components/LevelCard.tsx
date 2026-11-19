"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { DesempenhoAluno } from "@/lib/types";
import {
  Trophy,
  CheckCircle2,
  XCircle,
  Flame,
  Star,
  Target,
} from "lucide-react";

interface LevelCardProps {
  desempenho?: DesempenhoAluno;
  isLoading: boolean;
}

const LEVEL_CONFIG: Record<
  number,
  { title: string; gradient: string; badgeVariant: "default" | "secondary" | "success" | "warning" }
> = {
  1: { title: "Explorador Iniciante", gradient: "from-indigo-500 to-blue-500", badgeVariant: "default" },
  2: { title: "Aprendiz Curioso", gradient: "from-indigo-600 to-violet-500", badgeVariant: "default" },
  3: { title: "Desbravador do Saber", gradient: "from-violet-600 to-purple-500", badgeVariant: "secondary" },
  4: { title: "Estrategista", gradient: "from-purple-600 to-fuchsia-500", badgeVariant: "secondary" },
  5: { title: "Guardião do Conhecimento", gradient: "from-emerald-500 to-teal-500", badgeVariant: "success" },
  6: { title: "Mestre em Evolução", gradient: "from-emerald-600 to-cyan-600", badgeVariant: "success" },
  7: { title: "Sábio da Turma", gradient: "from-amber-500 to-orange-500", badgeVariant: "warning" },
  8: { title: "Campeão Adaptativo", gradient: "from-orange-500 to-rose-500", badgeVariant: "warning" },
  9: { title: "Lenda do Ensino", gradient: "from-indigo-600 via-violet-600 to-emerald-500", badgeVariant: "success" },
  10: { title: "Grão-Mestre EDUCRIA", gradient: "from-amber-400 via-indigo-600 to-emerald-500", badgeVariant: "success" },
};

export function LevelCard({ desempenho, isLoading }: LevelCardProps) {
  if (isLoading) {
    return (
      <Card className="overflow-hidden border-0 shadow-md">
        <div className="h-28 bg-slate-200 animate-pulse" />
        <CardContent className="p-6 space-y-6">
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-4 w-full" />
          <div className="grid grid-cols-2 gap-4">
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-24 w-full" />
          </div>
        </CardContent>
      </Card>
    );
  }

  const nivel = Math.min(Math.max(desempenho?.nivelAtual ?? 1, 1), 10);
  const acertos = desempenho?.totalAcertos ?? 0;
  const erros = desempenho?.totalErros ?? 0;
  const totalRespostas = acertos + erros;
  const taxaAcerto =
    totalRespostas > 0 ? Math.round((acertos / totalRespostas) * 100) : 0;

  // Calcula progresso: usa percentual da API se existir, senão estima pelos acertos no nível
  const progresso =
    desempenho?.progressoPercentual !== undefined
      ? Math.min(Math.max(desempenho.progressoPercentual, 0), 100)
      : nivel === 10
      ? 100
      : Math.min(((acertos % 10) / 10) * 100, 100);

  const levelInfo = LEVEL_CONFIG[nivel] || LEVEL_CONFIG[1];

  return (
    <Card className="overflow-hidden border-slate-200/80 shadow-md hover:shadow-lg transition-all duration-300">
      {/* Banner superior colorido */}
      <div
        className={`bg-gradient-to-r ${levelInfo.gradient} p-6 text-white relative overflow-hidden`}
      >
        <div className="absolute -right-6 -top-6 h-32 w-32 rounded-full bg-white/10 blur-xl" />
        <div className="flex flex-wrap items-center justify-between gap-4 relative z-10">
          <div className="flex items-center gap-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-white/20 backdrop-blur-sm border border-white/30 shadow-inner">
              <Trophy className="h-8 w-8 text-amber-200" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold uppercase tracking-wider text-white/80">
                  Seu Nível Atual
                </span>
                <span className="inline-flex items-center gap-1 rounded-full bg-white/20 px-2.5 py-0.5 text-xs font-semibold">
                  <Star className="h-3 w-3 fill-amber-300 text-amber-300" />
                  Nível {nivel} / 10
                </span>
              </div>
              <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight mt-0.5">
                {levelInfo.title}
              </h2>
            </div>
          </div>

          <div className="flex items-center gap-2 bg-white/15 backdrop-blur-sm rounded-xl px-3.5 py-2 border border-white/20">
            <Flame className="h-5 w-5 text-amber-300 fill-amber-300" />
            <div>
              <p className="text-[11px] text-white/80 leading-none">Precisão</p>
              <p className="text-base font-bold leading-tight">{taxaAcerto}%</p>
            </div>
          </div>
        </div>
      </div>

      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base font-semibold text-slate-700 flex items-center gap-2">
            <Target className="h-4 w-4 text-indigo-600" />
            Progresso para o {nivel < 10 ? `Nível ${nivel + 1}` : "Nível Máximo"}
          </CardTitle>
          <Badge variant={levelInfo.badgeVariant}>
            {Math.round(progresso)}% concluído
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-6">
        <div className="space-y-2">
          <Progress
            value={progresso}
            className="h-4 bg-slate-100"
            indicatorClassName="bg-gradient-to-r from-indigo-600 via-violet-600 to-emerald-500"
          />
          <p className="text-xs text-slate-500 flex justify-between">
            <span>
              {nivel < 10
                ? "Continue praticando para subir de nível!"
                : "Parabéns! Você atingiu o nível máximo da jornada!"}
            </span>
            <span className="font-semibold text-indigo-600">
              {totalRespostas} questões respondidas
            </span>
          </p>
        </div>

        {/* Estatísticas de Acertos e Erros */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
          <div className="flex items-center gap-4 rounded-2xl border border-emerald-200/80 bg-emerald-50/70 p-4 transition-transform hover:-translate-y-0.5">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-emerald-500 text-white shadow-sm">
              <CheckCircle2 className="h-6 w-6" />
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-emerald-700">
                Total de Acertos
              </p>
              <p className="text-2xl font-extrabold text-emerald-900">
                {acertos}
              </p>
              <p className="text-xs text-emerald-600">
                Excelente desempenho!
              </p>
            </div>
          </div>

          <div className="flex items-center gap-4 rounded-2xl border border-rose-200/70 bg-rose-50/60 p-4 transition-transform hover:-translate-y-0.5">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-rose-500 text-white shadow-sm">
              <XCircle className="h-6 w-6" />
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-rose-700">
                Total de Erros
              </p>
              <p className="text-2xl font-extrabold text-rose-900">
                {erros}
              </p>
              <p className="text-xs text-rose-600">
                Cada erro ajuda a aprender
              </p>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
