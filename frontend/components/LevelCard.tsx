import { Trophy } from "lucide-react";
import type { DesempenhoAluno } from "@/lib/types";
import styles from "./LevelCard.module.css";

export function LevelCard({ desempenho }: { desempenho: DesempenhoAluno }) {
  return (
    <section aria-labelledby="nivel-global" className={styles.card}>
      <div className={styles.copy}>
        <h2 id="nivel-global" className="text-sm font-semibold text-indigo-100">Seu nível global</h2>
        <p className="mt-2 text-3xl font-extrabold tracking-tight sm:text-4xl">
          Nível {desempenho.nivelAtual} <span className="text-lg font-medium text-indigo-100">de 10</span>
        </p>
        <a href="#titulo-materias" className="mt-6 inline-block rounded-sm text-sm font-semibold text-white underline decoration-white/50 underline-offset-4 hover:decoration-white">
          Ver evolução por assunto
        </a>
      </div>
      <div className={styles.art} aria-hidden="true">
        <div className={styles.orbit} />
        <div className={styles.ribbon} />
        <div className={styles.seal}><div className={styles.face}><Trophy size={42} strokeWidth={1.8} /></div></div>
      </div>
    </section>
  );
}
