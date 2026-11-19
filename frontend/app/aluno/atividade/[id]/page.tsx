"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, BookOpen, Pause } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { api } from "@/lib/api";
import { Alternativa, EstadoSessao, FeedbackResposta, limiteQuestoes, mensagemErro, QuestaoAtual, rotaSessoes, semQuestoes,
  useAssuntosDeEstudo, useChaveAluno } from "@/lib/estudo";

interface AlternativasProps {
  questao: QuestaoAtual;
  feedback?: FeedbackResposta;
  bloqueadas: boolean;
  enviando: boolean;
  enviar: (alternativa: Alternativa) => void;
}

function Alternativas({ questao, feedback, bloqueadas, enviando, enviar }: AlternativasProps) {
  const [marcada, setMarcada] = useState<Alternativa>();
  return (
    <div className="space-y-4">
      <fieldset disabled={bloqueadas || !!feedback} className="space-y-2.5">
        <legend className="mb-3 text-sm text-slate-600">{feedback ? "Sua alternativa" : "Escolha uma alternativa."}</legend>
        {(["A", "B", "C", "D"] as const).map((letra) => {
          const texto = questao[`alternativa${letra}`];
          return (
            <label key={letra} className="block">
              <input type="radio" name="alternativa" value={letra} checked={(feedback?.alternativa ?? marcada) === letra}
                disabled={!texto} onChange={() => setMarcada(letra)} className="peer sr-only" />
              <span className="flex min-h-14 cursor-pointer items-center gap-3 rounded-xl border border-slate-200 bg-white px-3 py-3 text-slate-800 hover:border-indigo-300 peer-checked:border-indigo-600 peer-checked:bg-indigo-50 peer-focus-visible:ring-2 peer-focus-visible:ring-indigo-600 peer-disabled:cursor-default sm:px-4">
                <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg border border-slate-200 bg-white text-sm font-bold text-indigo-700">{letra}</span>
                <span className="min-w-0 break-words">{texto || "Alternativa indisponível"}</span>
              </span>
            </label>
          );
        })}
      </fieldset>
      {feedback ? (
        <div role="status" className="space-y-1 rounded-xl bg-indigo-50 p-4 text-slate-800">
          <p className="font-semibold">{feedback.correta ? "Você acertou!" : "Resposta incorreta."}</p>
          {feedback.explicacao && <p>{feedback.explicacao}</p>}
        </div>
      ) : (
        <Button className="w-full sm:w-auto" disabled={!marcada || bloqueadas}
          onClick={() => marcada && enviar(marcada)}>{enviando ? "Enviando…" : "Enviar resposta"}</Button>
      )}
    </div>
  );
}

function Voltar() {
  return <Button asChild variant="outline"><Link href="/aluno">Voltar ao painel</Link></Button>;
}

