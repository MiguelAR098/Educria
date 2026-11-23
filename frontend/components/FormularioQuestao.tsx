"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import { AssuntoSimples, QuestaoDoProfessor, QuestaoPayload } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

const LETRAS = ["A", "B", "C", "D"] as const;

const campo =
  "w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus-visible:outline focus-visible:outline-2 focus-visible:outline-indigo-600";

/** Formulário de cadastro e edição de questão de múltipla escolha. */
export function FormularioQuestao({
  assuntos,
  inicial,
  assuntoPadrao,
  enviando,
  rotuloBotao,
  onSalvar,
  onCancelar,
}: {
  assuntos: AssuntoSimples[];
  inicial?: QuestaoDoProfessor;
  assuntoPadrao?: number;
  enviando: boolean;
  rotuloBotao: string;
  onSalvar: (payload: QuestaoPayload) => void;
  onCancelar?: () => void;
}) {
  const [assuntoId, setAssuntoId] = useState<number>(inicial?.assuntoId ?? assuntoPadrao ?? assuntos[0]?.id ?? 0);
  const [dificuldade, setDificuldade] = useState<number>(inicial?.dificuldade ?? 1);
  const [enunciado, setEnunciado] = useState(inicial?.enunciado ?? "");
  const [alternativas, setAlternativas] = useState<string[]>([
    inicial?.alternativaA ?? "",
    inicial?.alternativaB ?? "",
    inicial?.alternativaC ?? "",
    inicial?.alternativaD ?? "",
  ]);
  const [correta, setCorreta] = useState<string>(inicial?.respostaCorreta ?? "A");
  const [explicacao, setExplicacao] = useState(inicial?.explicacao ?? "");

  function submeter(evento: React.FormEvent) {
    evento.preventDefault();
    if (!enunciado.trim() || alternativas.some((a) => !a.trim())) {
      toast.error("Preencha o enunciado e as quatro alternativas.");
      return;
    }
    onSalvar({
      assuntoId,
      dificuldade,
      enunciado: enunciado.trim(),
      alternativaA: alternativas[0].trim(),
      alternativaB: alternativas[1].trim(),
      alternativaC: alternativas[2].trim(),
      alternativaD: alternativas[3].trim(),
      respostaCorreta: correta,
      explicacao: explicacao.trim() || undefined,
    });
  }

  return (
    <form onSubmit={submeter} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="q-assunto">Assunto</Label>
          <select
            id="q-assunto"
            value={assuntoId}
            onChange={(e) => setAssuntoId(Number(e.target.value))}
            className={campo}
          >
            {assuntos.map((a) => (
              <option key={a.id} value={a.id}>
                {a.nome}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-2">
          <Label htmlFor="q-dificuldade">Dificuldade (1 = mais fácil, 10 = mais difícil)</Label>
          <select
            id="q-dificuldade"
            value={dificuldade}
            onChange={(e) => setDificuldade(Number(e.target.value))}
            className={campo}
          >
            {Array.from({ length: 10 }, (_, i) => i + 1).map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="q-enunciado">Enunciado</Label>
        <textarea
          id="q-enunciado"
          value={enunciado}
          rows={3}
          maxLength={2000}
          onChange={(e) => setEnunciado(e.target.value)}
          className={campo}
        />
      </div>

      <fieldset className="space-y-2">
        <legend className="text-sm font-medium text-slate-900">
          Alternativas (marque a correta)
        </legend>
        {LETRAS.map((letra, i) => (
          <div key={letra} className="flex items-center gap-3">
            <input
              type="radio"
              name="q-correta"
              id={`q-correta-${letra}`}
              checked={correta === letra}
              onChange={() => setCorreta(letra)}
              aria-label={`Alternativa ${letra} é a correta`}
              className="h-4 w-4 accent-indigo-600"
            />
            <span className="w-4 text-sm font-bold text-slate-700">{letra}</span>
            <Input
              value={alternativas[i]}
              maxLength={500}
              aria-label={`Texto da alternativa ${letra}`}
              onChange={(e) =>
                setAlternativas((atual) => atual.map((t, j) => (j === i ? e.target.value : t)))
              }
            />
          </div>
        ))}
      </fieldset>

      <div className="space-y-2">
        <Label htmlFor="q-explicacao">Explicação mostrada ao aluno (opcional)</Label>
        <Input
          id="q-explicacao"
          value={explicacao}
          maxLength={1000}
          onChange={(e) => setExplicacao(e.target.value)}
        />
      </div>

      <div className="flex flex-wrap gap-3">
        <Button type="submit" disabled={enviando || assuntos.length === 0}>
          {enviando ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Salvando…
            </>
          ) : (
            rotuloBotao
          )}
        </Button>
        {onCancelar && (
          <Button type="button" variant="outline" onClick={onCancelar}>
            Cancelar
          </Button>
        )}
      </div>
    </form>
  );
}
