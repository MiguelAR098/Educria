#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 || ! $1 =~ ^educria_sqlserver_teste_[a-zA-Z0-9_]{1,60}$ ]]; then
  printf 'Uso: %s educria_sqlserver_teste_<sufixo> demonstracao|verificar|dicionario\n' "$0" >&2
  exit 2
fi

diretorio=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
compose=${EDUCRIA_COMPOSE_DIR:-"$diretorio/../.."}
banco=$1
sql() {
  docker --host unix:///var/run/docker.sock compose --project-directory "$compose" -f "$compose/compose.yaml" \
    exec -T db sh -euc '
    export SQLCMDPASSWORD="${MSSQL_SA_PASSWORD:?Credencial do SQL Server ausente no contêiner.}"
    exec /opt/mssql-tools18/bin/sqlcmd -S localhost -C -U sa -d "$1" -l 10 -b -I -x -f 65001
  ' academico "$banco"
}

case $2 in
  demonstracao)
    if [[ ! ${EDUCRIA_DEMO_BCRYPT:-} =~ ^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$ ]]; then
      printf 'Defina EDUCRIA_DEMO_BCRYPT com um hash BCrypt de 60 caracteres.\n' >&2
      exit 2
    fi
    { printf "DECLARE @HashDemo nvarchar(60) = N'%s';\n" "$EDUCRIA_DEMO_BCRYPT"; cat "$diretorio/academico/01-dados-demo.sql"; } | sql
    sql < "$diretorio/academico/02-automacao.sql"
    ;;
  verificar)
    sql < "$diretorio/academico/03-verificacao.sql"
    sql < "$diretorio/academico/04-integridade.sql"
    ;;
  dicionario)
    sql < "$diretorio/academico/05-dicionario.sql"
    ;;
  *) printf 'Etapa inválida: %s\n' "$2" >&2; exit 2 ;;
esac
