"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import { rotaInicial } from "@/lib/rotas";
import { AuthResponse, RegisterPayload } from "@/lib/types";
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
import {
  Sparkles,
  User,
  Mail,
  Lock,
  Hash,
  Users,
  Loader2,
  Rocket,
} from "lucide-react";
import { toast } from "sonner";
import { AxiosError } from "axios";

export default function RegisterPage() {
  const router = useRouter();
  const { setAuth, token, user, _hasHydrated } = useAuthStore();

  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [matricula, setMatricula] = useState("");
  const [turmaId, setTurmaId] = useState("");

  useEffect(() => {
    if (_hasHydrated && token && user) {
      router.replace(rotaInicial(user.role));
    }
  }, [_hasHydrated, token, user, router]);

  const registerMutation = useMutation({
    mutationFn: async (payload: RegisterPayload) => {
      const registerRes = await api.post<AuthResponse>(
        "/api/auth/register",
        payload
      );

      if (registerRes.data?.token) {
        return registerRes.data;
      }

      const loginRes = await api.post<AuthResponse>("/api/auth/login", {
        email: payload.email,
        senha: payload.senha,
      });
      return loginRes.data;
    },
    onSuccess: (data) => {
      const role = data.perfil || data.user?.role || data.role || "ALUNO";
      setAuth(data.token, {
        id: data.id || data.user?.id,
        nome: data.user?.nome || data.nome || nome,
        email: data.user?.email || data.email || email,
        role,
        matricula: data.user?.matricula || data.matricula || matricula || undefined,
      });

      toast.success("Conta criada com sucesso! Preparando seu painel...");
      router.push("/aluno");
    },
    onError: (
      error: AxiosError<{
        message?: string;
        error?: string;
        errors?: Record<string, string>;
      }>
    ) => {
      const validationErrors = error.response?.data?.errors;
      const firstValidationMsg = validationErrors
        ? Object.values(validationErrors)[0]
        : null;

      const msg =
        firstValidationMsg ||
        error.response?.data?.message ||
        error.response?.data?.error ||
        "Não foi possível concluir o cadastro. Tente novamente.";
      toast.error(msg);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!nome.trim() || !email.trim() || !senha.trim()) {
      toast.warning("Preencha nome, e-mail e senha.");
      return;
    }
    if (senha.length < 6) {
      toast.warning("A senha deve ter no mínimo 6 caracteres.");
      return;
    }

    registerMutation.mutate({
      nome: nome.trim(),
      email: email.trim(),
      senha,
      ...(matricula.trim() ? { matricula: matricula.trim() } : {}),
      ...(turmaId.trim() ? { turmaId: Number(turmaId.trim()) } : {}),
    });
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-violet-50 via-slate-50 to-indigo-100/70 p-4">
      <div className="w-full max-w-md space-y-6">
        <div className="flex flex-col items-center text-center space-y-2">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-600 to-violet-600 text-white shadow-lg shadow-indigo-200">
            <Sparkles className="h-7 w-7" />
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-indigo-600 to-violet-600 bg-clip-text text-transparent">
            EDUCRIA
          </h1>
          <p className="text-sm text-slate-500">
            Crie sua conta e comece a evoluir de nível hoje mesmo!
          </p>
        </div>

        <Card className="border-slate-200/80 shadow-xl shadow-indigo-100/50">
          <CardHeader className="space-y-1 pb-4">
            <CardTitle className="text-2xl text-center">Criar nova conta</CardTitle>
            <CardDescription className="text-center">
              Preencha seus dados de estudante do Ensino Fundamental II
            </CardDescription>
          </CardHeader>

          <form onSubmit={handleSubmit}>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="nome">Nome completo *</Label>
                <div className="relative">
                  <User className="absolute left-3.5 top-3 h-5 w-5 text-slate-400" />
                  <Input
                    id="nome"
                    type="text"
                    placeholder="Ex: Lucas Silva"
                    className="pl-11"
                    value={nome}
                    onChange={(e) => setNome(e.target.value)}
                    disabled={registerMutation.isPending}
                    required
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="email">E-mail *</Label>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-3 h-5 w-5 text-slate-400" />
                  <Input
                    id="email"
                    type="email"
                    placeholder="lucas@escola.com"
                    className="pl-11"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    disabled={registerMutation.isPending}
                    required
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="senha">Senha * (mínimo 6 caracteres)</Label>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-3 h-5 w-5 text-slate-400" />
                  <Input
                    id="senha"
                    type="password"
                    minLength={6}
                    placeholder="Crie uma senha segura"
                    className="pl-11"
                    value={senha}
                    onChange={(e) => setSenha(e.target.value)}
                    disabled={registerMutation.isPending}
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="matricula">Matrícula</Label>
                    <span className="text-[0.6875rem] text-slate-400">Opcional</span>
                  </div>
                  <div className="relative">
                    <Hash className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                    <Input
                      id="matricula"
                      type="text"
                      placeholder="202600123"
                      className="pl-9"
                      value={matricula}
                      onChange={(e) => setMatricula(e.target.value)}
                      disabled={registerMutation.isPending}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="turmaId">ID da Turma</Label>
                    <span className="text-[0.6875rem] text-slate-400">Opcional</span>
                  </div>
                  <div className="relative">
                    <Users className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                    <Input
                      id="turmaId"
                      type="number"
                      min={1}
                      placeholder="Ex: 1"
                      className="pl-9"
                      value={turmaId}
                      onChange={(e) => setTurmaId(e.target.value)}
                      disabled={registerMutation.isPending}
                    />
                  </div>
                </div>
              </div>
            </CardContent>

            <CardFooter className="flex flex-col space-y-4 pt-2">
              <Button
                type="submit"
                variant="success"
                className="w-full gap-2 text-base"
                disabled={registerMutation.isPending}
              >
                {registerMutation.isPending ? (
                  <>
                    <Loader2 className="h-5 w-5 animate-spin" />
                    Criando conta...
                  </>
                ) : (
                  <>
                    Começar jornada
                    <Rocket className="h-4 w-4" />
                  </>
                )}
              </Button>

              <p className="text-sm text-center text-slate-600">
                Já possui cadastro?{" "}
                <Link
                  href="/login"
                  className="font-semibold text-indigo-600 hover:text-indigo-700 hover:underline"
                >
                  Fazer login
                </Link>
              </p>
            </CardFooter>
          </form>
        </Card>
      </div>
    </div>
  );
}
