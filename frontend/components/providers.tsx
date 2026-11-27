"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Toaster } from "@/components/ui/toaster";
import { useAuthStore } from "@/lib/auth-store";

function ConsultasDaSessao({ children, sessao }: { children: React.ReactNode; sessao: number }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 1000 * 60, // 1 minuto
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      })
  );

  useEffect(() => {
    const limparSessaoAnterior = () => {
      if (useAuthStore.getState().sessao !== sessao) queryClient.clear();
    };
    const unsubscribe = useAuthStore.subscribe(limparSessaoAnterior);
    limparSessaoAnterior();
    return () => {
      unsubscribe();
      limparSessaoAnterior();
    };
  }, [queryClient, sessao]);

  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
}

export function Providers({ children }: { children: React.ReactNode }) {
  const sessao = useAuthStore((state) => state.sessao);

  return (
    <>
      <ConsultasDaSessao key={sessao} sessao={sessao}>{children}</ConsultasDaSessao>
      <Toaster />
    </>
  );
}
