import assert from "node:assert/strict";
import { fork } from "node:child_process";
import { once } from "node:events";
import { cpSync, mkdirSync, mkdtempSync, realpathSync, rmSync, symlinkSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import { tmpdir } from "node:os";
import { basename, dirname, join } from "node:path";
import { after, before, test } from "node:test";
import { fileURLToPath } from "node:url";

// Execute com node --test frontend/tests/cache-sessao.browser.mjs.
// PLAYWRIGHT_CORE_PATH aceita o caminho de uma instalação existente.
// CHROMIUM_BIN é opcional quando o Playwright já tem Chromium instalado.
const require = createRequire(new URL("../package.json", import.meta.url));
const frontend = fileURLToPath(new URL("..", import.meta.url));
const contas = {
  A: { id: 1, nome: "A", email: "a@example.test", role: "ADMIN" },
  B: { id: 2, nome: "B", email: "b@example.test", role: "ADMIN" },
};
const limite = { timeout: 90_000 };
const layout = `
import { Providers } from "@/components/providers";

export default function Layout({ children }: { children: React.ReactNode }) {
  return <html><body><Providers>{children}</Providers></body></html>;
}
`;
const painel = `
"use client";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import type { User } from "@/lib/types";

const contas: Record<string, User> = ${JSON.stringify(contas)};

async function consultar(canal: string, caminho: string) {
  const token = useAuthStore.getState().token;
  try {
    return (await api.get(caminho)).data;
  } finally {
    window.dispatchEvent(new CustomEvent("consulta-concluida", { detail: { canal, token } }));
  }
}

export default function Painel() {
  const { user, token, _hasHydrated, setAuth, logout } = useAuthStore();
  const { data } = useQuery<string>({
    queryKey: ["admin", "turmas"],
    queryFn: () => consultar("turmas", "/api/admin/turmas"),
    enabled: _hasHydrated && Boolean(token),
  });
  return <main>
    <p data-testid="sessao">{_hasHydrated ? user?.nome ?? "Sem sessão" : "Hidratando"}</p>
    <p data-testid="turmas">{data ?? "Sem dados"}</p>
    <button onClick={() => setAuth("sintetico-A", contas.A)}>Conta A</button>
    <button onClick={() => setAuth("sintetico-B", contas.B)}>Conta B</button>
    <button onClick={logout}>Sair</button>
    <button onClick={() => { void consultar("avulsa", "/api/admin/avulsa").catch(() => {}); }}>
      Consulta avulsa
    </button>
  </main>;
}
`;
const inicializarNext = `
const { createServer } = require("node:http");
const { once } = require("node:events");

process.once("disconnect", () => process.exit(0));
(async () => {
  const servidor = createServer();
  const pronto = once(servidor, "listening");
  servidor.listen(0, "127.0.0.1");
  await pronto;
  const { port } = servidor.address();
  const app = require("next")({ dev: true, dir: __dirname, hostname: "127.0.0.1", port });
  await app.prepare();
  servidor.on("request", app.getRequestHandler());
  process.send("http://127.0.0.1:" + port);
})().catch((erro) => {
  console.error(erro);
  process.exit(1);
});
`;

let pasta;
let servidor;
let navegador;
let origem;

before(async () => {
  const { chromium } = require(process.env.PLAYWRIGHT_CORE_PATH ?? "playwright-core");
  process.env.NEXT_TELEMETRY_DISABLED = "1";
  pasta = mkdtempSync(join(tmpdir(), "educria-sessao-"));
  for (const item of ["components", "lib", "tsconfig.json", "package.json"]) {
    cpSync(join(frontend, item), join(pasta, item), { recursive: true });
  }
  symlinkSync(join(frontend, "node_modules"), join(pasta, "node_modules"), "junction");
  const rota = join(pasta, "app", "[[...destino]]");
  mkdirSync(rota, { recursive: true });
  writeFileSync(join(pasta, "app", "layout.tsx"), layout);
  writeFileSync(join(rota, "page.tsx"), painel);
  const scriptServidor = join(pasta, "iniciar.cjs");
  writeFileSync(scriptServidor, inicializarNext);

  servidor = fork(scriptServidor, { cwd: pasta, execArgv: [] });
  [origem] = await Promise.race([
    once(servidor, "message"),
    once(servidor, "exit").then(([codigo]) => { throw new Error(`Next encerrou antes dos testes (${codigo})`); }),
  ]);
  navegador = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN });

  const pagina = await navegador.newPage();
  try {
    const resposta = await pagina.goto(origem, { waitUntil: "domcontentloaded", timeout: 180_000 });
    assert.equal(resposta.status(), 200);
    await mostra(pagina, "sessao", "Sem sessão");
  } finally {
    await pagina.close();
  }
});

after(async () => {
  await navegador?.close();
  if (servidor?.connected) {
    const encerrou = once(servidor, "exit");
    servidor.disconnect();
    await encerrou;
  }
  if (pasta) {
    const caminho = realpathSync(pasta);
    assert.equal(dirname(caminho), realpathSync(tmpdir()));
    assert.match(basename(caminho), /^educria-sessao-/);
    rmSync(join(caminho, "node_modules"), { force: true });
    rmSync(caminho, { recursive: true, force: true });
  }
});

function pendente() {
  const recebeu = Promise.withResolvers();
  const liberar = Promise.withResolvers();
  return {
    recebido: recebeu.promise,
    liberar: liberar.resolve,
    responder: async (resposta) => {
      recebeu.resolve();
      await liberar.promise;
      return resposta;
    },
  };
}

