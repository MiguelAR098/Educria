"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { api } from "@/lib/api";
import { AlunoDaTurma, PanoramaDaTurma, TurmaComAlunos } from "@/lib/types";
import { SITUACAO } from "@/lib/situacao";
import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertTriangle, ArrowLeft, BarChart3, BellRing, Grid3x3, TrendingUp, Trophy, Users } from "lucide-react";

const variacao = new Intl.NumberFormat("pt-BR", {
  signDisplay: "exceptZero",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const decimal = new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 });

function diaCurto(dia: string): string {
  return new Date(`${dia}T00:00:00`).toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" });
}

function motivo(aluno: AlunoDaTurma): string {
  const total = aluno.acertos + aluno.erros;
  if (aluno.situacao === "EM_RISCO") {
    return `Acertou ${aluno.taxaAcerto}% em ${total} respostas`;
  }
  if (aluno.errosConsecutivos >= 1) {
    return aluno.errosConsecutivos === 1
      ? "1 erro seguido: o próximo pode rebaixar o nível"
      : `${aluno.errosConsecutivos} erros seguidos`;
  }
  return `Acertou ${aluno.taxaAcerto}% em ${total} respostas`;
}

function ultimaAtividade(iso: string | null): string {
  if (!iso) return "—";
  const dias = Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000);
  if (dias <= 0) return "Hoje";
  if (dias === 1) return "Ontem";
  return `Há ${dias} dias`;
}

