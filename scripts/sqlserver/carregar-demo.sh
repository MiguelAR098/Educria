#!/usr/bin/env bash
set -euo pipefail

uso() {
  printf 'Uso: %s --desenvolvimento [--banco educria_demo_<sufixo>]\n' "$0"
}

if [[ $# -eq 1 && ( $1 == -h || $1 == --help ) ]]; then
  uso
  exit 0
fi

if [[ ${1:-} != --desenvolvimento ]]; then
  uso >&2
  exit 2
fi
shift

banco=educria
if (( $# > 0 )); then
  if [[ $# -ne 2 || $1 != --banco || ! $2 =~ ^educria_demo_[a-zA-Z0-9_]{1,60}$ ]]; then
    uso >&2
    exit 2
  fi
  banco=$2
fi

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
  cat <<'SQL'
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;

IF DB_NAME() <> N'educria' AND NOT EXISTS (
    SELECT 1 FROM sys.extended_properties
    WHERE class = 0 AND name = N'EducriaAmbiente' AND CONVERT(nvarchar(30), value) = N'descartavel'
)
    THROW 51100, N'O banco de teste precisa do marcador EducriaAmbiente=descartavel.', 1;

BEGIN TRANSACTION;

DECLARE @bloqueio int;
EXEC @bloqueio = sys.sp_getapplock @Resource = N'educria.carga_demo_contas',
    @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 15000;
IF @bloqueio < 0
    THROW 51101, N'Não foi possível obter o bloqueio da carga de demonstração.', 1;

CREATE TABLE #demo_turma (id bigint NOT NULL PRIMARY KEY);
CREATE TABLE #demo_alunos (
    email nvarchar(100) COLLATE DATABASE_DEFAULT NOT NULL PRIMARY KEY,
    id bigint NOT NULL,
    novo bit NOT NULL
);
GO
SQL

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
