"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { api } from "@/lib/api";
import { mensagemErro, rotaSessoes, useChaveAluno } from "@/lib/estudo";
import { Indicadores, inicio, Resultado, situacoes } from "@/lib/historico";

function Resumo({ sessao }: { sessao: Indicadores }) {
  const respondidas = sessao.acertos + sessao.erros;
  return (
    <div className="space-y-3">
      <dl className="grid grid-cols-2 gap-3 text-slate-700 sm:grid-cols-4">
        {([["Acertos", sessao.acertos], ["Erros", sessao.erros], ["Respondidas", respondidas],
          ["Sem resposta", sessao.apresentadas - respondidas]] as const).map(([nome, valor]) => (
          <div key={nome}><dt className="text-sm">{nome}</dt><dd className="text-xl font-bold text-slate-900">{valor}</dd></div>
        ))}
      </dl>
      {respondidas === 0 ? <p className="text-sm text-slate-600">Nenhuma resposta foi gravada nesta sessão.</p> :
        <p className="text-sm">Nível inicial: {sessao.nivelAntes ?? "sem registro"} · Nível final: {sessao.nivelDepois ?? "sem registro"}</p>}
    </div>
  );
}

export default function ResultadoPage() {
  const { id } = useParams<{ id: string }>();
  const aluno = useChaveAluno();
  const consulta = useQuery({
    queryKey: ["resultado-sessao", aluno, id],
    queryFn: async ({ signal }) => (await api.get<Resultado>(`${rotaSessoes}/${encodeURIComponent(id)}/resultado`, { signal })).data,
    staleTime: 0,
  });
  const dados = consulta.data;
  return (
    <>
      <Link href="/aluno/historico" className="inline-flex items-center gap-2 font-semibold text-indigo-700">
        <ArrowLeft size={16} /> Histórico de sessões
      </Link>
      <h1 className="text-3xl font-bold text-slate-900">Resultado da sessão</h1>
      {consulta.isPending && <p role="status">Carregando resultado…</p>}
      {consulta.isError && <div role="alert" className="space-y-2"><p>{mensagemErro(consulta.error)}</p>
        <Button variant="outline" onClick={() => consulta.refetch()}>Tentar novamente</Button></div>}
      {dados && <>
        <Card><CardHeader><h2 className="text-2xl font-bold first-letter:uppercase">{dados.sessao.assunto}</h2>
          <p className="text-sm text-slate-600">{inicio(dados.sessao)} · {situacoes[dados.sessao.situacao]}</p>
        </CardHeader><CardContent className="space-y-4">
          {dados.sessao.situacao !== "ENCERRADA" && <p className="font-medium text-indigo-700">Resultado parcial</p>}
          <Resumo sessao={dados.sessao} />
          {dados.sessao.situacao !== "ENCERRADA" && <Button asChild variant="outline">
            <Link href={`/aluno/atividade/${dados.sessao.id}`}>Abrir atividade</Link></Button>}
        </CardContent></Card>
        <Card><CardHeader><h2 className="text-xl font-bold">Suas respostas</h2></CardHeader>
          <CardContent>
            <ol className="divide-y divide-slate-200">{dados.questoes?.map((q) => (
              <li key={q.numero} className="space-y-1 py-3">
                <p className="break-words"><strong>Questão {q.numero}</strong> · {q.enunciado}</p>
                <p className="text-sm">
                  <strong>{q.correta === null ? "Sem resposta registrada" : q.correta ? "Acertou" : "Errou"}</strong>
                  {q.alternativa && <span> · Sua alternativa: {q.alternativa}</span>}
                </p>
              </li>))}</ol>
            {!dados.questoes && <p>Atualize o backend para ver as respostas por questão.</p>}
          </CardContent>
        </Card>
        <Card><CardHeader><h2 className="text-xl font-bold">Comparação com a sessão anterior</h2></CardHeader>
          <CardContent className="space-y-3">{dados.anterior ? <>
            <p className="text-sm"><strong className="capitalize">{dados.anterior.assunto}</strong> · {inicio(dados.anterior)}</p>
            <table className="w-full text-left text-sm"><thead><tr className="border-b text-slate-600">
              <th scope="col" className="py-2">Indicador</th><th scope="col">Esta sessão</th><th scope="col">Anterior</th>
            </tr></thead><tbody>
              {([["Acertos", "acertos"], ["Erros", "erros"], ["Apresentadas", "apresentadas"]] as const).map(([nome, campo]) => (
                <tr key={campo} className="border-b last:border-0"><th scope="row" className="py-2 font-medium">{nome}</th>
                  <td>{dados.sessao[campo]}</td><td>{dados.anterior?.[campo]}</td></tr>))}
            </tbody></table>
          </> : <p>Esta é a sua primeira sessão.</p>}</CardContent>
        </Card>
        <Card><CardHeader><h2 className="text-xl font-bold">Questões desta sessão para revisão</h2></CardHeader>
          <CardContent>{dados.revisoes.length ? <>
            <p className="mb-3 text-sm text-slate-600">Próximas datas na sua agenda atual.</p>
            <ul className="divide-y divide-slate-200">{dados.revisoes.map((r) => (
              <li key={r.questaoId} className="flex flex-wrap justify-between gap-2 py-2">
                <span className="break-words">{r.enunciado}</span>
                <time dateTime={r.proximaRevisao} className="font-semibold text-indigo-700">{r.proximaRevisao.split("-").reverse().join("/")}</time>
              </li>))}</ul>
          </> : <p>Nenhuma revisão agendada para as respostas desta sessão.</p>}</CardContent>
        </Card>
      </>}
    </>
  );
}
