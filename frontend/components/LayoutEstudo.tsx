import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import type { ReactNode } from "react";

export default function LayoutEstudo({ children }: { children: ReactNode }) {
  return (
    <AuthGuard allowedRole="ALUNO">
      <div className="min-h-screen bg-slate-50">
        <Navbar />
        <main className="mx-auto max-w-3xl space-y-5 px-4 py-6 sm:px-6 sm:py-10">{children}</main>
      </div>
    </AuthGuard>
  );
}
