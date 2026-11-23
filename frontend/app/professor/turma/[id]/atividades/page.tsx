"use client";

import { useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { toast } from "sonner";
import { ArrowLeft, Loader2, Trophy } from "lucide-react";
import { api } from "@/lib/api";
import { ErroApi, mensagemDeErro } from "@/lib/erros";
import { nomeDoTipo, PONTOS_ATIVIDADE, TIPOS_ATIVIDADE } from "@/lib/atividades";
import {
  AtividadeColetiva,
  HistoricoDeAtividades,
  LancarAtividadePayload,
  TipoAtividadeColetiva,
} from "@/lib/types";
import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const selectClasses =
  "w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus-visible:outline focus-visible:outline-2 focus-visible:outline-indigo-600";

function quando(iso: string): string {
  return new Date(iso).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export default function AtividadesColetivasPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const chave = ["professor", "turma", id, "atividades"];

  const [titulo, setTitulo] = useState("");
  const [tipo, setTipo] = useState<TipoAtividadeColetiva>("ROBOTICA");
  const [pontos, setPontos] = useState(40);
  const [resumo, setResumo] = useState("");

  const historico = useQuery({
    queryKey: chave,
    queryFn: async () => {
      const { data } = await api.get<HistoricoDeAtividades>(`/api/professor/turmas/${id}/atividades`);
      return data;
    },
    retry: false,
  });

  const lancar = useMutation({
    mutationFn: async (payload: LancarAtividadePayload) => {
      const { data } = await api.post<AtividadeColetiva>(`/api/professor/turmas/${id}/atividades`, payload);
      return data;
    },
    onSuccess: (atividade) => {
      toast.success(`Atividade registrada. A turma recebeu ${atividade.pontos} pontos coletivos.`);
      setTitulo("");
      setResumo("");
      queryClient.invalidateQueries({ queryKey: chave });
    },
    onError: (error: AxiosError<ErroApi>) => toast.error(mensagemDeErro(error, "Não foi possível lançar a atividade. Tente novamente.")),
  });

  const proibido = (historico.error as AxiosError | null)?.response?.status === 403;
  const descricaoDoTipo = TIPOS_ATIVIDADE.find((t) => t.id === tipo)?.descricao;

  function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    if (!titulo.trim()) {
      toast.error("Dê um título curto para a atividade.");
      return;
    }
    lancar.mutate({ tipo, titulo: titulo.trim(), pontos, resumo: resumo.trim() || undefined });
  }

  return (
    <AuthGuard allowedRole="PROFESSOR">
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />

        <main className="flex-1 mx-auto w-full max-w-5xl px-4 sm:px-6 py-8 space-y-8">
          <Link
            href={`/professor/turma/${id}`}
            className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-700"
          >
            <ArrowLeft className="h-4 w-4" /> Voltar à turma
          </Link>

          <div className="flex flex-wrap items-end justify-between gap-4">
            <div className="space-y-1">
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
                Atividades coletivas
              </h1>
              <p className="max-w-xl text-sm text-slate-500">
                Registre o que a turma fez junto e some pontos ao total coletivo.
              </p>
            </div>
            {historico.data && (
              <div className="flex items-center gap-3 rounded-xl bg-white px-5 py-3 shadow-sm border border-slate-200/80">
                <Trophy className="h-6 w-6 text-amber-500" />
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Pontos coletivos da turma
                  </p>
                  <p className="text-2xl font-extrabold tabular-nums text-slate-900">
                    {historico.data.pontosTotais}
                  </p>
                </div>
              </div>
            )}
          </div>

          <Card className="shadow-sm border-slate-200/80">
            <CardHeader className="pb-2">
              <CardTitle className="text-base">Duas moedas separadas</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 text-sm text-slate-700">
              <p>
                O <strong>ponto individual</strong> mede o que cada aluno sustenta em cada assunto. É privado: só o
                próprio aluno e os professores da turma veem. O <strong>ponto coletivo</strong> vem de uma atividade
                que a turma fez junto, é lançado por um professor e aparece para a turma.
              </p>
              <p className="text-slate-500">
                As duas nunca se convertem uma na outra. Um aluno que foi mal numa prova não derruba os pontos
                coletivos da sala, e uma turma que ganhou pontos numa atividade não empurra o nível individual de
                ninguém para cima.
              </p>
            </CardContent>
          </Card>

          {historico.isLoading && <Skeleton className="h-64 w-full" />}

          {historico.isError && (
            <p className="text-sm text-red-600">
              {proibido
                ? "Você não leciona nesta turma."
                : "Não foi possível carregar as atividades. Tente novamente."}
            </p>
          )}

          {historico.data && (
            <>
              <Card className="shadow-md border-slate-200/80">
                <CardHeader>
                  <CardTitle className="text-base">Lançar atividade</CardTitle>
                </CardHeader>
                <CardContent>
                  <form onSubmit={enviar} className="space-y-4">
                    <div className="grid gap-4 sm:grid-cols-2">
                      <div className="space-y-2">
                        <Label htmlFor="titulo">Título</Label>
                        <Input
                          id="titulo"
                          value={titulo}
                          maxLength={120}
                          placeholder="Ex.: Estação ESP32 sobre porcentagem"
                          onChange={(e) => setTitulo(e.target.value)}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="tipo">Tipo de atividade</Label>
                        <select
                          id="tipo"
                          value={tipo}
                          onChange={(e) => setTipo(e.target.value as TipoAtividadeColetiva)}
                          className={selectClasses}
                        >
                          {TIPOS_ATIVIDADE.map((t) => (
                            <option key={t.id} value={t.id}>
                              {t.nome}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="pontos">Pontos para a turma</Label>
                        <select
                          id="pontos"
                          value={pontos}
                          onChange={(e) => setPontos(Number(e.target.value))}
                          className={selectClasses}
                        >
                          {PONTOS_ATIVIDADE.map((p) => (
                            <option key={p.valor} value={p.valor}>
                              {p.rotulo}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="resumo">Como foi (opcional)</Label>
                        <Input
                          id="resumo"
                          value={resumo}
                          maxLength={300}
                          placeholder="Uma linha sobre o resultado"
                          onChange={(e) => setResumo(e.target.value)}
                        />
                      </div>
                    </div>
                    <p className="text-sm text-slate-500">{descricaoDoTipo}</p>
                    <Button type="submit" disabled={lancar.isPending}>
                      {lancar.isPending ? (
                        <>
                          <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Lançando…
                        </>
                      ) : (
                        "Lançar atividade"
                      )}
                    </Button>
                  </form>
                </CardContent>
              </Card>

              <Card className="shadow-md border-slate-200/80">
                <CardHeader>
                  <CardTitle className="text-base">Histórico da turma</CardTitle>
                </CardHeader>
                <CardContent>
                  {historico.data.atividades.length === 0 ? (
                    <p className="text-sm text-slate-500">
                      Nenhuma atividade lançada para esta turma ainda.
                    </p>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full min-w-[640px] text-sm">
                        <thead>
                          <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-500">
                            <th className="py-2 pr-4 font-semibold">Quando</th>
                            <th className="py-2 pr-4 font-semibold">Atividade</th>
                            <th className="py-2 pr-4 font-semibold">Tipo</th>
                            <th className="py-2 pr-4 font-semibold">Lançada por</th>
                            <th className="py-2 pr-4 font-semibold">Pontos</th>
                            <th className="py-2 font-semibold">Resumo</th>
                          </tr>
                        </thead>
                        <tbody>
                          {historico.data.atividades.map((a) => (
                            <tr key={a.id} className="border-b border-slate-100 last:border-0 align-top">
                              <td className="py-3 pr-4 text-slate-600 whitespace-nowrap">{quando(a.criadoEm)}</td>
                              <td className="py-3 pr-4 font-semibold text-slate-800">{a.titulo}</td>
                              <td className="py-3 pr-4 text-slate-700">{nomeDoTipo(a.tipo)}</td>
                              <td className="py-3 pr-4 text-slate-700">{a.lancadaPor ?? "—"}</td>
                              <td className="py-3 pr-4 font-semibold tabular-nums text-slate-800">+{a.pontos}</td>
                              <td className="py-3 text-slate-600">{a.resumo ?? "—"}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </CardContent>
              </Card>
            </>
          )}
        </main>
      </div>
    </AuthGuard>
  );
}
