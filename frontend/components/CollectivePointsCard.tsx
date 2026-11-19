import { Users } from "lucide-react";
import type { PontosTurma } from "@/lib/types";

export function CollectivePointsCard({ pontosTurma }: { pontosTurma: PontosTurma }) {
  return (
    <section aria-labelledby="pontos-turma" className="relative flex h-full min-h-40 flex-col justify-center overflow-hidden rounded-[1.25rem] border border-violet-200/70 bg-gradient-to-br from-white via-violet-50 to-violet-100/80 p-6">
      <div className="mb-4 flex items-center gap-2.5">
        <span className="grid h-9 w-9 place-items-center rounded-xl bg-violet-200/70 text-violet-800"><Users aria-hidden size={19} /></span>
        <h2 id="pontos-turma" className="font-bold text-violet-950">Pontos da turma</h2>
      </div>
      {pontosTurma.turmaId == null ? <p className="text-sm text-slate-600">Sem turma vinculada.</p> : <>
        <p className="text-3xl font-extrabold tracking-tight text-violet-950">
          {pontosTurma.pontosTotais.toLocaleString("pt-BR")} <span className="text-base font-semibold text-violet-700">pts</span>
        </p>
        <p className="mt-2 break-words text-sm text-violet-900">Pontuação coletiva · {pontosTurma.nomeTurma}</p>
      </>}
    </section>
  );
}
