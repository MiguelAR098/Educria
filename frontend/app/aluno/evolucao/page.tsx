"use client";

import { Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowLeft, CalendarDays } from "lucide-react";
import { AuthGuard } from "@/components/AuthGuard";
import { EstadoConsulta } from "@/components/EstadoConsulta";
import { GraficoEvolucao } from "@/components/GraficoEvolucao";
import { Navbar } from "@/components/Navbar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useEvolucao, type AssuntoEvolucao, type RelatorioEvolucao } from "@/lib/evolucao";
import { dataBrasileira } from "@/lib/revisoes";

const rota = "/aluno/evolucao";
const numero = new Intl.NumberFormat("pt-BR");
const percentual = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 2 });

function taxa(valor: number | null) {
  return valor === null ? "Sem dados" : percentual.format(valor / 100);
}

function Resumo({ resumo }: { resumo: RelatorioEvolucao["resumo"] }) {
  const indicadores = [
    ["Sessões iniciadas", numero.format(resumo.sessoesIniciadas)],
    ["Tentativas respondidas", numero.format(resumo.tentativas)],
    ["Questões únicas", numero.format(resumo.questoesUnicas)],
    ["Acerto na primeira tentativa", taxa(resumo.taxaAcertoPrimeiraTentativa)],
  ];
  const respostas = [
    ["Acertos", resumo.acertos],
    ["Erros", resumo.erros],
    ["Tentativas de reaprendizagem", resumo.tentativasReaprendizagem],
  ] as const;

  return (
    <Card>
      <CardHeader><h2 className="text-xl font-bold text-slate-900">Resumo do período</h2></CardHeader>
      <CardContent className="space-y-4">
        <dl className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          {indicadores.map(([rotulo, valor]) => (
            <div key={rotulo} className="flex min-w-0 flex-col justify-between rounded-xl bg-indigo-50/70 p-3 sm:p-4">
              <dt className="text-sm font-semibold text-indigo-900">{rotulo}</dt>
              <dd className="mt-1 text-2xl font-extrabold tabular-nums text-slate-900">{valor}</dd>
            </div>
          ))}
        </dl>
        <dl className="flex flex-wrap gap-x-6 gap-y-2 border-t border-slate-100 pt-3 text-sm text-slate-700">
          {respostas.map(([rotulo, valor]) => (
            <div key={rotulo} className="inline-flex flex-wrap items-baseline gap-x-2">
              <dt>{rotulo}</dt>
              <dd className="font-bold tabular-nums text-slate-900">{numero.format(valor)}</dd>
            </div>
          ))}
        </dl>
      </CardContent>
    </Card>
  );
}

function NivelObservado({ rotulo, nivel }: { rotulo: string; nivel: number | null }) {
  return (
    <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-2 gap-y-1">
      <dt className="text-sm text-slate-600">{rotulo}</dt>
      <dd className="text-sm font-semibold tabular-nums text-slate-800">{nivel === null ? "Sem registro" : `${nivel}/10`}</dd>
      {nivel !== null && (
        <dd aria-hidden="true" className="col-span-2 h-2 rounded-full bg-indigo-100">
          <div className="h-full rounded-full bg-indigo-600" style={{ width: `${nivel * 10}%` }} />
        </dd>
      )}
    </div>
  );
}

