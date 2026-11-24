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
    printf 'MSSQL_SA_PASSWORD=Aa1%s\n' "$(openssl rand -hex 24)" > "$temporario"
    printf 'EDUCRIA_JWT_SECRET=%s\n' "$(openssl rand -base64 48)" >> "$temporario"
    mv "$temporario" .env
  )
fi
set -a
source ./.env
set +a
: "${EDUCRIA_JWT_SECRET:?Preencha EDUCRIA_JWT_SECRET no .env}"
if [[ -z "${MSSQL_SA_PASSWORD:-}" ]]; then
  MSSQL_SA_PASSWORD="Aa1$(openssl rand -hex 24)"
  printf '\nMSSQL_SA_PASSWORD=%s\n' "$MSSQL_SA_PASSWORD" >> .env
fi
export MSSQL_SA_PASSWORD
unset SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD
unset SPRING_FLYWAY_URL SPRING_FLYWAY_USER SPRING_FLYWAY_PASSWORD
export EDUCRIA_SQLSERVER_URL='jdbc:sqlserver://127.0.0.1:1433;databaseName=educria;encrypt=true;trustServerCertificate=true'
export EDUCRIA_SQLSERVER_USER=sa
export EDUCRIA_SQLSERVER_PASSWORD="$MSSQL_SA_PASSWORD"

echo 'Iniciando SQL Server de desenvolvimento…'
docker compose up -d --wait --wait-timeout 120 db
docker compose exec -T db sh -c 'export SQLCMDPASSWORD="$MSSQL_SA_PASSWORD"; exec /opt/mssql-tools18/bin/sqlcmd -S localhost -C -U sa -d master -b' <<'SQL'
IF DB_ID(N'educria') IS NULL EXEC(N'CREATE DATABASE [educria] COLLATE Latin1_General_100_CS_AS_SC');
GO
SQL
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
