"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { ArrowLeft, BookOpen, CalendarDays } from "lucide-react";
import { api } from "@/lib/api";
import { DetalheDoAluno } from "@/lib/types";
import { SITUACAO } from "@/lib/situacao";
import { dataBrasileira } from "@/lib/revisoes";
import { AuthGuard } from "@/components/AuthGuard";
import { GraficoEvolucao } from "@/components/GraficoEvolucao";
import { Navbar } from "@/components/Navbar";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const numero = new Intl.NumberFormat("pt-BR");
const percentual = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 2 });

export default function DetalheDoAlunoPage() {
  const { id, alunoId } = useParams<{ id: string; alunoId: string }>();

  const detalhe = useQuery({
    queryKey: ["professor", "turma", id, "aluno", alunoId],
    queryFn: async () => {
      const { data } = await api.get<DetalheDoAluno>(
        `/api/professor/turmas/${id}/alunos/${alunoId}`
      );
      return data;
    },
    retry: false,
  });

  const status = (detalhe.error as AxiosError | null)?.response?.status;
  const assuntos = [...(detalhe.data?.assuntos ?? [])].sort(
    (a, b) =>
      SITUACAO[a.situacao].ordem - SITUACAO[b.situacao].ordem ||
      a.nome.localeCompare(b.nome, "pt-BR")
  );

  return (
    <AuthGuard allowedRole="PROFESSOR">
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />

        <main className="flex-1 mx-auto w-full max-w-5xl px-4 sm:px-6 py-8 space-y-8">
          <Link
            href={`/professor/turma/${id}`}
            className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-700"
          >
            <ArrowLeft className="h-4 w-4" /> {detalhe.data?.turmaNome ?? "Voltar à turma"}
          </Link>

          {detalhe.isLoading && (
            <div className="space-y-4">
              <Skeleton className="h-10 w-64" />
              <Skeleton className="h-40 w-full" />
              <Skeleton className="h-64 w-full" />
            </div>
          )}

          {detalhe.isError && (
            <p className="text-sm text-red-600">
              {status === 403
                ? "Você não leciona nesta turma."
                : status === 404
                  ? "Aluno não encontrado nesta turma."
                  : "Não foi possível carregar o aluno. Tente novamente."}
            </p>
          )}

          {detalhe.data && (
            <>
              <div className="space-y-2">
                <div className="flex flex-wrap items-center gap-3">
                  <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
                    {detalhe.data.aluno.nome}
                  </h1>
                  <Badge variant={SITUACAO[detalhe.data.aluno.situacao].variante}>
                    {SITUACAO[detalhe.data.aluno.situacao].rotulo}
                  </Badge>
                </div>
                <p className="text-sm text-slate-500">
                  {detalhe.data.aluno.matricula
                    ? `Matrícula ${detalhe.data.aluno.matricula} · `
                    : ""}
                  Nível geral {detalhe.data.aluno.nivelAtual}/10 · {detalhe.data.aluno.acertos}{" "}
                  acertos e {detalhe.data.aluno.erros} erros
                  {detalhe.data.aluno.taxaAcerto !== null &&
                    ` (${detalhe.data.aluno.taxaAcerto}% de acerto)`}
                </p>
              </div>

              <section className="space-y-3" aria-labelledby="titulo-assuntos">
                <h2
                  id="titulo-assuntos"
                  className="flex items-center gap-2 text-xl font-bold text-slate-900"
                >
                  <BookOpen className="h-5 w-5 text-indigo-600" />
                  Assuntos de {detalhe.data.materia}
                </h2>
                {assuntos.length === 0 ? (
                  <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-700">
                    Esta matéria ainda não tem assuntos cadastrados.
                  </p>
                ) : (
                  <ul className="grid gap-3 sm:grid-cols-2">
                    {assuntos.map((assunto) => (
                      <li key={assunto.id}>
                        <Card className="h-full shadow-sm border-slate-200/80">
                          <CardHeader className="p-5 pb-3">
                            <div className="flex items-start justify-between gap-2">
                              <CardTitle className="text-lg first-letter:uppercase">
                                {assunto.nome}
                              </CardTitle>
                              <Badge variant={SITUACAO[assunto.situacao].variante}>
                                {SITUACAO[assunto.situacao].rotulo}
                              </Badge>
                            </div>
                          </CardHeader>
                          <CardContent className="space-y-3 px-5 pb-5 text-sm">
                            <div className="flex items-center gap-2">
                              <Progress value={assunto.nivelAtual * 10} className="h-2 flex-1" />
                              <span className="font-semibold tabular-nums text-slate-800">
                                Nível {assunto.nivelAtual}/10
                              </span>
                            </div>
                            <p className="text-slate-600">
                              {assunto.acertos + assunto.erros === 0
                                ? "Ainda não praticou este assunto."
                                : `${assunto.acertos} acertos e ${assunto.erros} erros (${assunto.taxaAcerto}% de acerto)`}
                              {assunto.errosConsecutivos >= 1 &&
                                ` · ${assunto.errosConsecutivos} erro(s) seguido(s)`}
                            </p>
                          </CardContent>
                        </Card>
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              <section className="space-y-3" aria-labelledby="titulo-evolucao">
                <h2
                  id="titulo-evolucao"
                  className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xl font-bold text-slate-900"
                >
                  Evolução nos últimos 30 dias
                  <span className="inline-flex items-center gap-1.5 text-sm font-normal text-slate-600">
                    <CalendarDays className="h-4 w-4 text-indigo-700" />
                    {dataBrasileira(detalhe.data.evolucao.dataInicial)} a{" "}
                    {dataBrasileira(detalhe.data.evolucao.dataFinal)}
                  </span>
                </h2>

                <dl className="grid grid-cols-2 gap-3 lg:grid-cols-4">
                  {[
                    ["Sessões iniciadas", numero.format(detalhe.data.evolucao.resumo.sessoesIniciadas)],
                    ["Tentativas respondidas", numero.format(detalhe.data.evolucao.resumo.tentativas)],
                    ["Questões únicas", numero.format(detalhe.data.evolucao.resumo.questoesUnicas)],
                    [
                      "Acerto na primeira tentativa",
                      detalhe.data.evolucao.resumo.taxaAcertoPrimeiraTentativa === null
                        ? "Sem dados"
                        : percentual.format(
                            detalhe.data.evolucao.resumo.taxaAcertoPrimeiraTentativa / 100
                          ),
                    ],
                  ].map(([rotulo, valor]) => (
                    <div
                      key={rotulo}
                      className="flex min-w-0 flex-col justify-between rounded-xl bg-indigo-50/70 p-3 sm:p-4"
                    >
                      <dt className="text-sm font-semibold text-indigo-900">{rotulo}</dt>
                      <dd className="mt-1 text-2xl font-extrabold tabular-nums text-slate-900">
                        {valor}
                      </dd>
                    </div>
                  ))}
                </dl>

                <GraficoEvolucao dias={detalhe.data.evolucao.evolucaoDiaria} />
              </section>
            </>
          )}
        </main>
      </div>
    </AuthGuard>
  );
}
