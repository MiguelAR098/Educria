"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { CriarTurmaPayload, LancarPontosPayload } from "@/lib/types";
import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  FolderPlus,
  Award,
  UserPlus,
  Loader2,
  CheckCircle2,
  ShieldCheck,
} from "lucide-react";
import { toast } from "sonner";
import { AxiosError } from "axios";

export default function AdminDashboardPage() {
  // Estado Criar Turma
  const [nomeTurma, setNomeTurma] = useState("");
  const [codigoTurma, setCodigoTurma] = useState("");
  const [anoLetivo, setAnoLetivo] = useState(
    String(new Date().getFullYear())
  );
  const [turmaCriadaMsg, setTurmaCriadaMsg] = useState<string | null>(null);

  // Estado Lançar Pontos
  const [turmaId, setTurmaId] = useState("");
  const [pontos, setPontos] = useState("");
  const [pontosLancadosMsg, setPontosLancadosMsg] = useState<string | null>(
    null
  );

  // Estado Vincular Aluno
  const [vincularTurmaId, setVincularTurmaId] = useState("");
  const [vincularAlunoId, setVincularAlunoId] = useState("");
  const [alunoVinculadoMsg, setAlunoVinculadoMsg] = useState<string | null>(
    null
  );

  const criarTurmaMutation = useMutation({
    mutationFn: async (payload: CriarTurmaPayload) => {
      const { data } = await api.post("/api/admin/turmas", payload);
      return data;
    },
    onSuccess: (data) => {
      const idInfo = data?.id ? ` (ID: ${data.id})` : "";
      const successText = `Turma "${nomeTurma}" criada com sucesso${idInfo}!`;
      setTurmaCriadaMsg(successText);
      toast.success(successText);
      setNomeTurma("");
      setCodigoTurma("");
    },
    onError: (error: AxiosError<{ message?: string; error?: string }>) => {
      const msg =
        error.response?.data?.message ||
        error.response?.data?.error ||
        "Erro ao criar turma. Verifique os dados.";
      toast.error(msg);
    },
  });

  const lancarPontosMutation = useMutation({
    mutationFn: async ({ turmaId, pontos }: LancarPontosPayload) => {
      const { data } = await api.post(`/api/admin/turmas/${turmaId}/pontos`, {
        pontos,
      });
      return data;
    },
    onSuccess: () => {
      const successText = `${pontos} pontos adicionados à turma #${turmaId}!`;
      setPontosLancadosMsg(successText);
      toast.success(successText);
      setPontos("");
    },
    onError: (error: AxiosError<{ message?: string; error?: string }>) => {
      const msg =
        error.response?.data?.message ||
        error.response?.data?.error ||
        "Erro ao lançar pontos. Verifique o ID da turma.";
      toast.error(msg);
    },
  });

  const vincularAlunoMutation = useMutation({
    mutationFn: async ({
      turmaId,
      alunoId,
    }: {
      turmaId: string;
      alunoId: string;
    }) => {
      const { data } = await api.post(
        `/api/admin/turmas/${turmaId}/alunos/${alunoId}`
      );
      return data;
    },
    onSuccess: () => {
      const successText = `Aluno #${vincularAlunoId} vinculado à turma #${vincularTurmaId}!`;
      setAlunoVinculadoMsg(successText);
      toast.success(successText);
      setVincularAlunoId("");
    },
    onError: (error: AxiosError<{ message?: string; error?: string }>) => {
      const msg =
        error.response?.data?.message ||
        error.response?.data?.error ||
        "Erro ao vincular aluno. Verifique os IDs.";
      toast.error(msg);
    },
  });

  const handleCriarTurma = (e: React.FormEvent) => {
    e.preventDefault();
    setTurmaCriadaMsg(null);
    if (!nomeTurma.trim() || !codigoTurma.trim() || !anoLetivo.trim()) {
      toast.warning("Preencha todos os campos da turma.");
      return;
    }
    criarTurmaMutation.mutate({
      nome: nomeTurma.trim(),
      codigo: codigoTurma.trim(),
      anoLetivo: Number(anoLetivo),
    });
  };

  const handleLancarPontos = (e: React.FormEvent) => {
    e.preventDefault();
    setPontosLancadosMsg(null);
    if (!turmaId.trim() || !pontos.trim() || Number(pontos) <= 0) {
      toast.warning("Informe um ID de turma válido e uma pontuação positiva.");
      return;
    }
    lancarPontosMutation.mutate({
      turmaId: turmaId.trim(),
      pontos: Number(pontos),
    });
  };

  const handleVincularAluno = (e: React.FormEvent) => {
    e.preventDefault();
    setAlunoVinculadoMsg(null);
    if (!vincularTurmaId.trim() || !vincularAlunoId.trim()) {
      toast.warning("Informe o ID da turma e o ID do aluno.");
      return;
    }
    vincularAlunoMutation.mutate({
      turmaId: vincularTurmaId.trim(),
      alunoId: vincularAlunoId.trim(),
    });
  };

  return (
    <AuthGuard allowedRole="ADMIN">
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />

        <main className="flex-1 mx-auto w-full max-w-5xl px-4 sm:px-6 py-8 space-y-8">
          <div className="space-y-1">
            <Badge variant="secondary" className="gap-1">
              <ShieldCheck className="h-3.5 w-3.5" />
              Painel Administrativo
            </Badge>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
              Gestão de Turmas e Pontuação
            </h1>
            <p className="text-sm text-slate-500">
              Crie novas turmas do Ensino Fundamental II, vincule estudantes e
              distribua pontos coletivos.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Card Criar Turma */}
            <Card className="shadow-md border-slate-200/80">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-indigo-100 text-indigo-600">
                    <FolderPlus className="h-6 w-6" />
                  </div>
                  <div>
                    <CardTitle>Criar Nova Turma</CardTitle>
                    <CardDescription>
                      Cadastre uma turma no sistema EDUCRIA
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>

              <form onSubmit={handleCriarTurma}>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="nomeTurma">Nome da Turma</Label>
                    <Input
                      id="nomeTurma"
                      placeholder="Ex: 7º Ano A - Manhã"
                      value={nomeTurma}
                      onChange={(e) => setNomeTurma(e.target.value)}
                      disabled={criarTurmaMutation.isPending}
                      required
                    />
                  </div>

                  <div className="grid gap-4 sm:grid-cols-2">
                    <div className="space-y-2">
                      <Label htmlFor="codigoTurma">Código</Label>
                      <Input
                        id="codigoTurma"
                        placeholder="Ex: 7A-2026"
                        value={codigoTurma}
                        onChange={(e) => setCodigoTurma(e.target.value)}
                        disabled={criarTurmaMutation.isPending}
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="anoLetivo">Ano Letivo</Label>
                      <Input
                        id="anoLetivo"
                        type="number"
                        placeholder="2026"
                        value={anoLetivo}
                        onChange={(e) => setAnoLetivo(e.target.value)}
                        disabled={criarTurmaMutation.isPending}
                        required
                      />
                    </div>
                  </div>

                  {turmaCriadaMsg && (
                    <div className="flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs font-medium text-emerald-800">
                      <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
                      <span>{turmaCriadaMsg}</span>
                    </div>
                  )}
                </CardContent>

                <CardFooter>
                  <Button
                    type="submit"
                    className="w-full gap-2"
                    disabled={criarTurmaMutation.isPending}
                  >
                    {criarTurmaMutation.isPending ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Salvando turma...
                      </>
                    ) : (
                      <>
                        <FolderPlus className="h-4 w-4" />
                        Criar Turma
                      </>
                    )}
                  </Button>
                </CardFooter>
              </form>
            </Card>

            {/* Card Lançar Pontos */}
            <Card className="shadow-md border-slate-200/80">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600">
                    <Award className="h-6 w-6" />
                  </div>
                  <div>
                    <CardTitle>Lançar Pontos Coletivos</CardTitle>
                    <CardDescription>
                      Recompense o esforço conjunto de uma turma
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>

              <form onSubmit={handleLancarPontos}>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="turmaId">ID da Turma</Label>
                    <Input
                      id="turmaId"
                      placeholder="Ex: 1"
                      value={turmaId}
                      onChange={(e) => setTurmaId(e.target.value)}
                      disabled={lancarPontosMutation.isPending}
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="pontos">Quantidade de Pontos</Label>
                    <Input
                      id="pontos"
                      type="number"
                      min={1}
                      placeholder="Ex: 150"
                      value={pontos}
                      onChange={(e) => setPontos(e.target.value)}
                      disabled={lancarPontosMutation.isPending}
                      required
                    />
                  </div>

                  {pontosLancadosMsg && (
                    <div className="flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs font-medium text-emerald-800">
                      <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
                      <span>{pontosLancadosMsg}</span>
                    </div>
                  )}
                </CardContent>

                <CardFooter>
                  <Button
                    type="submit"
                    variant="success"
                    className="w-full gap-2"
                    disabled={lancarPontosMutation.isPending}
                  >
                    {lancarPontosMutation.isPending ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Lançando pontos...
                      </>
                    ) : (
                      <>
                        <Award className="h-4 w-4" />
                        Lançar Pontos
                      </>
                    )}
                  </Button>
                </CardFooter>
              </form>
            </Card>

            {/* Card Vincular Aluno à Turma */}
            <Card className="shadow-md border-slate-200/80 md:col-span-2">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-violet-100 text-violet-600">
                    <UserPlus className="h-6 w-6" />
                  </div>
                  <div>
                    <CardTitle>Vincular Aluno à Turma</CardTitle>
                    <CardDescription>
                      Associe um estudante cadastrado a uma turma existente
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>

              <form onSubmit={handleVincularAluno}>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="vincularTurmaId">ID da Turma</Label>
                      <Input
                        id="vincularTurmaId"
                        type="number"
                        min={1}
                        placeholder="Ex: 1"
                        value={vincularTurmaId}
                        onChange={(e) => setVincularTurmaId(e.target.value)}
                        disabled={vincularAlunoMutation.isPending}
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="vincularAlunoId">ID do Aluno</Label>
                      <Input
                        id="vincularAlunoId"
                        type="number"
                        min={1}
                        placeholder="Ex: 2"
                        value={vincularAlunoId}
                        onChange={(e) => setVincularAlunoId(e.target.value)}
                        disabled={vincularAlunoMutation.isPending}
                        required
                      />
                    </div>
                  </div>

                  {alunoVinculadoMsg && (
                    <div className="flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs font-medium text-emerald-800">
                      <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
                      <span>{alunoVinculadoMsg}</span>
                    </div>
                  )}
                </CardContent>

                <CardFooter>
                  <Button
                    type="submit"
                    variant="secondary"
                    className="w-full sm:w-auto gap-2"
                    disabled={vincularAlunoMutation.isPending}
                  >
                    {vincularAlunoMutation.isPending ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Vinculando...
                      </>
                    ) : (
                      <>
                        <UserPlus className="h-4 w-4" />
                        Vincular Aluno
                      </>
                    )}
                  </Button>
                </CardFooter>
              </form>
            </Card>
          </div>
        </main>
      </div>
    </AuthGuard>
  );
}