async function abrir(t, respostas = {}) {
  const pagina = await navegador.newPage({ serviceWorkers: "block" });
  t.after(() => pagina.close());
  await pagina.addInitScript(() => {
    window.consultasConcluidas = [];
    window.addEventListener("consulta-concluida", (evento) => {
      window.consultasConcluidas.push(evento.detail);
    });
  });
  await pagina.route("**/api/**", async (route) => {
    const pedido = route.request();
    const credencial = pedido.headers().authorization;
    const conta = credencial === "Bearer sintetico-A" ? "A" : credencial === "Bearer sintetico-B" ? "B" : null;
    const caminho = new URL(pedido.url()).pathname;
    const responder = respostas[`${conta}${caminho}`];
    const padrao = { status: conta ? 200 : 401, json: conta ? `Turma privada de ${conta}` : {} };
    await route.fulfill(responder ? await responder(padrao) : padrao);
  });
  const resposta = await pagina.goto(origem, { waitUntil: "domcontentloaded", timeout: 60_000 });
  assert.equal(resposta.status(), 200);
  await mostra(pagina, "sessao", "Sem sessão");
  return pagina;
}

async function mostra(pagina, campo, valor) {
  await pagina.waitForFunction(
    ({ campo, valor }) => document.querySelector(`[data-testid="${campo}"]`)?.textContent === valor,
    { campo, valor }
  );
}

async function entrar(pagina, conta) {
  await pagina.getByRole("button", { name: `Conta ${conta}` }).click();
  await mostra(pagina, "sessao", conta);
}

async function concluiu(pagina, canal, conta) {
  await pagina.waitForFunction(({ canal, token }) => window.consultasConcluidas.some(
    (consulta) => consulta.canal === canal && consulta.token === token
  ), { canal, token: `sintetico-${conta}` });
}

test("logout de A e login de B carregam a mesma chave sem reutilizar dados privados", limite, async (t) => {
  const consultaB = pendente();
  const pagina = await abrir(t, { "B/api/admin/turmas": consultaB.responder });
  await entrar(pagina, "A");
  await mostra(pagina, "turmas", "Turma privada de A");
  await pagina.reload({ waitUntil: "domcontentloaded" });
  await mostra(pagina, "sessao", "A");
  await mostra(pagina, "turmas", "Turma privada de A");
  await pagina.getByRole("button", { name: "Sair" }).click();
  await mostra(pagina, "sessao", "Sem sessão");
  await mostra(pagina, "turmas", "Sem dados");

  await entrar(pagina, "B");
  await mostra(pagina, "turmas", "Sem dados");
  await consultaB.recebido;
  consultaB.liberar();
  await mostra(pagina, "turmas", "Turma privada de B");
});

test("setAuth troca A por B diretamente e descarta o resultado ainda fresco", limite, async (t) => {
  const consultaB = pendente();
  const pagina = await abrir(t, { "B/api/admin/turmas": consultaB.responder });
  await entrar(pagina, "A");
  await mostra(pagina, "turmas", "Turma privada de A");
  await entrar(pagina, "B");
  await mostra(pagina, "turmas", "Sem dados");
  await consultaB.recebido;
  consultaB.liberar();
  await mostra(pagina, "turmas", "Turma privada de B");
});

test("resposta 200 pendente da consulta de A chega depois de B e seus dados não reaparecem", limite, async (t) => {
  const consultaA = pendente();
  const pagina = await abrir(t, { "A/api/admin/turmas": consultaA.responder });
  await entrar(pagina, "A");
  await consultaA.recebido;
  await entrar(pagina, "B");
  await mostra(pagina, "turmas", "Turma privada de B");
  consultaA.liberar();
  await concluiu(pagina, "turmas", "A");
  await mostra(pagina, "turmas", "Turma privada de B");
});

test("401 atrasado preserva a sessão após troca ou novo login, e 401 atual encerra", limite, async (t) => {
  const consultaA = pendente();
  const consultaB = pendente();
  let consultouB = false;
  const rejeitar = { status: 401, json: {} };
  const pagina = await abrir(t, {
    "A/api/admin/avulsa": () => consultaA.responder(rejeitar),
    "B/api/admin/avulsa": () => {
      if (consultouB) return rejeitar;
      consultouB = true;
      return consultaB.responder(rejeitar);
    },
  });
  const consulta = pagina.getByRole("button", { name: "Consulta avulsa" });
  await entrar(pagina, "A");
  await consulta.click();
  await consultaA.recebido;
  await entrar(pagina, "B");
  await mostra(pagina, "turmas", "Turma privada de B");
  consultaA.liberar();
  await concluiu(pagina, "avulsa", "A");
  await mostra(pagina, "sessao", "B");
  assert.equal(new URL(pagina.url()).pathname, "/");

  await consulta.click();
  await consultaB.recebido;
  await pagina.getByRole("button", { name: "Sair" }).click();
  await mostra(pagina, "sessao", "Sem sessão");
  await entrar(pagina, "B");
  await mostra(pagina, "turmas", "Turma privada de B");
  consultaB.liberar();
  await concluiu(pagina, "avulsa", "B");
  await mostra(pagina, "sessao", "B");
  assert.equal(new URL(pagina.url()).pathname, "/");

  await consulta.click();
  await pagina.waitForURL("**/login", { waitUntil: "domcontentloaded" });
  await mostra(pagina, "sessao", "Sem sessão");
  await mostra(pagina, "turmas", "Sem dados");
});
