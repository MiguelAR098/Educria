export const dynamic = "force-dynamic";
export const runtime = "nodejs";

function origemPermitida(origem: string) {
  if (origem === "http://localhost:3000" || origem === "http://127.0.0.1:3000") {
    return true;
  }
  const nome = process.env.CODESPACE_NAME;
  const dominio = process.env.GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN;
  return Boolean(nome && dominio && origem === `https://${nome}-3000.${dominio}`);
}

function requisicaoPermitida(request: Request) {
  // Navegadores precisam de preflight para enviar X-Educria-Client entre origens.
  // Esta API atende o próprio site e nunca autoriza tal preflight.
  if (request.method === "OPTIONS" && request.headers.has("access-control-request-method")) {
    return false;
  }
  if (request.headers.get("x-educria-client") === "web") {
    return true;
  }

  // Mantém a política anterior para clientes sem o cabeçalho da interface.
  const origem = request.headers.get("origin");
  return request.headers.get("sec-fetch-site") !== "cross-site" &&
    (origem === null || origemPermitida(origem));
}

async function encaminhar(request: Request) {
  if (!requisicaoPermitida(request)) {
    return Response.json({ message: "Origem não permitida" }, { status: 403 });
  }

  const entrada = new URL(request.url);
  const destino = new URL("http://127.0.0.1:8080");
  destino.pathname = entrada.pathname;
  destino.search = entrada.search;

  const headers = new Headers();
  for (const nome of ["authorization", "content-type", "accept"]) {
    const valor = request.headers.get(nome);
    if (valor !== null) headers.set(nome, valor);
  }

  try {
    const resposta = await fetch(destino, {
      method: request.method,
      headers,
      body: ["GET", "HEAD"].includes(request.method) ? undefined : await request.arrayBuffer(),
      cache: "no-store",
      redirect: "manual",
      signal: request.signal,
    });
    const retorno = new Headers({ "cache-control": "no-store" });
    for (const nome of ["content-type", "www-authenticate", "allow"]) {
      const valor = resposta.headers.get(nome);
      if (valor !== null) retorno.set(nome, valor);
    }
    return new Response(resposta.body, { status: resposta.status, headers: retorno });
  } catch {
    return Response.json({ message: "A API EDUCRIA está indisponível" }, { status: 502 });
  }
}

export { encaminhar as GET, encaminhar as HEAD, encaminhar as POST, encaminhar as PUT,
  encaminhar as PATCH, encaminhar as DELETE, encaminhar as OPTIONS };
