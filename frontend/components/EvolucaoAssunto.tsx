import Link from "next/link";
import { Button } from "@/components/ui/button";
import { EstadoConsulta } from "@/components/EstadoConsulta";
import { useSessoes } from "@/lib/estudo";
import { inicio, useHistorico } from "@/lib/historico";

export function EvolucaoAssunto({ assuntoId }: { assuntoId: number }) {
  const sessoes = useSessoes();
  const historico = useHistorico();
  // O histórico traz só o nome. A lista de sessões fornece o ID, inclusive para assuntos homônimos.
  const ids = new Set(sessoes.data?.filter((s) => s.assuntoId === assuntoId).map((s) => s.id));
  const itens = historico.data?.pages.flatMap((p) => p.sessoes).filter((s) => ids.has(s.id));
  const pronto = sessoes.isSuccess && historico.isSuccess;
  return <div className="mt-3 border-l-2 border-indigo-200 pl-3 text-sm">
    <Link className="mb-2 inline-block rounded font-semibold text-indigo-700 underline" href={`/aluno/evolucao?assuntoId=${assuntoId}`}>
      Relatório de 30 dias
    </Link>
    <EstadoConsulta consulta={sessoes} recurso="as sessões do assunto" />
    <EstadoConsulta consulta={historico} recurso="a evolução" />
    {pronto && ids.size === 0 && <p>Nenhuma sessão neste assunto.</p>}
    {sessoes.data && itens && <>
      <ul className="space-y-2">{itens.map((s) => (
        <li key={s.id}>
          <Link className="text-indigo-700 underline" href={`/aluno/historico/${s.id}`}>{inicio(s)}</Link>
          <p>{s.acertos + s.erros === 0 ? "Aguardando a primeira resposta." :
            `Nível inicial: ${s.nivelAntes ?? "sem registro"} · Nível final: ${s.nivelDepois ?? "sem registro"}`}</p>
        </li>
      ))}</ul>
      {ids.size > itens.length && historico.hasNextPage && <Button size="sm" variant="outline" className="mt-2"
        disabled={historico.isFetching} onClick={() => historico.fetchNextPage()}>
        Buscar sessões anteriores
      </Button>}
    </>}
  </div>;
}
