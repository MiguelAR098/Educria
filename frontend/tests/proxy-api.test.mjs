import assert from "node:assert/strict";
import { once } from "node:events";
import { createServer } from "node:http";
import { afterEach, test } from "node:test";
import { GET, OPTIONS, POST } from "../app/api/[...path]/route.ts";

const configuracaoOriginal = {
  EDUCRIA_BACKEND_URL: process.env.EDUCRIA_BACKEND_URL,
  EDUCRIA_MODO_APRESENTACAO: process.env.EDUCRIA_MODO_APRESENTACAO,
};
const frontend = "http://127.0.0.1:3000";

afterEach(() => {
  for (const [variavel, valor] of Object.entries(configuracaoOriginal)) {
    if (valor === undefined) delete process.env[variavel];
    else process.env[variavel] = valor;
  }
});

async function simularBackend(t, porta) {
  const recebidas = [];
  const servidor = createServer(async (request, response) => {
    request.setEncoding("utf8");
    let corpo = "";
    for await (const trecho of request) corpo += trecho;
    recebidas.push({ metodo: request.method, caminho: request.url, headers: request.headers, corpo });

    response.writeHead(request.method === "POST" ? 201 : 200, {
      "content-type": "application/json",
      "www-authenticate": "Bearer",
      allow: "GET, POST",
      "set-cookie": "teste=ignorado",
    });
    response.end(JSON.stringify({ porta }));
  });

  const pronto = once(servidor, "listening");
  servidor.listen(porta, "127.0.0.1");
  await pronto;
  t.after(() => new Promise((resolve) => servidor.close(resolve)));
  return recebidas;
}

test("usa a porta 8080 por padrão", async (t) => {
  delete process.env.EDUCRIA_BACKEND_URL;
  delete process.env.EDUCRIA_MODO_APRESENTACAO;
  const recebidas = await simularBackend(t, 8080);
  const caminho = "/api/aluno/evolucao?assuntoId=123";
  const resposta = await GET(new Request(frontend + caminho));

  assert.equal(resposta.status, 200);
  assert.deepEqual(await resposta.json(), { porta: 8080 });
  assert.equal(recebidas.length, 1);
  assert.equal(recebidas[0].metodo, "GET");
  assert.equal(recebidas[0].caminho, caminho);
});

test("usa a porta 8085 configurada e preserva o contrato de encaminhamento", async (t) => {
  process.env.EDUCRIA_BACKEND_URL = "http://127.0.0.1:8085";
  delete process.env.EDUCRIA_MODO_APRESENTACAO;
  const recebidas = await simularBackend(t, 8085);
  const caminho = "/api/auth/login?destino=http%3A%2F%2F127.0.0.1%3A8080";
  const corpo = JSON.stringify({ email: "aluno@example.test" });
  const resposta = await POST(new Request(frontend + caminho, {
    method: "POST",
    body: corpo,
    headers: {
      authorization: "Bearer token-sintetico",
      "content-type": "application/json",
      accept: "application/json",
      origin: "https://tunel.example.test",
      "x-educria-client": "web",
      "x-educria-backend-url": "http://127.0.0.1:8080",
      cookie: "teste=ignorado",
    },
  }));

  assert.equal(resposta.status, 201);
  assert.deepEqual(await resposta.json(), { porta: 8085 });
  assert.equal(resposta.headers.get("cache-control"), "no-store");
  assert.equal(resposta.headers.get("content-type"), "application/json");
  assert.equal(resposta.headers.get("www-authenticate"), "Bearer");
  assert.equal(resposta.headers.get("allow"), "GET, POST");
  assert.equal(resposta.headers.get("set-cookie"), null);
  assert.equal(recebidas.length, 1);
  const [encaminhada] = recebidas;
  assert.equal(encaminhada.metodo, "POST");
  assert.equal(encaminhada.caminho, caminho);
  assert.equal(encaminhada.corpo, corpo);
  assert.equal(encaminhada.headers.authorization, "Bearer token-sintetico");
  assert.equal(encaminhada.headers["content-type"], "application/json");
  assert.equal(encaminhada.headers.accept, "application/json");
  for (const nome of ["origin", "x-educria-client", "x-educria-backend-url", "cookie"]) {
    assert.equal(encaminhada.headers[nome], undefined);
  }
});

test("recusa origem externa e preflight sem consultar o backend", async (t) => {
  const buscar = t.mock.method(globalThis, "fetch", async () => new Response());
  process.env.EDUCRIA_BACKEND_URL = "http://api:8080";
  process.env.EDUCRIA_MODO_APRESENTACAO = "1";
  const url = frontend + "/api/auth/login";
  const origem = "https://externa.example.test";
  const externo = await GET(new Request(url, { headers: { origin: origem } }));
  const preflight = await OPTIONS(new Request(url, {
    method: "OPTIONS",
    headers: { origin: origem, "access-control-request-method": "POST", "x-educria-client": "web" },
  }));

  for (const resposta of [externo, preflight]) {
    assert.equal(resposta.status, 403);
    assert.equal(resposta.headers.get("access-control-allow-origin"), null);
  }
  assert.equal(buscar.mock.callCount(), 0);
});

test("aceita somente a URL interna exata no modo apresentação", async (t) => {
  const buscar = t.mock.method(globalThis, "fetch", async () => new Response(null, {
    status: 302,
    headers: { location: "https://externa.example.test" },
  }));
  const registrar = t.mock.method(console, "error", () => {});
  const caminho = "/api/aluno/evolucao?assuntoId=123&modo=1";
  const requisitar = () => GET(new Request(frontend + caminho, {
    headers: { "x-educria-modo-apresentacao": "1" },
  }));

  process.env.EDUCRIA_BACKEND_URL = "http://api:8080";
  delete process.env.EDUCRIA_MODO_APRESENTACAO;
  assert.equal((await requisitar()).status, 500);
  process.env.EDUCRIA_MODO_APRESENTACAO = "true";
  assert.equal((await requisitar()).status, 500);

  process.env.EDUCRIA_MODO_APRESENTACAO = "1";
  const invalidas = [
    "http://externa.example.test:8085",
    "https://127.0.0.1:8085",
    "http://127.0.0.1:0",
    "http://localhost:65536",
    "http://localhost",
    "http://127.0.0.1:8085/outro-caminho",
    "http://127.0.0.1:8085@externa.example.test",
    "http://api:8080/",
    "http://api:8080/admin",
    "http://api:8080?destino=local",
    "http://api:8080#rota",
    "http://api:8081",
    "http://api:08080",
    "http://api.example.test:8080",
    "http://usuario:senha-sintetica@api:8080",
    "https://api:8080",
    "",
  ];

  for (const valor of invalidas) {
    process.env.EDUCRIA_BACKEND_URL = valor;
    const resposta = await requisitar();
    assert.equal(resposta.status, 500);
    assert.equal((await resposta.json()).message, "Configuração interna da API EDUCRIA inválida");
  }

  assert.equal(buscar.mock.callCount(), 0);
  assert.equal(registrar.mock.callCount(), invalidas.length + 2);

  process.env.EDUCRIA_BACKEND_URL = "http://api:8080";
  const resposta = await requisitar();
  assert.equal(resposta.status, 302);
  assert.equal(resposta.headers.get("location"), null);
  assert.equal(buscar.mock.callCount(), 1);
  const [[destino, opcoes]] = buscar.mock.calls.map((chamada) => chamada.arguments);
  assert.equal(destino.href, "http://api:8080" + caminho);
  assert.equal(opcoes.redirect, "manual");
});
