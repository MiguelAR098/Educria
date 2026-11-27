#!/usr/bin/env bash
set -euo pipefail

segredo=/run/secrets/EDUCRIA_SQLSERVER_PASSWORD
if [[ ! -s "$segredo" ]]; then
  echo 'Arquivo da senha do SQL Server de apresentacao ausente.' >&2
  exit 1
fi

case "${1:-}" in
  iniciar)
    export MSSQL_SA_PASSWORD="$(<"$segredo")"
    exec /opt/mssql/bin/sqlservr
    ;;
  pronto)
    banco=master
    roteiro=/opt/educria/criar-banco.sql
    limite=45
    ;;
  verificar)
    banco=educria_demo_apresentacao
    roteiro=/opt/educria/validar-banco.sql
    limite=15
    ;;
  *)
    echo 'Use iniciar, pronto ou verificar.' >&2
    exit 2
    ;;
esac

export SQLCMDPASSWORD="$(<"$segredo")"
exec /opt/mssql-tools18/bin/sqlcmd -S tcp:127.0.0.1,1433 -C -U sa -d "$banco" \
  -l 3 -t "$limite" -b -f 65001 -h -1 -W -i "$roteiro"
