"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { ArrowLeft, Grid3x3 } from "lucide-react";
import { api } from "@/lib/api";
import { classeDoNivel } from "@/lib/situacao";
import { MapaDaTurma } from "@/lib/types";
import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const decimal = new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 });

export default function MapaDaTurmaPage() {
  const { id } = useParams<{ id: string }>();

  const mapa = useQuery({
    queryKey: ["professor", "turma", id, "mapa"],
    queryFn: async () => {
      const { data } = await api.get<MapaDaTurma>(`/api/professor/turmas/${id}/mapa`);
      return data;
    },
    retry: false,
  });

  const proibido = (mapa.error as AxiosError | null)?.response?.status === 403;

  return (
    <AuthGuard allowedRole="PROFESSOR">
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />

        <main className="flex-1 mx-auto w-full max-w-5xl px-4 sm:px-6 py-8 space-y-8">
          <Link
            href={`/professor/turma/${id}`}
            className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-700"
          >
            <ArrowLeft className="h-4 w-4" /> {mapa.data?.turmaNome ?? "Voltar à turma"}
          </Link>

          {mapa.isLoading && <Skeleton className="h-72 w-full" />}

          {mapa.isError && (
            <p className="text-sm text-red-600">
              {proibido
                ? "Você não leciona nesta turma."
                : "Não foi possível carregar o mapa. Tente novamente."}
            </p>
          )}

          {mapa.data && (
            <>
              <div className="space-y-1">
                <Badge variant="secondary" className="gap-1">
                  <Grid3x3 className="h-3.5 w-3.5" />
                  {mapa.data.materia}
                </Badge>
                <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
                  Mapa por assunto
                </h1>
                <p className="max-w-2xl text-sm text-slate-500">
                  Nível de cada aluno em cada assunto. Um traço quer dizer que o aluno ainda não praticou o
                  assunto, então o nível 1 é só o ponto de partida e não entra nas médias.
                </p>
              </div>

              {mapa.data.assuntos.length === 0 ? (
                <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-700">
                  Esta matéria ainda não tem assuntos cadastrados, então não há o que mapear.
                </p>
              ) : (
                <>
                  <Card className="shadow-md border-slate-200/80">
                    <CardHeader>
                      <CardTitle className="text-base">Assuntos mais frágeis da turma</CardTitle>
                    </CardHeader>
                    <CardContent>
                      {mapa.data.maisFrageis.length === 0 ? (
                        <p className="text-sm text-slate-500">Ainda não há prática suficiente para comparar assuntos.</p>
                      ) : (
                        <div className="overflow-x-auto">
                          <table className="w-full min-w-[480px] text-sm">
                            <thead>
                              <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-500">
                                <th className="py-2 pr-4 font-semibold">Assunto</th>
                                <th className="py-2 pr-4 font-semibold">Nível médio</th>
                                <th className="py-2 font-semibold">No nível 2 ou abaixo</th>
                              </tr>
                            </thead>
                            <tbody>
                              {mapa.data.maisFrageis.map((assunto) => (
                                <tr key={assunto.assuntoId} className="border-b border-slate-100 last:border-0">
                                  <td className="py-3 pr-4 font-semibold text-slate-800 first-letter:uppercase">
                                    {assunto.nome}
                                  </td>
                                  <td className="py-3 pr-4 tabular-nums text-slate-700">
                                    {assunto.nivelMedio === null ? "—" : decimal.format(assunto.nivelMedio)}
                                  </td>
                                  <td className="py-3 text-slate-700">
                                    {assunto.noNivelBaixo} de {assunto.alunosQuePraticaram} que praticaram
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </CardContent>
                  </Card>

                  <Card className="shadow-md border-slate-200/80">
                    <CardHeader>
                      <CardTitle className="text-base">Nível por aluno e por assunto</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      {mapa.data.alunos.length === 0 ? (
                        <p className="text-sm text-slate-500">Nenhum aluno nesta turma ainda.</p>
                      ) : (
                        <div className="overflow-x-auto">
                          <table className="w-full border-separate border-spacing-1 text-sm">
                            <thead>
                              <tr>
                                <th className="sticky left-0 bg-white py-2 pr-3 text-left text-xs uppercase tracking-wide text-slate-500">
                                  Aluno
                                </th>
                                {mapa.data.assuntos.map((assunto) => (
                                  <th
                                    key={assunto.id}
                                    className="min-w-[5.5rem] px-1 py-2 text-xs font-semibold text-slate-600 first-letter:uppercase"
                                  >
                                    {assunto.nome}
                                  </th>
                                ))}
                              </tr>
                            </thead>
                            <tbody>
                              {mapa.data.alunos.map((aluno) => (
                                <tr key={aluno.alunoId}>
                                  <td className="sticky left-0 bg-white py-1 pr-3 font-medium text-slate-800 whitespace-nowrap">
                                    <Link
                                      href={`/professor/turma/${id}/aluno/${aluno.alunoId}`}
                                      className="text-indigo-700 hover:underline"
                                    >
                                      {aluno.nome}
                                    </Link>
                                  </td>
                                  {aluno.celulas.map((celula, i) => (
                                    <td
                                      key={mapa.data.assuntos[i].id}
                                      title={celula.praticou ? `Nível ${celula.nivel}` : "Ainda não praticou"}
                                      className={`rounded-md px-2 py-2 text-center font-bold tabular-nums ${
                                        celula.praticou ? classeDoNivel(celula.nivel) : "bg-slate-50 text-slate-400"
                                      }`}
                                    >
                                      {celula.praticou ? celula.nivel : "—"}
                                    </td>
                                  ))}
                                </tr>
                              ))}
                              <tr>
                                <td className="sticky left-0 bg-white py-2 pr-3 text-xs font-bold uppercase tracking-wide text-slate-600">
                                  Média da turma
                                </td>
                                {mapa.data.medias.map((media) => (
                                  <td
                                    key={media.assuntoId}
                                    className={`rounded-md px-2 py-2 text-center font-extrabold tabular-nums ${
                                      media.nivelMedio === null
                                        ? "bg-slate-50 text-slate-400"
                                        : classeDoNivel(Math.round(media.nivelMedio))
                                    }`}
                                  >
                                    {media.nivelMedio === null ? "—" : decimal.format(media.nivelMedio)}
                                  </td>
                                ))}
                              </tr>
                            </tbody>
                          </table>
                        </div>
                      )}

                      <p className="flex flex-wrap items-center gap-2 text-xs text-slate-600">
                        Legenda de níveis:
                        {[1, 3, 5, 7, 9].map((nivel) => (
                          <span key={nivel} className={`rounded px-2 py-0.5 font-semibold ${classeDoNivel(nivel)}`}>
                            {nivel === 1 ? "1 a 2" : nivel === 9 ? "9 a 10" : `${nivel} a ${nivel + 1}`}
                          </span>
                        ))}
                      </p>
                    </CardContent>
                  </Card>
                </>
              )}
            </>
          )}
        </main>
      </div>
    </AuthGuard>
  );
}
