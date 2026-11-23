"use client";

import { useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { toast } from "sonner";
import { Copy, Download, Loader2, Upload } from "lucide-react";
import { api } from "@/lib/api";
import { ErroApi, mensagemDeErro } from "@/lib/erros";
import { AssuntoSimples, ResultadoImportacao } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

function exemplo(assunto: string): string {
  return JSON.stringify(
    [
      {
        conceito: assunto,
        dificuldade: 2,
        enunciado: "Quanto é 20% de 90?",
        alts: ["16", "18", "20", "22"],
        correta: 1,
        explicacao: "20% de 90 é 18.",
      },
    ],
    null,
    2
  );
}

function instrucoesParaIA(materia: string, assuntos: AssuntoSimples[]): string {
  return [
    `Crie questões de múltipla escolha de ${materia} para alunos do ensino fundamental.`,
    "Responda somente com um JSON: uma lista entre colchetes, sem texto antes ou depois.",
    "Cada item deve ter exatamente estes campos:",
    '- "conceito": um destes assuntos, escrito igual: ' + assuntos.map((a) => `"${a.nome}"`).join(", "),
    '- "dificuldade": número inteiro de 1 (mais fácil) a 10 (mais difícil)',
    '- "enunciado": o texto da pergunta',
    '- "alts": lista com exatamente 4 alternativas diferentes entre si',
    '- "correta": posição da alternativa correta, de 0 a 3 (0 = primeira)',
    '- "explicacao": uma frase curta explicando a resposta',
    "Exemplo de um item:",
    exemplo(assuntos[0]?.nome ?? "assunto").slice(1, -1).trim(),
    "Quantidade de questões: 10. Assunto: (escreva aqui o assunto desejado).",
  ].join("\n");
}

/** Exportação do banco e carga de questões em lote por JSON (colado ou de arquivo). */
export function ImportarExportarQuestoes({
  materia,
  assuntos,
  aoImportar,
}: {
  materia: string;
  assuntos: AssuntoSimples[];
  aoImportar: () => void;
}) {
  const [texto, setTexto] = useState("");
  const [aviso, setAviso] = useState<string | null>(null);
  const [resultado, setResultado] = useState<ResultadoImportacao | null>(null);
  const [exportando, setExportando] = useState(false);
  const arquivo = useRef<HTMLInputElement>(null);

  const importar = useMutation({
    mutationFn: async (lista: unknown[]) =>
      (await api.post<ResultadoImportacao>("/api/professor/questoes/importar", lista)).data,
    onSuccess: (dados) => {
      setResultado(dados);
      if (dados.importadas > 0) {
        setTexto("");
        aoImportar();
      }
    },
    onError: (error: AxiosError<ErroApi>) =>
      setAviso(mensagemDeErro(error, "Não foi possível importar. Tente novamente.")),
  });

  function enviar() {
    setAviso(null);
    setResultado(null);
    if (!texto.trim()) {
      setAviso("Cole o JSON ou escolha um arquivo antes de importar.");
      return;
    }
    let lista: unknown;
    try {
      lista = JSON.parse(texto);
    } catch {
      setAviso("O texto não é um JSON válido. Confira vírgulas e aspas.");
      return;
    }
    if (!Array.isArray(lista)) {
      setAviso("O JSON precisa ser uma lista entre colchetes.");
      return;
    }
    importar.mutate(lista);
  }

  async function lerArquivo(evento: React.ChangeEvent<HTMLInputElement>) {
    const escolhido = evento.target.files?.[0];
    if (!escolhido) return;
    setTexto(await escolhido.text());
    setAviso(null);
    setResultado(null);
    evento.target.value = "";
  }

  async function exportar() {
    setExportando(true);
    try {
      const { data } = await api.get<unknown[]>("/api/professor/questoes/exportar");
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "banco-de-questoes.json";
      link.click();
      URL.revokeObjectURL(url);
      toast.success(`${data.length} questões exportadas.`);
    } catch {
      toast.error("Não foi possível exportar. Tente novamente.");
    } finally {
      setExportando(false);
    }
  }

  async function copiarInstrucoes() {
    try {
      await navigator.clipboard.writeText(instrucoesParaIA(materia, assuntos));
      toast.success("Instruções copiadas. Cole em uma IA e peça as questões.");
    } catch {
      toast.error("Não foi possível copiar. Selecione o texto do exemplo manualmente.");
    }
  }

  return (
    <div className="space-y-6">
      <div className="space-y-2 text-sm text-slate-700">
        <p>
          Cada item precisa de <strong>conceito</strong> (um dos assuntos:{" "}
          {assuntos.map((a) => a.nome).join(", ") || "nenhum cadastrado"}), <strong>dificuldade</strong> de 1 a 10,{" "}
          <strong>enunciado</strong>, <strong>alts</strong> com quatro textos diferentes, <strong>correta</strong> de 0
          a 3 (ou a letra A a D) e <strong>explicacao</strong> (opcional). Enunciado repetido é ignorado e até 200
          questões por vez.
        </p>
        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="outline" size="sm" onClick={copiarInstrucoes}>
            <Copy className="mr-2 h-4 w-4" /> Copiar instruções para uma IA
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => setTexto(exemplo(assuntos[0]?.nome ?? "assunto"))}
          >
            Usar exemplo
          </Button>
          <Button type="button" variant="outline" size="sm" disabled={exportando} onClick={exportar}>
            {exportando ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Download className="mr-2 h-4 w-4" />}
            Exportar banco (JSON)
          </Button>
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="json-questoes">JSON das questões</Label>
        <textarea
          id="json-questoes"
          value={texto}
          rows={8}
          spellCheck={false}
          placeholder='[{"conceito":"porcentagem","dificuldade":2,"enunciado":"Quanto é 20% de 90?","alts":["16","18","20","22"],"correta":1,"explicacao":"20% de 90 é 18."}]'
          onChange={(e) => setTexto(e.target.value)}
          className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 font-mono text-xs text-slate-900 focus-visible:outline focus-visible:outline-2 focus-visible:outline-indigo-600"
        />
        <div className="flex flex-wrap items-center gap-3">
          <Button type="button" onClick={enviar} disabled={importar.isPending}>
            {importar.isPending ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Importando…
              </>
            ) : (
              <>
                <Upload className="mr-2 h-4 w-4" /> Importar
              </>
            )}
          </Button>
          <input
            ref={arquivo}
            type="file"
            accept="application/json,.json"
            onChange={lerArquivo}
            aria-label="Escolher arquivo JSON"
            className="text-sm text-slate-600"
          />
        </div>
        {aviso && <p role="alert" className="text-sm text-red-600">{aviso}</p>}
        {resultado && (
          <div role="status" className="space-y-2 rounded-lg bg-slate-50 p-3 text-sm text-slate-800">
            <p className="font-semibold">
              {resultado.importadas} importadas, {resultado.repetidas} repetidas, {resultado.recusadas} recusadas por
              formato.
            </p>
            {resultado.erros.length > 0 && (
              <ul className="list-disc space-y-1 pl-5 text-red-700">
                {resultado.erros.map((erro) => (
                  <li key={erro}>{erro}</li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
