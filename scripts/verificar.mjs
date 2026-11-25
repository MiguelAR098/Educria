// Executar em outro terminal com os serviços em execução.
import assert from "node:assert/strict";
import { randomBytes, randomUUID } from "node:crypto";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

process.chdir(fileURLToPath(new URL("../", import.meta.url)));
const base = "http://127.0.0.1:3000";
const origem = process.env.CODESPACE_NAME && process.env.GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN
  ? `https://${process.env.CODESPACE_NAME}-3000.${process.env.GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN}`
  : base;
const email = `teste-${randomUUID()}@example.test`;
const senha = randomBytes(18).toString("hex");

function post(caminho, dados, headers = {}) {
  return fetch(base + caminho, {
    method: "POST",
    headers: { "Content-Type": "application/json", Origin: origem, ...headers },
    body: JSON.stringify(dados),
  });
}

function consultar(sql) {
  return execFileSync("docker", ["compose", "exec", "-T", "db", "sh", "-c",
    'export SQLCMDPASSWORD="$MSSQL_SA_PASSWORD"; exec /opt/mssql-tools18/bin/sqlcmd -S localhost -C -U sa -d educria -b -h -1 -W -Q "$1"',
    "sqlcmd", `SET NOCOUNT ON; ${sql}`],
    { encoding: "utf8" }).trim();
}

assert.ok(Number(consultar("SELECT SERVERPROPERTY('ProductMajorVersion')")) >= 15);
assert.equal(consultar("SELECT STRING_AGG(version, ',') WITHIN GROUP (ORDER BY installed_rank) FROM dbo.flyway_schema_history WHERE success=1 AND version IN ('1','2','3','4')"), "1,2,3,4");
console.log("OK · SQL Server e Flyway V1 a V4");

const preflight = await fetch(base + "/api/auth/register", {
  method: "OPTIONS",
  headers: {
    Origin: "https://outro.example",
    "Access-Control-Request-Method": "POST",
    "Access-Control-Request-Headers": "content-type,x-educria-client",
  },
});
assert.equal(preflight.status, 403, "Preflight de outra página deve ser recusado");
assert.equal(preflight.headers.get("access-control-allow-origin"), null);

const externo = await post("/api/auth/register", {}, { Origin: "https://outro.example" });
assert.equal(externo.status, 403, "Uma origem externa deve ser recusada");
const semToken = await fetch(base + "/api/aluno/desempenho");
assert.ok([401, 403].includes(semToken.status), "Rota do aluno deve exigir token");
console.log("OK · origem externa e consulta sem token recusadas");

// Origem de teste independente das variáveis que o Next recebe do Codespace.
const viaInterface = { Origin: "https://tunel-de-teste.example", "X-Educria-Client": "web" };
const cadastro = await post("/api/auth/register", { nome: "Aluno de verificação", email, senha }, viaInterface);
assert.equal(cadastro.status, 201, "Cadastro com os cabeçalhos enviados pela interface");
const login = await post("/api/auth/login", { email, senha }, viaInterface);
assert.equal(login.status, 200, "Login");
const { token } = await login.json();
assert.equal(typeof token, "string");
assert.equal(token.split(".").length, 3);
const headers = { Authorization: "Bearer " + token, Origin: origem };
const desempenho = await fetch(base + "/api/aluno/desempenho", { headers });
assert.equal(desempenho.status, 200);
assert.deepEqual(await desempenho.json(), {
  nivelAtual: 1, errosConsecutivos: 0, totalAcertos: 0, totalErros: 0, nivelMaximo: false, pontosIndividuais: 0,
});
const pontos = await fetch(base + "/api/aluno/turma/pontos", { headers });
assert.equal(pontos.status, 200);
const admin = await post("/api/admin/turmas", {}, headers);
assert.equal(admin.status, 403, "O perfil ALUNO deve ser recusado nas rotas administrativas");
console.log("OK · cadastro 201, login JWT, dados do dashboard e autorização por perfil");

assert.equal(consultar(`SELECT count(*) FROM desempenho_por_assunto d
  JOIN assuntos s ON s.id=d.assunto_id JOIN materias m ON m.id=s.materia_id
  JOIN usuarios u ON u.id=d.aluno_id WHERE u.email='${email}' AND m.nome=N'Matemática'
  AND d.nivel_atual=1 AND d.total_acertos=0 AND d.total_erros=0 AND d.erros_consecutivos=0`), "8");
console.log("OK · aluno de teste com os oito assuntos no nível 1");

const catalogo = await fetch(base + "/api/aluno/materias", { headers });
assert.equal(catalogo.status, 200);
const materias = await catalogo.json();
const matematica = materias.find((materia) => materia.nome === "Matemática");
assert.equal(matematica?.totalAssuntos, 8);
assert.ok(materias.every((materia) => materia.pontosIndividuais === 0));
const rotaAssuntos = base + `/api/aluno/materias/${matematica.id}/assuntos`;
const respostaAssuntos = await fetch(rotaAssuntos, { headers });
assert.equal(respostaAssuntos.status, 200);
const assuntos = await respostaAssuntos.json();
assert.equal(new Set(assuntos.map((assunto) => assunto.id)).size, 8);
assert.ok(assuntos.every((assunto) => assunto.nivelAtual === 1));
assert.ok(assuntos.every((assunto) => assunto.pontosIndividuais === 0));
for (const rota of [base + "/api/aluno/materias", rotaAssuntos]) {
  assert.ok([401, 403].includes((await fetch(rota)).status));
}
console.log("OK · catálogo autenticado e níveis do aluno recém-cadastrado");
