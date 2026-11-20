"use client";

import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { SlidersHorizontal, X } from "lucide-react";
import { chaveTamanho, tamanhos, Tamanho, tamanhoValido } from "@/lib/tamanho-texto";

export function TamanhoTexto() {
  const painel = useRef<HTMLDialogElement>(null);
  const botao = useRef<HTMLButtonElement>(null);
  const [tamanho, setTamanho] = useState<Tamanho>("Normal");

  useEffect(() => {
    setTamanho(tamanhoValido(document.documentElement.dataset.tamanhoTexto));

    function sincronizar(evento: StorageEvent) {
      if (evento.storageArea !== localStorage || (evento.key !== null && evento.key !== chaveTamanho)) return;
      const valor = tamanhoValido(evento.newValue);
      document.documentElement.dataset.tamanhoTexto = valor;
      setTamanho(valor);
    }

    window.addEventListener("storage", sincronizar);
    return () => window.removeEventListener("storage", sincronizar);
  }, []);

  function escolher(valor: Tamanho) {
    document.documentElement.dataset.tamanhoTexto = valor;
    setTamanho(valor);
    try {
      localStorage.setItem(chaveTamanho, valor);
    } catch {
      // Com armazenamento bloqueado, a escolha vale durante a navegação nesta aba.
    }
  }

  return (
    <div className="mx-auto flex max-w-5xl justify-end px-4">
      <Button ref={botao} type="button" variant="ghost" size="icon"
        className="h-10 w-10 text-slate-600 hover:text-indigo-700"
        aria-label="Configurações de leitura" title="Configurações de leitura"
        aria-haspopup="dialog" aria-controls="painel-leitura" onClick={() => painel.current?.showModal()}>
        <SlidersHorizontal size={18} aria-hidden="true" />
      </Button>
      <dialog ref={painel} id="painel-leitura" aria-labelledby="titulo-leitura"
        onClose={() => botao.current?.focus({ preventScroll: true })}
        className="m-auto max-h-[90dvh] w-80 max-w-[calc(100%-2rem)] overflow-auto rounded-2xl border border-slate-200 bg-white p-5 text-slate-800 shadow-xl backdrop:bg-slate-900/30">
        <div className="mb-4 flex items-start justify-between gap-2">
          <h2 id="titulo-leitura" className="text-lg font-bold">Configurações de leitura</h2>
          <Button type="button" variant="ghost" size="icon" className="h-9 w-9 shrink-0"
            aria-label="Fechar configurações de leitura" onClick={() => painel.current?.close()}>
            <X size={18} aria-hidden="true" />
          </Button>
        </div>
        <label htmlFor="tamanho-texto" className="mb-2 block text-sm font-semibold">Tamanho do texto</label>
        <select id="tamanho-texto" value={tamanho} onChange={(e) => escolher(tamanhoValido(e.target.value))}
          className="min-h-11 w-full rounded-lg border border-slate-400 bg-white px-3 focus-visible:outline-indigo-700">
          {tamanhos.map((opcao) => <option key={opcao}>{opcao}</option>)}
        </select>
      </dialog>
    </div>
  );
}
