"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/auth-store";
import { Loader2, Sparkles } from "lucide-react";

export default function HomePage() {
  const router = useRouter();
  const { token, user, _hasHydrated } = useAuthStore();

  useEffect(() => {
    if (!_hasHydrated) return;

    if (!token || !user) {
      router.replace("/login");
    } else if (user.role === "ADMIN") {
      router.replace("/admin");
    } else {
      router.replace("/aluno");
    }
  }, [_hasHydrated, token, user, router]);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gradient-to-br from-indigo-50 via-white to-violet-50 gap-3">
      <div className="flex items-center gap-2.5 text-indigo-600 font-extrabold text-2xl">
        <Sparkles className="h-7 w-7 animate-pulse" />
        <span>EDUCRIA</span>
      </div>
      <div className="flex items-center gap-2 text-sm text-slate-500">
        <Loader2 className="h-4 w-4 animate-spin text-indigo-600" />
        <span>Carregando plataforma...</span>
      </div>
    </div>
  );
}
