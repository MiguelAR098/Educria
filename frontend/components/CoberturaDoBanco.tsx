import { Check, Minus, TriangleAlert } from "lucide-react";
import type { CoberturaBancoQuestoes } from "@/lib/types";
import { cn } from "@/lib/utils";

const dificuldades = Array.from({ length: 10 }, (_, indice) => indice + 1);
const meta = 6;
const numero = new Intl.NumberFormat("pt-BR");

const estados = {
  vazio: {
    icone: Minus,
    rotulo: "sem questões",
    intervalo: "0 questões",
    cor: "border-red-200 bg-red-50 text-red-900",
  },
  abaixo: {
    icone: TriangleAlert,
    rotulo: "abaixo da meta",
    intervalo: `1 a ${meta - 1} questões`,
    cor: "border-amber-200 bg-amber-50 text-amber-900",
  },
  atingida: {
    icone: Check,
    rotulo: "meta atingida",
    intervalo: `${meta} ou mais questões`,
    cor: "border-emerald-200 bg-emerald-50 text-emerald-900",
  },
};

type Props = {
  cobertura: CoberturaBancoQuestoes;
  assuntoId: number | null;
  dificuldade: number | null;
  onSelecionar: (assuntoId: number, dificuldade: number) => void;
};

export function CoberturaDoBanco({ cobertura, assuntoId, dificuldade, onSelecionar }: Props) {
  if (cobertura.assuntos.length === 0) {
    return <p className="text-sm text-slate-700">Esta matéria ainda não tem assuntos cadastrados.</p>;
  }

  return (
    <div className="min-w-0 space-y-4 text-sm">
      <dl className="grid gap-3 sm:grid-cols-2">
        <div className="min-w-0 rounded-xl bg-slate-50 px-4 py-3">
          <dt className="text-slate-700">Total de questões elegíveis</dt>
          <dd className="text-lg font-bold tabular-nums text-slate-900">{numero.format(cobertura.total)}</dd>
        </div>
        <div className="min-w-0 rounded-xl bg-slate-50 px-4 py-3">
          <dt className="text-slate-700">Combinações sem questões</dt>
          <dd className="text-lg font-bold tabular-nums text-slate-900">{numero.format(cobertura.combinacoesVazias)}</dd>
        </div>
      </dl>

      {cobertura.total === 0 && (
        <p className="rounded-xl bg-slate-50 px-4 py-3 text-slate-700">Nenhuma questão elegível no banco.</p>
      )}

      <p className="text-slate-700">
        Selecione uma célula para filtrar a lista por assunto e dificuldade.
        A meta de {meta} questões por combinação é uma referência pedagógica.
      </p>
      <ul aria-label="Legenda da cobertura" className="flex flex-wrap gap-2">
        {Object.values(estados).map(({ icone: Icone, intervalo, cor }) => (
          <li key={intervalo} className={cn("inline-flex items-center gap-1.5 rounded-lg border px-2 py-1", cor)}>
            <Icone aria-hidden="true" className="size-4 shrink-0" />
            {intervalo}
          </li>
        ))}
      </ul>

      <div
        role="region"
        aria-label="Matriz de cobertura por assunto e dificuldade"
        tabIndex={0}
        className="max-w-full overflow-x-auto rounded-xl border border-slate-200 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
      >
        <table className="w-full border-collapse">
          <caption className="sr-only">Questões elegíveis por assunto e dificuldade</caption>
          <colgroup span={1} />
          <colgroup span={dificuldades.length} />
          <colgroup span={1} />
          <thead className="bg-slate-50 text-slate-700">
            <tr>
              <th rowSpan={2} scope="col" className="min-w-28 px-3 py-2 text-left">Assunto</th>
              <th colSpan={dificuldades.length} scope="colgroup" className="px-2 pt-2 text-center">Dificuldade</th>
              <th rowSpan={2} scope="col" className="px-3 py-2 text-center">Total</th>
            </tr>
            <tr>
              {dificuldades.map((nivel) => (
                <th key={nivel} scope="col" className="px-2 py-2 text-center tabular-nums">{nivel}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200">
            {cobertura.assuntos.map((assunto) => (
              <tr key={assunto.id}>
                <th scope="row" className="px-3 py-2 text-left font-semibold text-slate-900 first-letter:uppercase">
                  {assunto.nome}
                </th>
                {dificuldades.map((nivel, indice) => {
                  const quantidade = assunto.contagens[indice];
                  const { icone: Icone, rotulo, cor } =
                    quantidade === 0 ? estados.vazio : quantidade < meta ? estados.abaixo : estados.atingida;
                  const total = numero.format(quantidade);
                  const unidade = quantidade === 1 ? "questão elegível" : "questões elegíveis";
                  return (
                    <td key={nivel} className="p-1.5 text-center">
                      <button
                        type="button"
                        aria-label={`${assunto.nome}, dificuldade ${nivel}, ${total} ${unidade}, ${rotulo}`}
                        aria-pressed={assunto.id === assuntoId && nivel === dificuldade}
                        className={cn(
                          "inline-flex min-h-11 min-w-11 items-center justify-center gap-1 whitespace-nowrap rounded-lg border px-1.5 font-semibold tabular-nums",
                          "hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-700 aria-pressed:ring-2 aria-pressed:ring-indigo-700",
                          cor
                        )}
                        onClick={() => onSelecionar(assunto.id, nivel)}
                      >
                        <Icone aria-hidden="true" className="size-3.5 shrink-0" />
                        {total}
                      </button>
                    </td>
                  );
                })}
                <td className="px-3 py-2 text-center font-bold tabular-nums text-slate-900">
                  {numero.format(assunto.total)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
