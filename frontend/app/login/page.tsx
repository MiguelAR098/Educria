"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import { rotaInicial } from "@/lib/rotas";
import { AuthResponse, LoginPayload } from "@/lib/types";
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
import { Sparkles, Mail, Lock, Loader2, ArrowRight } from "lucide-react";
import { toast } from "sonner";
import { AxiosError } from "axios";

export default function LoginPage() {
  const router = useRouter();
  const { setAuth, token, user, _hasHydrated } = useAuthStore();

  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");

  useEffect(() => {
    if (_hasHydrated && token && user) {
      router.replace(rotaInicial(user.role));
    }
  }, [_hasHydrated, token, user, router]);

  const loginMutation = useMutation({
    mutationFn: async (payload: LoginPayload) => {
      const response = await api.post<AuthResponse>("/api/auth/login", payload);
      return response.data;
    },
    onSuccess: (data) => {
      const role = data.perfil || data.user?.role || data.role || "ALUNO";
      setAuth(data.token, {
        id: data.id || data.user?.id,
        nome: data.user?.nome || data.nome || email.split("@")[0],
        email: data.user?.email || data.email || email,
        role,
        matricula: data.user?.matricula || data.matricula,
      });

      toast.success("Login realizado com sucesso! Bem-vindo de volta.");
      router.push(rotaInicial(role));
    },
    onError: (error: AxiosError<{ message?: string; error?: string }>) => {
      const msg =
        error.response?.data?.message ||
        error.response?.data?.error ||
        "E-mail ou senha inválidos. Verifique seus dados.";
      toast.error(msg);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !senha.trim()) {
      toast.warning("Preencha seu e-mail e sua senha.");
      return;
    }
    loginMutation.mutate({ email, senha });
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-50 via-slate-50 to-violet-100/70 p-4">
      <div className="w-full max-w-md space-y-6">
        {/* Logo / Marca */}
        <div className="flex flex-col items-center text-center space-y-2">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-600 to-violet-600 text-white shadow-lg shadow-indigo-200">
            <Sparkles className="h-7 w-7" />
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-indigo-600 to-violet-600 bg-clip-text text-transparent">
            EDUCRIA
          </h1>
          <p className="text-sm text-slate-500 max-w-xs">
            Sua jornada de aprendizagem adaptativa e gamificada começa aqui.
          </p>
        </div>

        <Card className="border-slate-200/80 shadow-xl shadow-indigo-100/50">
          <CardHeader className="space-y-1 pb-4">
            <CardTitle className="text-2xl text-center">Entrar na conta</CardTitle>
            <CardDescription className="text-center">
              Digite seu e-mail e senha para acessar sua trilha
            </CardDescription>
          </CardHeader>

          <form onSubmit={handleSubmit}>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">E-mail</Label>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-3 h-5 w-5 text-slate-400" />
                  <Input
                    id="email"
                    type="email"
                    placeholder="seu.email@escola.com"
                    className="pl-11"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    disabled={loginMutation.isPending}
                    required
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="senha">Senha</Label>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-3 h-5 w-5 text-slate-400" />
                  <Input
                    id="senha"
                    type="password"
                    placeholder="••••••••"
                    className="pl-11"
                    value={senha}
                    onChange={(e) => setSenha(e.target.value)}
                    disabled={loginMutation.isPending}
                    required
                  />
                </div>
              </div>
            </CardContent>

            <CardFooter className="flex flex-col space-y-4 pt-2">
              <Button
                type="submit"
                className="w-full gap-2 text-base"
                disabled={loginMutation.isPending}
              >
                {loginMutation.isPending ? (
                  <>
                    <Loader2 className="h-5 w-5 animate-spin" />
                    Entrando...
                  </>
                ) : (
                  <>
                    Entrar
                    <ArrowRight className="h-4 w-4" />
                  </>
                )}
              </Button>

              <p className="text-sm text-center text-slate-600">
                Ainda não tem uma conta?{" "}
                <Link
                  href="/register"
                  className="font-semibold text-indigo-600 hover:text-indigo-700 hover:underline"
                >
                  Cadastre-se gratuitamente
                </Link>
              </p>
            </CardFooter>
          </form>
        </Card>
      </div>
    </div>
  );
}
