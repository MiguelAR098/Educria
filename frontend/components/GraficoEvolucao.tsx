import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { dataBrasileira } from "@/lib/revisoes";
import type { DiaEvolucao } from "@/lib/evolucao";

const numero = new Intl.NumberFormat("pt-BR");
const series = [
  { rotulo: "Acertos", cor: "bg-indigo-600", valor: (dia: DiaEvolucao) => dia.acertos },
  { rotulo: "Erros contabilizados", cor: "bg-amber-400", valor: (dia: DiaEvolucao) => dia.erros },
  { rotulo: "Outras tentativas incorretas", cor: "bg-slate-400", valor: (dia: DiaEvolucao) => dia.tentativasIncorretas - dia.erros },
];

export function GraficoEvolucao({ dias }: { dias: DiaEvolucao[] }) {
  const maior = Math.max(0, ...dias.map((dia) => dia.tentativas));
  const intervalo = Math.max(1, Math.ceil(maior / 3));
  const teto = intervalo * 3;
  const marcas = [0, 1, 2, 3, 4].map((indice) => dias[Math.floor(indice * (dias.length - 1) / 4)]);

  return (
    <Card>
      <CardHeader className="gap-3 space-y-0 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="text-xl font-bold text-slate-900">Tentativas por dia</h2>
        <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-700">
          {series.map(({ rotulo, cor }) => (
            <span key={rotulo} className="inline-flex items-center gap-2">
              <span aria-hidden="true" className={`h-3 w-3 shrink-0 rounded-sm ${cor}`} /> {rotulo}
            </span>
          ))}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {maior === 0 ? <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-700">Nenhuma tentativa registrada no período.</p> : (
          <div role="img" aria-label="Gráfico de tentativas por dia, dividido em acertos, erros contabilizados e outras tentativas incorretas."
            className="grid grid-cols-[auto_minmax(0,1fr)] gap-x-2 gap-y-2 sm:gap-x-3">
            <div className="flex h-40 flex-col justify-between pb-px text-right text-xs leading-none tabular-nums text-slate-600">
              {[teto, intervalo * 2, intervalo, 0].map((valor) => <span key={valor}>{numero.format(valor)}</span>)}
            </div>
            <div className="relative h-40 border-b border-slate-300">
              <div className="absolute inset-0 grid grid-rows-3">
                {[0, 1, 2].map((linha) => <div key={linha} className="border-t border-dashed border-slate-200" />)}
              </div>
              <div className="relative flex h-full gap-px px-px sm:gap-1">
                {dias.map((dia) => (
                  <div key={dia.data} title={`${dataBrasileira(dia.data)} · ${dia.tentativas} tentativas · ${dia.acertos} acertos · ${dia.tentativasIncorretas} tentativas incorretas · ${dia.erros} erros contabilizados`}
                    className="flex min-w-0 flex-1 flex-col-reverse hover:bg-indigo-50">
                    {series.map(({ rotulo, cor, valor }) => (
                      <span key={rotulo} className={cor} style={{ height: `${valor(dia) / teto * 100}%` }} />
                    ))}
                  </div>
                ))}
              </div>
            </div>
            <div className="col-start-2 flex justify-between gap-1 text-xs tabular-nums text-slate-600">
              {marcas.map((dia, indice) => (
                <time key={dia.data} dateTime={dia.data} className={indice % 2 ? "hidden sm:inline" : undefined}>
                  {dataBrasileira(dia.data).slice(0, 5)}
                </time>
              ))}
            </div>
          </div>
        )}
        <details className="rounded-lg border border-slate-200 px-3">
          <summary className="cursor-pointer rounded py-2 text-sm font-semibold text-indigo-700">Ver dados por dia</summary>
          <table className="w-full table-auto border-collapse text-left text-sm tabular-nums">
            <caption className="sr-only">Tentativas, acertos, tentativas incorretas e erros contabilizados por data</caption>
            <thead className="bg-slate-50 text-slate-700">
              <tr>
                <th scope="col" className="px-1 py-2 sm:px-3">Data</th>
                <th scope="col" className="px-1 py-2 text-right sm:px-3">Tentativas</th>
                <th scope="col" className="px-1 py-2 text-right sm:px-3">Acertos</th>
                <th scope="col" className="px-1 py-2 text-right sm:px-3">Tentativas incorretas</th>
                <th scope="col" className="px-1 py-2 text-right sm:px-3">Erros contabilizados</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {dias.map((dia) => (
                <tr key={dia.data}>
                  <th scope="row" className="px-1 py-1.5 font-medium sm:px-3"><time dateTime={dia.data}>{dataBrasileira(dia.data)}</time></th>
                  <td className="px-1 py-1.5 text-right sm:px-3">{numero.format(dia.tentativas)}</td>
                  <td className="px-1 py-1.5 text-right sm:px-3">{numero.format(dia.acertos)}</td>
                  <td className="px-1 py-1.5 text-right sm:px-3">{numero.format(dia.tentativasIncorretas)}</td>
                  <td className="px-1 py-1.5 text-right sm:px-3">{numero.format(dia.erros)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </details>
      </CardContent>
    </Card>
  );
}