function DesempenhoPorAssunto({ assuntos }: { assuntos: AssuntoEvolucao[] }) {
  return (
    <section aria-labelledby="titulo-assuntos-evolucao" className="space-y-3">
      <h2 id="titulo-assuntos-evolucao" className="text-xl font-bold text-slate-900">Desempenho por assunto</h2>
      {assuntos.length === 0 ? (
        <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-700">Nenhum assunto com atividade no período.</p>
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2">
          {assuntos.map((assunto) => (
            <li key={assunto.assuntoId}>
              <Card className="h-full">
                <CardHeader className="p-5 pb-3">
                  <CardTitle className="text-lg first-letter:uppercase">{assunto.nome}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4 px-5 pb-5 text-sm">
                  <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-slate-700">
                    {([
                      ["Sessões iniciadas", assunto.sessoesIniciadas],
                      ["Tentativas", assunto.tentativas],
                      ["Acertos", assunto.acertos],
                      ["Erros", assunto.erros],
                      ["Questões únicas", assunto.questoesUnicas],
                    ] as const).map(([rotulo, valor]) => (
                      <div key={rotulo}><dt>{rotulo}</dt><dd className="font-bold tabular-nums">{numero.format(valor)}</dd></div>
                    ))}
                    <div><dt>Acerto na primeira tentativa</dt><dd className="font-bold">{taxa(assunto.taxaAcertoPrimeiraTentativa)}</dd></div>
                  </dl>
                  <div className="space-y-2 rounded-xl bg-slate-50 p-3">
                    <p className="font-semibold text-slate-900">Níveis observados</p>
                    <dl className="space-y-2">
                      <NivelObservado rotulo="Antes da primeira resposta" nivel={assunto.nivelInicialObservado} />
                      <NivelObservado rotulo="Após a última resposta" nivel={assunto.nivelFinalObservado} />
                    </dl>
                  </div>
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function EvolucaoDoAluno() {
  const router = useRouter();
  const params = useSearchParams();
  const id = Number(params.get("assuntoId"));
  const assuntoId = Number.isSafeInteger(id) && id > 0 ? id : null;
  const geral = useEvolucao();
  const filtrada = useEvolucao(assuntoId, assuntoId !== null);
  const consulta = assuntoId === null ? geral : filtrada;
  const relatorio = consulta.data;
  const opcoes = geral.data?.assuntos ?? [];
  const assuntoForaDaLista = assuntoId !== null && !opcoes.some((assunto) => assunto.assuntoId === assuntoId);

  return (
    <>
      <header className="flex flex-wrap items-end justify-between gap-4 rounded-[1.25rem] border border-indigo-100 bg-white p-5 shadow-sm sm:p-7">
        <div className="min-w-0">
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">Evolução da aprendizagem</h1>
          <p className="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-slate-600">
            <CalendarDays aria-hidden="true" size={16} className="text-indigo-700" /> Últimos 30 dias
            {relatorio && <span><time dateTime={relatorio.dataInicial}>{dataBrasileira(relatorio.dataInicial)}</time>
              {" a "}<time dateTime={relatorio.dataFinal}>{dataBrasileira(relatorio.dataFinal)}</time></span>}
          </p>
        </div>
        <label className="flex w-full flex-col gap-1.5 text-sm font-semibold text-slate-700 sm:w-auto">
          Assunto
          <select value={assuntoId ?? ""} onChange={(evento) => {
            const filtro = evento.target.value;
            router.replace(filtro ? `${rota}?assuntoId=${filtro}` : rota, { scroll: false });
          }} className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 focus-visible:outline focus-visible:outline-2 focus-visible:outline-indigo-600 sm:w-60">
            <option value="">Todos os assuntos</option>
            {opcoes.map((assunto) => (
              <option key={assunto.assuntoId} value={assunto.assuntoId}>
                {opcoes.some((outro) => outro.assuntoId !== assunto.assuntoId && outro.nome === assunto.nome)
                  ? `${assunto.nome} (ID ${assunto.assuntoId})` : assunto.nome}
              </option>
            ))}
            {assuntoForaDaLista && <option value={assuntoId}>{filtrada.data?.assuntos[0]?.nome ?? "Assunto selecionado"}</option>}
          </select>
        </label>
      </header>
      <EstadoConsulta consulta={consulta} recurso="a evolução da aprendizagem" />
      {relatorio && (
        <>
          <Resumo resumo={relatorio.resumo} />
          <GraficoEvolucao key={assuntoId ?? "todos"} dias={relatorio.evolucaoDiaria} />
          <DesempenhoPorAssunto assuntos={relatorio.assuntos} />
        </>
      )}
    </>
  );
}

export default function EvolucaoPage() {
  return (
    <AuthGuard allowedRole="ALUNO">
      <div className="min-h-screen bg-gradient-to-b from-indigo-50/70 via-slate-50 to-slate-50">
        <Navbar />
        <main className="mx-auto w-full max-w-5xl space-y-5 px-4 py-6 sm:px-6 sm:py-9">
          <Link href="/aluno" className="inline-flex items-center gap-2 rounded text-sm font-semibold text-indigo-700 hover:underline">
            <ArrowLeft aria-hidden="true" size={16} /> Voltar ao painel
          </Link>
          <Suspense fallback={<p role="status">Carregando a evolução da aprendizagem…</p>}>
            <EvolucaoDoAluno />
          </Suspense>
        </main>
      </div>
    </AuthGuard>
  );
}