export default function QuestaoDaSessao() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const cache = useQueryClient();
  const aluno = useChaveAluno();
  const assuntos = useAssuntosDeEstudo();
  const chave = ["sessao-estudo", aluno, id];
  const rota = `${rotaSessoes}/${encodeURIComponent(id)}`;
  const consulta = useQuery({
    queryKey: chave,
    staleTime: 0,
    queryFn: async ({ signal }) => (await api.get<EstadoSessao>(rota, { signal })).data,
  });
  const acao = useMutation({
    mutationFn: async (caminho: string) => (await api.put<EstadoSessao>(`${rota}/${caminho}`)).data,
    onSuccess: async (estado, caminho) => {
      await cache.cancelQueries({ queryKey: chave });
      cache.setQueryData(chave, estado);
      void cache.invalidateQueries({ queryKey: ["sessoes", aluno] });
      if (caminho === "interromper" || caminho === "encerrar") router.replace("/aluno");
    },
    onError: () => { void consulta.refetch(); },
  });

  const envio = useMutation({
    mutationFn: async ({ questaoId, alternativa }: { questaoId: number; alternativa: Alternativa }) =>
      (await api.put<FeedbackResposta>(`${rota}/questoes/${questaoId}/resposta`, { alternativa })).data,
    onSuccess: async (feedback, { questaoId }) => {
      await cache.cancelQueries({ queryKey: chave });
      cache.setQueryData<EstadoSessao>(chave, (estado) =>
        estado?.questaoAtual?.id === questaoId ? { ...estado, feedback } : estado);
    },
    onError: () => { void consulta.refetch(); },
  });

  if (consulta.isPending) return <p role="status">Carregando a sessão…</p>;
  if (!consulta.data) return (
    <Card><CardContent className="space-y-4 pt-6" role="alert">
      <p>{mensagemErro(consulta.error)}</p>
      <div className="flex flex-wrap gap-2"><Button onClick={() => consulta.refetch()}>Tentar novamente</Button><Voltar /></div>
    </CardContent></Card>
  );

  const { sessao, questaoAtual } = consulta.data;
  const assunto = assuntos.data?.find((a) => a.id === sessao.assuntoId);
  const posicao = sessao.questoesApresentadas;
  const esgotada = semQuestoes(acao.error);
  const erro = acao.isError && !esgotada ? acao.error : consulta.error;
  const bloqueada = acao.isPending || envio.isPending;
  const falhaEnvio = envio.isError && !consulta.data.feedback && envio.variables?.questaoId === questaoAtual?.id;
  const fim = posicao >= limiteQuestoes || esgotada || !questaoAtual;

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2 text-indigo-700">
          <BookOpen size={20} aria-hidden="true" />
          <span className="font-semibold">{assunto?.materia ?? "Sessão de estudo"}</span>
        </div>
        {sessao.situacao === "ATIVA" && <Button size="sm" variant="outline" disabled={bloqueada}
          onClick={() => acao.mutate("interromper")}><Pause size={15} /> Pausar e sair</Button>}
      </div>
      <Card>
        <CardHeader className="gap-3">
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 first-letter:uppercase">
            {assunto?.nome ?? "Atividade"}
          </h1>
          <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-slate-600">
            <span aria-live="polite">Questão {posicao} de até {limiteQuestoes}</span>
            {questaoAtual && <Badge variant="secondary">Dificuldade {questaoAtual.dificuldade}</Badge>}
          </div>
          <Progress value={100 * posicao / limiteQuestoes} className="h-2" aria-label="Questões apresentadas"
            aria-valuenow={posicao} aria-valuemin={0} aria-valuemax={limiteQuestoes} />
        </CardHeader>
        <CardContent className="space-y-5">
          {erro && <p role="alert" className="rounded-xl bg-amber-50 p-3 text-sm text-amber-900">{mensagemErro(erro)}</p>}
          {sessao.situacao === "ENCERRADA" ? (
            <><p>Sessão encerrada.</p><Voltar /></>
          ) : sessao.situacao === "INTERROMPIDA" ? (
            <><p>Sessão pausada. Sua posição está salva.</p>
              <div className="flex flex-wrap gap-2"><Button disabled={bloqueada} onClick={() => acao.mutate("retomar")}>
                Retomar sessão</Button><Voltar /></div></>
          ) : (
            <>
              {questaoAtual ? (
                <>
                  <h2 aria-live="polite" className="break-words text-xl font-bold leading-snug text-slate-900 sm:text-2xl">
                    {questaoAtual.enunciado}
                  </h2>
                  <Alternativas key={questaoAtual.id} questao={questaoAtual} feedback={consulta.data.feedback}
                    bloqueadas={bloqueada} enviando={envio.isPending}
                    enviar={(alternativa) => envio.mutate({ questaoId: questaoAtual.id, alternativa })} />
                  {falhaEnvio && <p role="alert" className="text-sm text-amber-900">
                    {mensagemErro(envio.error)} Confira a questão e tente enviar novamente.
                  </p>}
                </>
              ) : <p role="status">Não há questão disponível nesta sessão.</p>}
              {esgotada && <p role="status" className="rounded-xl bg-indigo-50 p-3 text-sm text-indigo-900">
                Você chegou à última questão disponível deste assunto.
              </p>}
              {(!questaoAtual || consulta.data.feedback) && <div className="flex justify-end border-t border-slate-100 pt-4">
                <Button className="w-full sm:w-auto" disabled={bloqueada}
                  onClick={() => acao.mutate(fim ? "encerrar" : `questoes/${posicao + 1}`)}>
                  {bloqueada ? "Aguarde…" : fim ? "Encerrar sessão" : "Próxima questão"} <ArrowRight size={16} />
                </Button>
              </div>}
            </>
          )}
        </CardContent>
      </Card>
    </>
  );
}
