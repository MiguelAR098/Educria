"use client";

import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/auth-store";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { LogOut, Sparkles, ShieldCheck, GraduationCap } from "lucide-react";
import { toast } from "sonner";

export function Navbar() {
  const router = useRouter();
  const { user, logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    toast.info("Você saiu da sua conta. Até logo!");
    router.replace("/login");
  };

  const initials = user?.nome
    ? user.nome
        .split(" ")
        .slice(0, 2)
        .map((n) => n[0])
        .join("")
        .toUpperCase()
    : "ED";

  return (
    <header className="sticky top-0 z-30 w-full border-b border-slate-200/80 bg-white/90 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-5xl items-center justify-between px-4 sm:px-6">
        <div className="flex items-center gap-2.5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-600 to-violet-600 text-white shadow-sm">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <span className="text-lg font-extrabold tracking-tight bg-gradient-to-r from-indigo-600 to-violet-600 bg-clip-text text-transparent">
              EDUCRIA
            </span>
            <p className="hidden sm:block text-[11px] font-medium text-slate-400 leading-none">
              Aprendizagem Adaptativa
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3 sm:gap-4">
          <div className="flex items-center gap-2.5">
            <Avatar className="h-9 w-9 border border-indigo-200">
              <AvatarFallback>{initials}</AvatarFallback>
            </Avatar>
            <div className="hidden md:flex flex-col text-right">
              <span className="text-sm font-semibold text-slate-800 leading-tight">
                {user?.nome || "Estudante"}
              </span>
              <span className="text-xs text-slate-500">
                {user?.matricula ? `Matrícula: ${user.matricula}` : user?.email}
              </span>
            </div>
            <Badge
              variant={user?.role === "ADMIN" ? "secondary" : "default"}
              className="hidden sm:inline-flex gap-1"
            >
              {user?.role === "ADMIN" ? (
                <>
                  <ShieldCheck className="h-3.5 w-3.5" />
                  Admin
                </>
              ) : (
                <>
                  <GraduationCap className="h-3.5 w-3.5" />
                  Aluno
                </>
              )}
            </Badge>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={handleLogout}
            className="text-slate-600 hover:text-red-600 hover:border-red-200 hover:bg-red-50/60 gap-1.5"
          >
            <LogOut className="h-4 w-4" />
            <span className="hidden sm:inline">Sair</span>
          </Button>
        </div>
      </div>
    </header>
  );
}