export default function TurmaDoProfessorPage() {
  const { id } = useParams<{ id: string }>();

  const turma = useQuery({
    queryKey: ["professor", "turma", id],
    queryFn: async () => {
      const { data } = await api.get<TurmaComAlunos>(`/api/professor/turmas/${id}`);
      return data;
    },
    retry: false,
  });

  const panorama = useQuery({
    queryKey: ["professor", "turma", id, "panorama"],
    queryFn: async () => (await api.get<PanoramaDaTurma>(`/api/professor/turmas/${id}/panorama`)).data,
    retry: false,
  });
  const indicadores = new Map((panorama.data?.alunos ?? []).map((i) => [i.alunoId, i]));
  const tendencia = (panorama.data?.tendencia ?? []).filter((p) => p.nivelMedio !== null);

  const proibido = (turma.error as AxiosError | null)?.response?.status === 403;
  const alunos = [...(turma.data?.alunos ?? [])].sort(
    (a, b) =>
      SITUACAO[a.situacao].ordem - SITUACAO[b.situacao].ordem ||
      a.nome.localeCompare(b.nome, "pt-BR")
  );
  const precisamAtencao = alunos.filter(
    (a) => a.situacao === "EM_RISCO" || a.situacao === "ATENCAO"
  );

  return (
    <AuthGuard allowedRole="PROFESSOR">
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />

        <main className="flex-1 mx-auto w-full max-w-5xl px-4 sm:px-6 py-8 space-y-8">
          <Link
            href="/professor"
            className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-700"
          >
            <ArrowLeft className="h-4 w-4" /> Minhas turmas
          </Link>

          {turma.isLoading && (
            <div className="space-y-4">
              <Skeleton className="h-10 w-64" />
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-64 w-full" />
            </div>
          )}

          {turma.isError && (
            <p className="text-sm text-red-600">
              {proibido
                ? "Você não leciona nesta turma."
                : "Não foi possível carregar a turma. Tente novamente."}
            </p>
          )}

          {turma.data && (
            <>
              <div className="flex flex-wrap items-end justify-between gap-4">
                <div className="space-y-1">
                  <Badge variant="secondary" className="gap-1">
                    <Users className="h-3.5 w-3.5" />
                    Turma
                  </Badge>
                  <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
                    {turma.data.nome}
                  </h1>
                  <p className="text-sm text-slate-500">
                    {turma.data.codigo} · {turma.data.anoLetivo}
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Link
                    href={`/professor/turma/${turma.data.id}/mapa`}
                    className="inline-flex items-center gap-2 rounded-lg border border-indigo-200 bg-white px-4 py-2 text-sm font-semibold text-indigo-700 shadow-sm hover:bg-indigo-50"
                  >
                    <Grid3x3 className="h-4 w-4" />
                    Mapa por assunto
                  </Link>
                  <Link
                    href={`/professor/turma/${turma.data.id}/atividades`}
                    className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-700"
                  >
                    <Trophy className="h-4 w-4" />
                    Atividades coletivas
                  </Link>
                </div>
              </div>

              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <Resumo titulo="Alunos" valor={turma.data.resumo.totalAlunos} />
                <Resumo
                  titulo="Nível médio"
                  valor={turma.data.resumo.nivelMedio ?? "—"}
                />
                <Resumo titulo="Em risco" valor={turma.data.resumo.emRisco} destaque="text-red-600" />
                <Resumo titulo="Em atenção" valor={turma.data.resumo.atencao} destaque="text-amber-600" />
              </div>

              {panorama.data && (
                <Card className="shadow-md border-indigo-200 bg-indigo-50/30">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-base">
                      <BellRing className="h-5 w-5 text-indigo-600" />
                      O que pede atenção agora
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-3 text-sm">
                    {panorama.data.alertas.length === 0 ? (
                      <p className="text-slate-600">
                        Nada pede atenção imediata na turma. Isso não quer dizer que todos estão bem: veja a
                        situação de cada aluno abaixo.
                      </p>
                    ) : (
                      panorama.data.alertas.map((alerta) => (
                        <div key={alerta.titulo + alerta.texto}>
                          <p className="font-semibold text-slate-900">{alerta.titulo}</p>
                          <p className="text-slate-700">{alerta.texto}</p>
                        </div>
                      ))
                    )}
                  </CardContent>
                </Card>
              )}

              {precisamAtencao.length > 0 && (
                <Card className="shadow-md border-amber-200 bg-amber-50/40">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-base">
                      <AlertTriangle className="h-5 w-5 text-amber-600" />
                      Precisam de atenção
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-2">
                    {precisamAtencao.map((aluno) => (
                      <div
                        key={aluno.id}
                        className="flex flex-wrap items-center justify-between gap-2 text-sm"
                      >
                        <span className="font-semibold text-slate-800">{aluno.nome}</span>
                        <span className="text-slate-600">{motivo(aluno)}</span>
                      </div>
                    ))}
                  </CardContent>
                </Card>
              )}

              <Card className="shadow-md border-slate-200/80">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-base">
                    <BarChart3 className="h-5 w-5 text-indigo-600" />
                    Alunos da turma
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {alunos.length === 0 ? (
                    <p className="text-sm text-slate-500">
                      Nenhum aluno nesta turma ainda.
                    </p>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full min-w-[900px] text-sm">
                        <thead>
                          <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-500">
                            <th className="py-2 pr-4 font-semibold">Aluno</th>
                            <th className="py-2 pr-4 font-semibold">Nível</th>
                            <th className="py-2 pr-4 font-semibold">Acertos / erros</th>
                            <th className="py-2 pr-4 font-semibold">Taxa</th>
                            <th className="py-2 pr-4 font-semibold">Situação</th>
                            <th className="py-2 pr-4 font-semibold">Evolução 30 dias</th>
                            <th className="py-2 pr-4 font-semibold">Revisões vencidas</th>
                            <th className="py-2 pr-4 font-semibold">Erra mais em</th>
                            <th className="py-2 font-semibold">Última atividade</th>
                          </tr>
                        </thead>
                        <tbody>
                          {alunos.map((aluno) => (
                            <tr key={aluno.id} className="border-b border-slate-100 last:border-0">
                              <td className="py-3 pr-4">
                                <Link
                                  href={`/professor/turma/${turma.data.id}/aluno/${aluno.id}`}
                                  className="font-semibold text-indigo-700 hover:underline"
                                >
                                  {aluno.nome}
                                </Link>
                                {aluno.matricula && (
                                  <p className="text-xs text-slate-500">{aluno.matricula}</p>
                                )}
                              </td>
                              <td className="py-3 pr-4">
                                <div className="flex items-center gap-2">
                                  <Progress value={aluno.nivelAtual * 10} className="h-2 w-20" />
                                  <span className="text-slate-700">{aluno.nivelAtual}/10</span>
                                </div>
                              </td>
                              <td className="py-3 pr-4 text-slate-700">
                                {aluno.acertos} / {aluno.erros}
                              </td>
                              <td className="py-3 pr-4 text-slate-700">
                                {aluno.taxaAcerto === null ? "—" : `${aluno.taxaAcerto}%`}
                              </td>
                              <td className="py-3 pr-4">
                                <Badge variant={SITUACAO[aluno.situacao].variante}>
                                  {SITUACAO[aluno.situacao].rotulo}
                                </Badge>
                              </td>
                              <td className="py-3 pr-4 tabular-nums text-slate-700">
                                {indicadores.get(aluno.id)?.evolucao30d == null
                                  ? "—"
                                  : variacao.format(indicadores.get(aluno.id)!.evolucao30d!)}
                              </td>
                              <td className="py-3 pr-4 tabular-nums text-slate-700">
                                {indicadores.get(aluno.id)?.revisoesVencidas ?? "—"}
                              </td>
                              <td className="py-3 pr-4 text-slate-700 first-letter:uppercase">
                                {(indicadores.get(aluno.id)?.erraMaisEm ?? []).join(", ") || "—"}
                              </td>
                              <td className="py-3 text-slate-600">
                                {ultimaAtividade(aluno.ultimaAtividade)}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </CardContent>
              </Card>

              {panorama.data && (
                <Card className="shadow-md border-slate-200/80">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-base">
                      <TrendingUp className="h-5 w-5 text-indigo-600" />
                      Tendência do nível médio da turma
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-3 text-sm">
                    {tendencia.length === 0 ? (
                      <p className="text-slate-500">Ainda não há respostas suficientes para mostrar a tendência.</p>
                    ) : (
                      <>
                        <div className="overflow-x-auto">
                          <table className="w-full min-w-[420px]">
                            <thead>
                              <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                                <th className="py-2 pr-4 text-left font-semibold">Semana de</th>
                                {panorama.data.tendencia.map((ponto) => (
                                  <th key={ponto.dia} className="py-2 pr-4 text-right font-semibold">
                                    {diaCurto(ponto.dia)}
                                  </th>
                                ))}
                              </tr>
                            </thead>
                            <tbody>
                              <tr>
                                <td className="py-3 pr-4 font-semibold text-slate-800">Nível médio</td>
                                {panorama.data.tendencia.map((ponto) => (
                                  <td key={ponto.dia} className="py-3 pr-4 text-right font-bold tabular-nums text-slate-900">
                                    {ponto.nivelMedio === null ? "—" : decimal.format(ponto.nivelMedio)}
                                  </td>
                                ))}
                              </tr>
                            </tbody>
                          </table>
                        </div>
                        <p className="text-slate-500">
                          Média dos níveis dos alunos nos assuntos que já praticaram, ao fim de cada semana. Um
                          traço quer dizer que ninguém tinha praticado ainda.
                        </p>
                      </>
                    )}
                  </CardContent>
                </Card>
              )}
            </>
          )}
        </main>
      </div>
    </AuthGuard>
  );
}

function Resumo({
  titulo,
  valor,
  destaque = "text-slate-900",
}: {
  titulo: string;
  valor: number | string;
  destaque?: string;
}) {
  return (
    <Card className="shadow-sm border-slate-200/80">
      <CardContent className="pt-6">
        <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{titulo}</p>
        <p className={`text-3xl font-extrabold ${destaque}`}>{valor}</p>
      </CardContent>
    </Card>
  );
}
