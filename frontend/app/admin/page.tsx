"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
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
import { Separator } from "@/components/ui/separator";
import { toast } from "sonner";
import { AxiosError } from "axios";

// ── tipos locais ─────────────────────────────────────────────────────────────

interface TurmaResumo {
  id: number;
  nome: string;
  codigo: string;
  anoLetivo: number;
  pontosTotais: number;
  totalAlunos: number;
}

interface AlunoResumo {
  id: number;
  nome: string;
  matricula?: string;
}

// ── helpers ──────────────────────────────────────────────────────────────────

function apiError(err: unknown, fallback: string): string {
  const e = err as AxiosError<{ message?: string; error?: string }>;
  return e.response?.data?.message ?? e.response?.data?.error ?? fallback;
}

// ── componente principal ──────────────────────────────────────────────────────

export default function AdminDashboardPage() {
  const qc = useQueryClient();

  // ── Criar Turma
  const [nomeTurma, setNomeTurma] = useState("");
  const [codigoTurma, setCodigoTurma] = useState("");
  const [anoLetivo, setAnoLetivo] = useState(String(new Date().getFullYear()));

  // ── Lançar Pontos
  const [turmaIdPontos, setTurmaIdPontos] = useState("");
  const [qtdPontos, setQtdPontos] = useState("");

  // ── Vincular / Desvincular Aluno
  const [turmaIdVincular, setTurmaIdVincular] = useState("");
  const [alunoIdVincular, setAlunoIdVincular] = useState("");
  const [turmaIdDesvincular, setTurmaIdDesvincular] = useState("");
  const [alunoIdDesvincular, setAlunoIdDesvincular] = useState("");

  // ── Query: lista de turmas ────────────────────────────────────────────────
  const { data: turmas = [], isLoading: carregandoTurmas } = useQuery<TurmaResumo[]>({
    queryKey: ["admin", "turmas"],
    queryFn: async () => {
      const { data } = await api.get("/api/admin/turmas");
      return data;
    },
  });

  // ── Query: alunos da turma selecionada (desvincular) ─────────────────────
  const { data: alunosDaTurma = [] } = useQuery<AlunoResumo[]>({
    queryKey: ["admin", "turma-alunos", turmaIdDesvincular],
    queryFn: async () => {
      const { data } = await api.get(`/api/admin/turmas/${turmaIdDesvincular}/alunos`);
      return data;
    },
    enabled: !!turmaIdDesvincular,
  });

  // ── Mutações ──────────────────────────────────────────────────────────────

  const criarTurma = useMutation({
    mutationFn: async () => {
      const { data } = await api.post("/api/admin/turmas", {
        nome: nomeTurma.trim(),
        codigo: codigoTurma.trim(),
        anoLetivo: Number(anoLetivo),
      });
      return data;
    },
    onSuccess: (data) => {
      toast.success(`Turma "${data.nome}" criada (ID: ${data.id})`);
      qc.invalidateQueries({ queryKey: ["admin", "turmas"] });
      setNomeTurma(""); setCodigoTurma("");
    },
    onError: (err) => toast.error(apiError(err, "Erro ao criar turma.")),
  });

  const lancarPontos = useMutation({
    mutationFn: async () => {
      await api.post(`/api/admin/turmas/${turmaIdPontos}/pontos`, {
        pontos: Number(qtdPontos),
      });
    },
    onSuccess: () => {
      const turma = turmas.find((t) => String(t.id) === turmaIdPontos);
      toast.success(`${qtdPontos} pts adicionados à ${turma?.nome ?? "turma"}`);
      qc.invalidateQueries({ queryKey: ["admin", "turmas"] });
      setQtdPontos("");
    },
    onError: (err) => toast.error(apiError(err, "Erro ao lançar pontos.")),
  });

  const vincularAluno = useMutation({
    mutationFn: async () => {
      await api.post(`/api/admin/turmas/${turmaIdVincular}/alunos/${alunoIdVincular}`);
    },
    onSuccess: () => {
      const turma = turmas.find((t) => String(t.id) === turmaIdVincular);
      toast.success(`Aluno #${alunoIdVincular} vinculado à ${turma?.nome ?? "turma"}`);
      qc.invalidateQueries({ queryKey: ["admin", "turmas"] });
      qc.invalidateQueries({ queryKey: ["admin", "turma-alunos"] });
      setAlunoIdVincular("");
    },
    onError: (err) => toast.error(apiError(err, "Erro ao vincular aluno.")),
  });

  const desvincularAluno = useMutation({
    mutationFn: async () => {
      await api.delete(`/api/admin/turmas/${turmaIdDesvincular}/alunos/${alunoIdDesvincular}`);
    },
    onSuccess: () => {
      toast.success(`Aluno desvinculado com sucesso.`);
      qc.invalidateQueries({ queryKey: ["admin", "turmas"] });
      qc.invalidateQueries({ queryKey: ["admin", "turma-alunos", turmaIdDesvincular] });
      setAlunoIdDesvincular("");
    },
    onError: (err) => toast.error(apiError(err, "Erro ao desvincular aluno.")),
  });

  // ── handlers ──────────────────────────────────────────────────────────────

  const handleCriarTurma = (e: React.FormEvent) => {
    e.preventDefault();
    if (!nomeTurma.trim() || !codigoTurma.trim() || !anoLetivo.trim()) {
      toast.warning("Preencha todos os campos."); return;
    }
    criarTurma.mutate();
  };

  const handleLancarPontos = (e: React.FormEvent) => {
    e.preventDefault();
    if (!turmaIdPontos || Number(qtdPontos) < 1) {
      toast.warning("Selecione uma turma e informe uma pontuação positiva."); return;
    }
    lancarPontos.mutate();
  };

  const handleVincular = (e: React.FormEvent) => {
    e.preventDefault();
    if (!turmaIdVincular || !alunoIdVincular.trim()) {
      toast.warning("Selecione a turma e informe o ID do aluno."); return;
    }
    vincularAluno.mutate();
  };

  const handleDesvincular = (e: React.FormEvent) => {
    e.preventDefault();
    if (!turmaIdDesvincular || !alunoIdDesvincular) {
      toast.warning("Selecione a turma e o aluno."); return;
    }
    desvincularAluno.mutate();
  };

  // ── render ────────────────────────────────────────────────────────────────

  return (
    <AuthGuard allowedRole="ADMIN">
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />

        <main className="flex-1 mx-auto w-full max-w-5xl px-4 sm:px-6 py-10 space-y-10">

          {/* Cabeçalho */}
          <div className="space-y-1">
            <Badge variant="secondary">Painel Administrativo</Badge>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Gestão de Turmas
            </h1>
            <p className="text-sm text-slate-500">
              Crie turmas, vincule alunos e lance pontos coletivos.
            </p>
          </div>

          {/* Lista de turmas existentes */}
          <section className="space-y-3">
            <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wide">
              Turmas cadastradas
            </h2>
            {carregandoTurmas ? (
              <p className="text-sm text-slate-400">Carregando turmas...</p>
            ) : turmas.length === 0 ? (
              <p className="text-sm text-slate-400">Nenhuma turma cadastrada ainda.</p>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {turmas.map((t) => (
                  <div
                    key={t.id}
                    className="rounded-lg border border-slate-200 bg-white px-4 py-3 space-y-1"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-semibold text-slate-800">{t.nome}</span>
                      <span className="text-xs text-slate-400 font-mono">{t.codigo}</span>
                    </div>
                    <div className="flex items-center gap-3 text-xs text-slate-500">
                      <span>{t.anoLetivo}</span>
                      <span>{t.totalAlunos} alunos</span>
                      <span className="font-medium text-slate-700">{t.pontosTotais} pts</span>
                    </div>
                    <span className="text-[10px] font-mono text-slate-300">ID: {t.id}</span>
                  </div>
                ))}
              </div>
            )}
          </section>

          <Separator />

          {/* Formulários */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">

            {/* Criar Turma */}
            <Card className="border-slate-200">
              <CardHeader>
                <CardTitle className="text-base">Criar Turma</CardTitle>
                <CardDescription>Cadastra uma nova turma com pontuação coletiva zerada.</CardDescription>
              </CardHeader>
              <form onSubmit={handleCriarTurma}>
                <CardContent className="space-y-3">
                  <div className="space-y-1">
                    <Label htmlFor="nomeTurma">Nome</Label>
                    <Input id="nomeTurma" placeholder="Ex: 7º Ano A" value={nomeTurma}
                      onChange={(e) => setNomeTurma(e.target.value)}
                      disabled={criarTurma.isPending} required />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="space-y-1">
                      <Label htmlFor="codigo">Código</Label>
                      <Input id="codigo" placeholder="7A-2026" value={codigoTurma}
                        onChange={(e) => setCodigoTurma(e.target.value)}
                        disabled={criarTurma.isPending} required />
                    </div>
                    <div className="space-y-1">
                      <Label htmlFor="anoLetivo">Ano letivo</Label>
                      <Input id="anoLetivo" type="number" value={anoLetivo}
                        onChange={(e) => setAnoLetivo(e.target.value)}
                        disabled={criarTurma.isPending} required />
                    </div>
                  </div>
                </CardContent>
                <CardFooter>
                  <Button type="submit" className="w-full" disabled={criarTurma.isPending}>
                    {criarTurma.isPending ? "Salvando..." : "Criar Turma"}
                  </Button>
                </CardFooter>
              </form>
            </Card>

            {/* Lançar Pontos */}
            <Card className="border-slate-200">
              <CardHeader>
                <CardTitle className="text-base">Lançar Pontos Coletivos</CardTitle>
                <CardDescription>Recompensa o engajamento de uma turma.</CardDescription>
              </CardHeader>
              <form onSubmit={handleLancarPontos}>
                <CardContent className="space-y-3">
                  <div className="space-y-1">
                    <Label htmlFor="turmaPontos">Turma</Label>
                    <select
                      id="turmaPontos"
                      className="w-full h-9 rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-400"
                      value={turmaIdPontos}
                      onChange={(e) => setTurmaIdPontos(e.target.value)}
                      disabled={lancarPontos.isPending}
                    >
                      <option value="">Selecione uma turma</option>
                      {turmas.map((t) => (
                        <option key={t.id} value={String(t.id)}>
                          {t.nome} ({t.pontosTotais} pts)
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="space-y-1">
                    <Label htmlFor="qtdPontos">Quantidade de pontos</Label>
                    <Input id="qtdPontos" type="number" min={1} placeholder="Ex: 150"
                      value={qtdPontos} onChange={(e) => setQtdPontos(e.target.value)}
                      disabled={lancarPontos.isPending} required />
                  </div>
                </CardContent>
                <CardFooter>
                  <Button type="submit" className="w-full" disabled={lancarPontos.isPending}>
                    {lancarPontos.isPending ? "Lançando..." : "Lançar Pontos"}
                  </Button>
                </CardFooter>
              </form>
            </Card>

            {/* Vincular Aluno */}
            <Card className="border-slate-200">
              <CardHeader>
                <CardTitle className="text-base">Vincular Aluno</CardTitle>
                <CardDescription>Associa um aluno a uma turma pelo ID do aluno.</CardDescription>
              </CardHeader>
              <form onSubmit={handleVincular}>
                <CardContent className="space-y-3">
                  <div className="space-y-1">
                    <Label>Turma</Label>
                    <select
                      className="w-full h-9 rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-400"
                      value={turmaIdVincular}
                      onChange={(e) => setTurmaIdVincular(e.target.value)}
                      disabled={vincularAluno.isPending}
                    >
                      <option value="">Selecione uma turma</option>
                      {turmas.map((t) => (
                        <option key={t.id} value={String(t.id)}>{t.nome}</option>
                      ))}
                    </select>
                  </div>
                  <div className="space-y-1">
                    <Label htmlFor="alunoIdVincular">ID do aluno</Label>
                    <Input id="alunoIdVincular" type="number" min={1} placeholder="Ex: 42"
                      value={alunoIdVincular}
                      onChange={(e) => setAlunoIdVincular(e.target.value)}
                      disabled={vincularAluno.isPending} required />
                  </div>
                </CardContent>
                <CardFooter>
                  <Button type="submit" className="w-full" disabled={vincularAluno.isPending}>
                    {vincularAluno.isPending ? "Vinculando..." : "Vincular Aluno"}
                  </Button>
                </CardFooter>
              </form>
            </Card>

            {/* Desvincular Aluno */}
            <Card className="border-slate-200">
              <CardHeader>
                <CardTitle className="text-base">Desvincular Aluno</CardTitle>
                <CardDescription>Remove um aluno de uma turma. Os dados de desempenho são mantidos.</CardDescription>
              </CardHeader>
              <form onSubmit={handleDesvincular}>
                <CardContent className="space-y-3">
                  <div className="space-y-1">
                    <Label>Turma</Label>
                    <select
                      className="w-full h-9 rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-400"
                      value={turmaIdDesvincular}
                      onChange={(e) => { setTurmaIdDesvincular(e.target.value); setAlunoIdDesvincular(""); }}
                      disabled={desvincularAluno.isPending}
                    >
                      <option value="">Selecione uma turma</option>
                      {turmas.map((t) => (
                        <option key={t.id} value={String(t.id)}>{t.nome}</option>
                      ))}
                    </select>
                  </div>
                  <div className="space-y-1">
                    <Label>Aluno</Label>
                    <select
                      className="w-full h-9 rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-400"
                      value={alunoIdDesvincular}
                      onChange={(e) => setAlunoIdDesvincular(e.target.value)}
                      disabled={!turmaIdDesvincular || desvincularAluno.isPending}
                    >
                      <option value="">
                        {turmaIdDesvincular ? "Selecione um aluno" : "Selecione a turma primeiro"}
                      </option>
                      {alunosDaTurma.map((a) => (
                        <option key={a.id} value={String(a.id)}>
                          {a.nome}{a.matricula ? ` — ${a.matricula}` : ""}
                        </option>
                      ))}
                    </select>
                  </div>
                </CardContent>
                <CardFooter>
                  <Button type="submit" variant="destructive" className="w-full"
                    disabled={desvincularAluno.isPending || !alunoIdDesvincular}>
                    {desvincularAluno.isPending ? "Desvinculando..." : "Desvincular Aluno"}
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
