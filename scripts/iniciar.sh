#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Impede que duas inicializações gerem credenciais ou iniciem serviços em paralelo.
exec 9>"/tmp/educria-iniciar.lock"
flock -n 9 || { echo 'O iniciador já está em execução neste Codespace.' >&2; exit 1; }
if [[ ! -e .env ]]; then
  (
    umask 077
    temporario=$(mktemp .env.XXXXXX)
    trap 'rm -f "$temporario"' EXIT
    printf 'POSTGRES_PASSWORD=%s\n' "$(openssl rand -hex 24)" > "$temporario"
    printf 'EDUCRIA_JWT_SECRET=%s\n' "$(openssl rand -base64 48)" >> "$temporario"
    mv "$temporario" .env
  )
fi
set -a
source ./.env
set +a
: "${POSTGRES_PASSWORD:?Preencha POSTGRES_PASSWORD no .env}"
: "${EDUCRIA_JWT_SECRET:?Preencha EDUCRIA_JWT_SECRET no .env}"

echo 'Iniciando PostgreSQL 16…'
docker compose up -d --wait --wait-timeout 120 db
echo 'Compilando o backend e executando os testes…'
mvn -B -f backend/pom.xml clean package
echo 'Preparando o frontend…'
(cd frontend && npm ci --no-audit --no-fund && npm run build)

api_pid=''
web_pid=''
encerrar() {
  if [[ -n "$web_pid" ]]; then kill "$web_pid" 2>/dev/null || true; fi
  if [[ -n "$api_pid" ]]; then kill "$api_pid" 2>/dev/null || true; fi
  wait || true
}
trap encerrar EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
java -jar backend/target/educria-backend-1.0.0-SNAPSHOT.jar &
api_pid=$!
pronto=false
for ((i=0; i<120; i++)); do
  kill -0 "$api_pid" 2>/dev/null || { echo 'Falha ao iniciar o Spring. Confira o log acima.' >&2; exit 1; }
  status=$(curl --silent --max-time 2 --output /dev/null --write-out '%{http_code}' \
    http://127.0.0.1:8080/api/aluno/desempenho) || status=000
  if [[ "$status" == 403 || "$status" == 401 ]]; then pronto=true; break; fi
  sleep 1
done
"$pronto" || { echo 'Tempo esgotado aguardando o Spring na porta 8080.' >&2; exit 1; }
echo 'Abra a porta 3000 na aba Portas quando o Next indicar Ready. Ctrl+C encerra os dois servidores.'
(cd frontend && exec node node_modules/next/dist/bin/next start -H 0.0.0.0 -p 3000) &
web_pid=$!
wait -n "$api_pid" "$web_pid"
