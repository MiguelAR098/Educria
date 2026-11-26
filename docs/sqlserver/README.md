# SQL Server e execução do backend

O backend requer Java 21, Maven e SQL Server. O perfil padrão aplica o Flyway de V1 a V5 e valida as entidades com Hibernate. O perfil `dev` acrescenta o callback de 160 questões de Matemática. As contas de demonstração são uma carga separada. A API ouve em `127.0.0.1:8080` por padrão. [Execução nativa no Windows](APRESENTAR_WINDOWS.md) · [Modelo e dicionário](MODELO.md)

## Codespaces

Use o SQL Server 2022 Developer no Compose e uma base nova identificada. Execute os comandos da raiz, em um terminal Bash. As portas precisam estar livres para os serviços da demonstração. O nome do projeto Compose distingue o contêiner e seu volume. O projeto deve ser usado em um Codespace isolado, com a porta SQL local 1433 livre.

```bash
export COMPOSE_PROJECT_NAME=educria_apresentacao
read -rsp 'Senha sa para o contêiner de demonstração: ' MSSQL_SA_PASSWORD
printf '\n'
export MSSQL_SA_PASSWORD
docker compose up -d --wait db

docker compose exec -T db sh -ec '
  export SQLCMDPASSWORD="$MSSQL_SA_PASSWORD"
  exec /opt/mssql-tools18/bin/sqlcmd -S localhost -C -U sa -d master -b
' <<'SQL'
IF DB_ID(N'educria_demo_apresentacao') IS NOT NULL
    THROW 51000, N'Escolha outro nome para uma base nova.', 1;
GO
CREATE DATABASE educria_demo_apresentacao COLLATE Latin1_General_100_CS_AS_SC;
GO
USE educria_demo_apresentacao;
EXEC sys.sp_addextendedproperty @name=N'EducriaAmbiente', @value=N'descartavel';
GO
SQL

export EDUCRIA_SQLSERVER_URL='jdbc:sqlserver://127.0.0.1:1433;databaseName=educria_demo_apresentacao;encrypt=true;trustServerCertificate=true'
export EDUCRIA_SQLSERVER_USER=sa EDUCRIA_SQLSERVER_PASSWORD="$MSSQL_SA_PASSWORD"
export EDUCRIA_JWT_SECRET=$(openssl rand -base64 48)
mvn -B -f backend/pom.xml -DskipTests package
SPRING_PROFILES_ACTIVE=dev java -jar backend/target/educria-backend-1.0.0-SNAPSHOT.jar
```

O parâmetro de confiança do certificado se destina ao contêiner local de demonstração. Para outro ambiente, configure a cadeia de confiança e o certificado do servidor. Mantenha o terminal com a API aberta. Em outro terminal com as mesmas variáveis Compose e SQL, execute a carga de contas e confirme que uma rota privada responde `403` sem autenticação.

```bash
bash scripts/sqlserver/carregar-demo.sh --desenvolvimento --banco educria_demo_apresentacao
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/api/aluno/desempenho
```

Use o nome da base marcada em `--banco` e mantenha o backend conectado a ela. O script de contas é idempotente e usa o mesmo SQL no Bash e no PowerShell. Execute login e demais operações diretamente no backend. [Roteiro HTTP da API](../../backend/README.md#apresentação-pelo-backend)

## Testes de integração

Crie uma segunda base pelo mesmo procedimento usando `educria_sqlserver_teste_<sufixo>` em vez de `educria_demo_apresentacao`. Mantenha a propriedade estendida `EducriaAmbiente=descartavel`. A URL precisa usar `localhost` ou `127.0.0.1`, uma porta explícita e o nome dessa base. As suítes aplicam V1–V5 no perfil `integracao`, sem catálogo de questões do perfil `dev`.

```bash
export EDUCRIA_SQLSERVER_TEST_URL='jdbc:sqlserver://127.0.0.1:1433;databaseName=educria_sqlserver_teste_integracao;encrypt=true;trustServerCertificate=true'
export EDUCRIA_SQLSERVER_TEST_USER=sa EDUCRIA_SQLSERVER_TEST_PASSWORD="$MSSQL_SA_PASSWORD"
mvn -B -f backend/pom.xml '-Dtest=*Test,*IT' test
```

Confira o resumo e os XMLs em `backend/target/surefire-reports/`. O Surefire não inclui por padrão os testes `*IT`. Eles ficam ignorados se `EDUCRIA_SQLSERVER_TEST_URL` estiver ausente. Se o sandbox bloquear a anexação dinâmica do Mockito e o agente estiver no cache Maven, acrescente `-DargLine=-javaagent:/caminho/byte-buddy-agent-1.14.19.jar` ao Maven.

## Provas acadêmicas

Crie outra base nova `educria_sqlserver_teste_academico` com o marcador, usando o mesmo procedimento. Aponte `EDUCRIA_SQLSERVER_URL` explicitamente para ela, inicie o JAR com o **perfil padrão** para o Flyway aplicar V1–V5 e encerre a API antes da carga acadêmica. O seed acadêmico usa dados sintéticos próprios em tabelas de usuários, turmas e atividades inicialmente vazias. Mantenha essa base separada da apresentação no perfil `dev` e da suíte de integração.

```bash
# O hash abaixo é extraído da conta fictícia pública do projeto.
export EDUCRIA_DEMO_BCRYPT=$(rg -m 1 -o '\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}' backend/src/test/sql/criar_professor_demo.sql)
bash scripts/sqlserver/academico.sh educria_sqlserver_teste_academico demonstracao
unset EDUCRIA_DEMO_BCRYPT
bash scripts/sqlserver/academico.sh educria_sqlserver_teste_academico verificar
bash scripts/sqlserver/academico.sh educria_sqlserver_teste_academico dicionario
```

O lançador lê a credencial do próprio contêiner. Para um projeto Compose separado com arquivo em outro diretório, configure `EDUCRIA_COMPOSE_DIR` para esse diretório e mantenha o nome do projeto em `COMPOSE_PROJECT_NAME`, se aplicável. Os scripts verificam o nome e o marcador da base antes das operações. A carga é transacional e preserva `flyway_schema_history`.

| Arquivo | Evidência acadêmica |
|---|---|
| `01-dados-demo.sql` | Ao menos três registros por tabela de domínio em uma base descartável nova, incluindo o histórico de participação e pontos individuais e coletivos independentes. |
| `02-automacao.sql` | Função de contagem de erros estatísticos e procedure de resumo coletivo. As rotinas apenas consultam dados. |
| `03-verificacao.sql` | Contagens, PKs, N:N, `IDENTITY(1000,10)`, efeito do trigger da V2, índices, aliases, JOINs, `COUNT`, `SUM`, `AVG`, `MAX` e `MIN`. |
| `04-integridade.sql` | Cascatas e restrições do núcleo com dados temporários em transação revertida. |
| `05-dicionario.sql` | Dicionário físico a partir de `sys.*`, com tipos, chaves, CHECKs e índices. |
| `06-medicao-indice.sql` | Consulta opcional de planos com dados sintéticos. Resultados dependem da instância e da execução. |

## Execução no Windows

O fluxo usa PowerShell e autenticação integrada no SQL Server Express. O roteiro documenta os comandos para a instalação nativa. A validação em SQL Server Express nativo continua pendente nesta revisão. [Roteiro Windows](APRESENTAR_WINDOWS.md)
