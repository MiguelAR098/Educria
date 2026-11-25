"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, BookOpen, Check, Pause } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { api } from "@/lib/api";
import { chaveEvolucao } from "@/lib/evolucao";
import { Alternativa, EstadoSessao, FeedbackResposta, limiteQuestoes, mensagemErro, QuestaoAtual, rotaSessoes, semQuestoes,
  useAssuntosDeEstudo, useChaveAluno } from "@/lib/estudo";
import { formatarPontos } from "@/lib/pontuacao";
import estilos from "./leitura.module.css";

const letras = ["A", "B", "C", "D"] as const;

const orientacoes = {
  DICA_INICIAL: { titulo: "Dica para tentar novamente", cor: "bg-amber-50" },
  DICA_APRIMORADA: { titulo: "Vamos por partes", cor: "bg-indigo-50" },
  RESOLUCAO: { titulo: "Entenda a resposta", cor: "bg-violet-50" },
};

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

  const respondida = feedback != null;
  const orientacao = feedback && !feedback.correta && feedback.tipoOrientacao !== "EXPLICACAO"
    ? orientacoes[feedback.tipoOrientacao ?? "DICA_INICIAL"] : null;
  const revelou = orientacao && feedback?.tipoOrientacao === "RESOLUCAO";
  useEffect(() => {
    // O componente tem a questão como chave, inclusive ao avançar pelo teclado.
    (respondida ? retorno.current : titulo.current)?.focus();
  }, [respondida]);

  useEffect(() => {
    if (respondida || bloqueadas) return;
    function usarAtalho(evento: KeyboardEvent) {
      if (evento.defaultPrevented || evento.repeat || evento.isComposing ||
          evento.ctrlKey || evento.altKey || evento.metaKey) return;
      if (evento.target instanceof HTMLElement && (evento.target.isContentEditable ||
          evento.target.closest('input:not([type="radio"]), textarea, select, [role="textbox"], [role="combobox"], [role="dialog"], [aria-modal="true"]'))) return;
      const tecla = evento.key.toUpperCase();
      const letra = letras.find((opcao, indice) => tecla === opcao || tecla === String(indice + 1));
      if (!letra || !questao[`alternativa${letra}`]) return;
      evento.preventDefault();
      setMarcada(letra);
      grupo.current?.querySelector<HTMLInputElement>(`input[value="${letra}"]`)?.focus();
    }
    document.addEventListener("keydown", usarAtalho);
    return () => document.removeEventListener("keydown", usarAtalho);
  }, [respondida, bloqueadas, questao]);

  return (
    <section aria-labelledby="enunciado-atual" className="min-w-0 space-y-4">
      <h2 id="enunciado-atual" ref={titulo} tabIndex={-1}
        className="scroll-mt-24 break-words rounded text-xl font-bold leading-snug text-slate-900 focus-visible:outline-indigo-700 sm:text-2xl">
        {questao.enunciado}
      </h2>
      <fieldset ref={grupo} disabled={bloqueadas || !!feedback} className="min-w-0 space-y-2.5">
        <legend className="mb-3 text-sm text-slate-600">{feedback ? "Sua alternativa" : "Clique em uma alternativa ou use A–D ou 1–4 para marcar."}</legend>
        {letras.map((letra, indice) => {
          const texto = questao[`alternativa${letra}`];
          const selecionada = (feedback?.alternativa ?? marcada) === letra;
          return (
            <label key={letra} className="block">
              <input type="radio" name="alternativa" value={letra} checked={selecionada} aria-keyshortcuts={!feedback ? `${letra} ${indice + 1}` : undefined}
                disabled={!texto} onChange={() => setMarcada(letra)} className="peer sr-only" />
              <span className="flex min-h-14 cursor-pointer items-center gap-3 rounded-xl border border-slate-500 bg-white px-3 py-3 text-slate-800 hover:border-indigo-300 peer-checked:border-indigo-600 peer-checked:bg-indigo-50 peer-focus-visible:ring-2 peer-focus-visible:ring-indigo-700 peer-focus-visible:ring-offset-2 peer-disabled:cursor-default sm:px-4">
                <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg border border-slate-200 bg-white text-sm font-bold text-indigo-700">{letra}</span>
                <span className={estilos.alternativa}>{texto || "Alternativa indisponível"}</span>
                {selecionada && <Check aria-hidden="true" className="ml-auto h-[20px] w-[20px] shrink-0 text-indigo-800" />}
              </span>
            </label>
          );
        })}
      </fieldset>
      <span role="status" className="sr-only">{enviando ? "Enviando sua resposta…" : ""}</span>
      {feedback ? (
        <div ref={retorno} tabIndex={-1} aria-label="Correção da resposta" role="group" className={`${estilos.retorno} scroll-mt-24 space-y-2 rounded-xl p-4 text-slate-900 focus-visible:outline-indigo-700 ${feedback.correta ? "bg-emerald-50" : orientacao?.cor ?? "bg-amber-50"}`}>
          <p role="status" className="font-semibold">{feedback.correta ? "Você acertou!" : "Resposta incorreta."}</p>
          {feedback.pontosConcedidos != null && (feedback.pontosConcedidos > 0
            ? <p className="font-semibold text-emerald-900">+{formatarPontos(feedback.pontosConcedidos)}</p>
            : <p className="text-sm text-slate-600">Nenhum ponto nesta resposta.</p>)}
          {feedback.nivelAnterior != null && feedback.nivelAtual != null && (
            <p>Nível neste assunto: {feedback.nivelAnterior} → {feedback.nivelAtual}</p>
          )}
          {orientacao && <h3 className="font-bold">{orientacao.titulo}</h3>}
          {revelou && (feedback.errosDaQuestao === 3
            ? <p>Esta foi sua terceira tentativa incorreta. Vamos revisar o caminho juntos.</p>
            : <p>Vamos rever a resolução para você tentar de novo.</p>)}
          {revelou && feedback.alternativaCorreta && feedback.textoAlternativaCorreta && (
            <p className="break-words rounded-lg bg-white/80 p-3 font-semibold">
              Alternativa correta: {feedback.alternativaCorreta} — {feedback.textoAlternativaCorreta}
            </p>
          )}
          {feedback.explicacao && <p>{feedback.explicacao}</p>}
          {feedback.reaprender && <p>Esta questão voltará nesta sessão para uma nova tentativa.</p>}
          {feedback.correta && <p>Próxima revisão agendada.</p>}
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
    mutationFn: async ({ questaoId, alternativa, passo }: { questaoId: number; alternativa: Alternativa; passo?: number }) =>
      (await api.put<FeedbackResposta>(`${rota}/questoes/${questaoId}/resposta`, { alternativa, passo })).data,
    onSuccess: async (feedback, { questaoId, passo }) => {
      await cache.cancelQueries({ queryKey: chave });
      cache.setQueryData<EstadoSessao>(chave, (estado) =>
        estado?.questaoAtual?.id === questaoId && estado.passo === passo && !estado.feedback
          ? { ...estado, feedback } : estado);
      await cache.invalidateQueries({ queryKey: chave, exact: true });
      void cache.invalidateQueries({ queryKey: ["historico-sessoes", aluno] });
      void cache.invalidateQueries({ queryKey: [chaveEvolucao, aluno] });
      void cache.invalidateQueries({ queryKey: ["aluno-desempenho", aluno] });
      void cache.invalidateQueries({ queryKey: ["resultado-sessao", aluno] });
      void cache.invalidateQueries({ queryKey: ["catalogo", "materias", aluno] });
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

  const { sessao, questaoAtual, pontosSessao } = consulta.data;
  const assunto = assuntos.data?.find((a) => a.id === sessao.assuntoId);
  const posicao = sessao.questoesApresentadas;
  const esgotada = semQuestoes(acao.error);
  const erro = acao.isError && !esgotada ? acao.error : consulta.error;
  const bloqueada = acao.isPending || envio.isPending;
  const falhaEnvio = envio.isError && !consulta.data.feedback && envio.variables?.questaoId === questaoAtual?.id
    && envio.variables?.passo === consulta.data.passo;
  const { passo, pendencias = 0, repetindo } = consulta.data;
  const fim = pendencias === 0 && (posicao >= limiteQuestoes || esgotada || !questaoAtual);

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
      <Card className={estilos.leitura}>
        <CardHeader className="gap-3">
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 first-letter:uppercase">
            {assunto?.nome ?? "Atividade"}
          </h1>
          <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-slate-600">
            <span aria-live="polite">Questões distintas: {posicao} de até {limiteQuestoes} · Para reaprender: {pendencias}</span>
            {questaoAtual && <Badge variant="secondary">Dificuldade {questaoAtual.dificuldade}</Badge>}
          </div>
          {pontosSessao != null && <p className="text-sm font-semibold text-indigo-700" aria-live="polite">
            {formatarPontos(pontosSessao)} na sessão
          </p>}
          {repetindo && <p className={`${estilos.orientacao} font-semibold text-indigo-700`}>Nova tentativa de uma questão anterior</p>}
          <Progress value={100 * posicao / limiteQuestoes} className="h-[8px]" aria-label="Questões apresentadas"
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
                  <Alternativas key={`${questaoAtual.id}-${passo}`} questao={questaoAtual} feedback={consulta.data.feedback}
                    bloqueadas={bloqueada} enviando={envio.isPending}
                    enviar={(alternativa) => {
                      if (envioEmCurso.current || bloqueada || consulta.data?.feedback) return;
                      envioEmCurso.current = true;
                      envio.mutate({ questaoId: questaoAtual.id, alternativa, passo });
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
                  onClick={() => acao.mutate(fim ? "encerrar" : `questoes/${(passo ?? posicao) + 1}`)}>
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
