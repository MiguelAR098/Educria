"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useRef, useState, type KeyboardEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, BookOpen, Check, Pause } from "lucide-react";
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
  const titulo = useRef<HTMLHeadingElement>(null);
  const grupo = useRef<HTMLFieldSetElement>(null);
  const retorno = useRef<HTMLDivElement>(null);
  const letras = ["A", "B", "C", "D"] as const;

  const respondida = feedback != null;
  useEffect(() => {
    // O componente tem a questão como chave, inclusive ao avançar pelo teclado.
    (respondida ? retorno.current : titulo.current)?.focus();
  }, [respondida]);

  function usarAtalho(evento: KeyboardEvent<HTMLElement>) {
    if (feedback || bloqueadas || evento.defaultPrevented || evento.repeat || evento.nativeEvent.isComposing ||
        evento.ctrlKey || evento.altKey || evento.metaKey) return;
    if (evento.target instanceof HTMLElement && (evento.target.isContentEditable ||
        evento.target.closest('input:not([type="radio"]), textarea, select, [role="textbox"], [role="combobox"]'))) return;
    const tecla = evento.key.toUpperCase();
    const letra = letras.find((opcao, indice) => tecla === opcao || tecla === String(indice + 1));
    if (!letra || !questao[`alternativa${letra}`]) return;
    evento.preventDefault();
    setMarcada(letra);
    grupo.current?.querySelector<HTMLInputElement>(`input[value="${letra}"]`)?.focus();
  }

  return (
    <section aria-labelledby="enunciado-atual" onKeyDown={usarAtalho} className="min-w-0 space-y-4">
      <h2 id="enunciado-atual" ref={titulo} tabIndex={-1}
        className="scroll-mt-24 break-words rounded text-xl font-bold leading-snug text-slate-900 focus-visible:outline-indigo-700 sm:text-2xl">
        {questao.enunciado}
      </h2>
      <fieldset ref={grupo} disabled={bloqueadas || !!feedback} aria-describedby={feedback ? undefined : "atalhos-questao"} className="min-w-0 space-y-2.5">
        <legend className="mb-3 text-sm text-slate-600">{feedback ? "Sua alternativa" : "Escolha uma alternativa e clique em Enviar resposta para ver a correção."}</legend>
        {!feedback && <p id="atalhos-questao" className="text-sm text-slate-700">
          Com o foco na questão, use A–D ou 1–4 para marcar. Use Tab para chegar a Enviar resposta.
        </p>}
        {letras.map((letra, indice) => {
          const texto = questao[`alternativa${letra}`];
          const selecionada = (feedback?.alternativa ?? marcada) === letra;
          return (
            <label key={letra} className="block">
              <input type="radio" name="alternativa" value={letra} checked={selecionada} aria-keyshortcuts={`${letra} ${indice + 1}`}
                disabled={!texto} onChange={() => setMarcada(letra)} className="peer sr-only" />
              <span className="flex min-h-14 cursor-pointer items-center gap-3 rounded-xl border border-slate-500 bg-white px-3 py-3 text-slate-800 hover:border-indigo-300 peer-checked:border-indigo-600 peer-checked:bg-indigo-50 peer-focus-visible:ring-2 peer-focus-visible:ring-indigo-700 peer-focus-visible:ring-offset-2 peer-disabled:cursor-default sm:px-4">
                <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg border border-slate-200 bg-white text-sm font-bold text-indigo-700">{letra}</span>
                <span className="min-w-0 break-words">{texto || "Alternativa indisponível"}</span>
                {selecionada && <Check aria-hidden="true" className="ml-auto h-5 w-5 shrink-0 text-indigo-800" />}
              </span>
            </label>
          );
        })}
      </fieldset>
      <span role="status" className="sr-only">{enviando ? "Enviando sua resposta…" : ""}</span>
      {feedback ? (
        <div ref={retorno} tabIndex={-1} aria-label="Correção da resposta" role="group" className="scroll-mt-24 space-y-1 rounded-xl focus-visible:outline-indigo-700 bg-indigo-50 p-4 text-slate-800">
          <p role="status" className="font-semibold">{feedback.correta ? "Você acertou!" : "Resposta incorreta."}</p>
          {feedback.nivelAnterior != null && feedback.nivelAtual != null && (
            <p>Nível neste assunto: {feedback.nivelAnterior} → {feedback.nivelAtual}</p>
          )}
          {feedback.explicacao && <p>{feedback.explicacao}</p>}
        </div>
      ) : (
        <Button type="button" className="w-full disabled:bg-slate-200 disabled:text-slate-700 disabled:opacity-100 sm:w-auto" disabled={!marcada || bloqueadas}
          onClick={() => marcada && enviar(marcada)}>{enviando ? "Enviando…" : "Enviar resposta"}</Button>
      )}
    </section>
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
      void cache.invalidateQueries({ queryKey: ["historico-sessoes", aluno] });
      void cache.invalidateQueries({ queryKey: ["resultado-sessao", aluno, id] });
      cache.setQueryData(chave, estado);
      void cache.invalidateQueries({ queryKey: ["sessoes", aluno] });
      if (caminho === "interromper") router.replace("/aluno");
      if (caminho === "encerrar") router.replace(`/aluno/historico/${id}`);
    },
    onError: () => { void consulta.refetch(); },
  });

  const envioEmCurso = useRef(false);
  const envio = useMutation({
    mutationFn: async ({ questaoId, alternativa }: { questaoId: number; alternativa: Alternativa }) =>
      (await api.put<FeedbackResposta>(`${rota}/questoes/${questaoId}/resposta`, { alternativa })).data,
    onSuccess: async (feedback, { questaoId }) => {
      await cache.cancelQueries({ queryKey: chave });
      cache.setQueryData<EstadoSessao>(chave, (estado) =>
        estado?.questaoAtual?.id === questaoId ? { ...estado, feedback } : estado);
      void cache.invalidateQueries({ queryKey: ["historico-sessoes", aluno] });
      void cache.invalidateQueries({ queryKey: ["resultado-sessao", aluno, id] });
      void cache.invalidateQueries({ queryKey: ["catalogo", "assuntos", aluno] });
      void cache.invalidateQueries({ queryKey: ["assuntos-de-estudo", aluno] });
      void cache.invalidateQueries({ queryKey: ["revisoes", aluno] });
    },
    onError: () => { void consulta.refetch(); },
    onSettled: () => { envioEmCurso.current = false; },
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
        {sessao.situacao === "ATIVA" && <Button size="sm" className="min-h-11" variant="outline" disabled={bloqueada}
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
            <><p>Sessão encerrada.</p><Button asChild><Link href={`/aluno/historico/${id}`}>Ver resultado</Link></Button></>
          ) : sessao.situacao === "INTERROMPIDA" ? (
            <><p>Sessão pausada. Sua posição está salva.</p>
              <div className="flex flex-wrap gap-2"><Button disabled={bloqueada} onClick={() => acao.mutate("retomar")}>
                Retomar sessão</Button><Voltar /></div></>
          ) : (
            <>
              {questaoAtual ? (
                <>
                  <Alternativas key={questaoAtual.id} questao={questaoAtual} feedback={consulta.data.feedback}
                    bloqueadas={bloqueada} enviando={envio.isPending}
                    enviar={(alternativa) => {
                      if (envioEmCurso.current || bloqueada || consulta.data?.feedback) return;
                      envioEmCurso.current = true;
                      envio.mutate({ questaoId: questaoAtual.id, alternativa });
                    }} />
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
