#!/usr/bin/env bash
set -euo pipefail

uso() {
  printf 'Uso: %s --desenvolvimento --banco educria_demo_<sufixo>\n' "$0"
}

if [[ $# -eq 1 && ( $1 == -h || $1 == --help ) ]]; then
  uso
  exit 0
fi

if [[ $# -ne 3 || $1 != --desenvolvimento || $2 != --banco || ! $3 =~ ^educria_demo_[a-zA-Z0-9_]{1,60}$ ]]; then
  uso >&2
  exit 2
fi
banco=$3

raiz=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
sql="$raiz/backend/src/test/sql"
arquivos=(
  criar_professor_demo.sql
  criar_professor_portugues_demo.sql
  criar_alunos_demo.sql
  criar_desempenho_assuntos_demo.sql
  criar_historico_demo.sql
)

{
  cat "$raiz/scripts/sqlserver/carga-demo-inicio.sql"

  for arquivo in "${arquivos[@]}"; do
    cat "$sql/$arquivo"
    printf '\nGO\n'
  done
  printf 'COMMIT TRANSACTION;\nGO\n'
} | docker --host unix:///var/run/docker.sock compose --project-directory "$raiz" -f "$raiz/compose.yaml" \
  exec -T db sh -euc '
    if [ "${MSSQL_PID:-}" != Developer ]; then
      printf "%s\n" "A carga exige o SQL Server Developer do Compose local." >&2
      exit 1
    fi
    export SQLCMDPASSWORD="${MSSQL_SA_PASSWORD:?Credencial do SQL Server ausente no contêiner.}"
    exec /opt/mssql-tools18/bin/sqlcmd -S localhost -C -U sa -d "$1" -l 10 -b -r 0 -x -f 65001
  ' carregar-demo "$banco"

printf 'Carga de demonstração concluída no banco %s.\n' "$banco"
