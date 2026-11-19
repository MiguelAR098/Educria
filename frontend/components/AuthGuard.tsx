"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/auth-store";
import { UserRole } from "@/lib/types";
import { Loader2, Sparkles } from "lucide-react";

interface AuthGuardProps {
  children: React.ReactNode;
  allowedRole: UserRole;
}

export function AuthGuard({ children, allowedRole }: AuthGuardProps) {
  const router = useRouter();
  const { token, user, _hasHydrated } = useAuthStore();

  useEffect(() => {
    if (!_hasHydrated) return;

    if (!token || !user) {
      router.replace("/login");
      return;
    }

    if (user.role !== allowedRole) {
      if (user.role === "ADMIN") {
        router.replace("/admin");
      } else {
        router.replace("/aluno");
      }
    }
  }, [_hasHydrated, token, user, allowedRole, router]);

  if (!_hasHydrated || !token || !user || user.role !== allowedRole) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 gap-3">
        <div className="flex items-center gap-2 text-indigo-600 font-bold text-xl">
          <Sparkles className="h-6 w-6 animate-pulse" />
          <span>EDUCRIA</span>
        </div>
        <div className="flex items-center gap-2 text-sm text-slate-500">
          <Loader2 className="h-4 w-4 animate-spin" />
          <span>Verificando sua sessão...</span>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
