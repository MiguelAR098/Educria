# Executar o backend em Windows nativo

O roteiro é para Windows PowerShell com Java 21, Maven, SQL Server Express local e o `sqlcmd.exe` clássico da distribuição ODBC no `PATH`. Configure uma porta TCP local fixa para o JDBC. A etapa `Criar` exige permissão para criar bancos na instância isolada. Use uma conta com permissões nas bases descartáveis para as etapas seguintes. Para o JDBC com autenticação integrada, coloque a DLL `mssql-jdbc_auth` da mesma versão do driver e da mesma arquitetura da JVM no `PATH`. [Autenticação integrada no JDBC](https://learn.microsoft.com/en-us/sql/connect/jdbc/building-the-connection-url?view=sql-server-ver17)

## Criar e iniciar a demonstração

Abra o PowerShell na raiz do projeto. A primeira operação usa o protocolo local da instância `SQLEXPRESS`. O exemplo assume que a mesma instância escuta em `localhost:1433` para o JDBC. Ajuste a porta à configuração da máquina. Use outro sufixo para criar uma base nova se o nome escolhido já existir.

```powershell
$banco = 'educria_demo_windows'
$servidor = 'lpc:.\SQLEXPRESS'
.\scripts\sqlserver\academico.ps1 -Banco $banco -Etapa Criar -Servidor $servidor

$env:EDUCRIA_SQLSERVER_URL = "jdbc:sqlserver://localhost:1433;databaseName=$banco;integratedSecurity=true;encrypt=true;trustServerCertificate=true"
$segredo = [byte[]]::new(48)
$gerador = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $gerador.GetBytes($segredo) } finally { $gerador.Dispose() }
$env:EDUCRIA_JWT_SECRET = [Convert]::ToBase64String($segredo)
mvn -B -f backend/pom.xml -DskipTests package
java -jar backend/target/educria-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

O exemplo de certificado vale para o servidor de desenvolvimento no loopback. Com certificado confiável, use `trustServerCertificate=false`. Inicie em um terminal sem valores de `EDUCRIA_SQLSERVER_USER` e `EDUCRIA_SQLSERVER_PASSWORD` herdados de outro ambiente ao usar autenticação integrada. A sessão deve registrar o Flyway até a V5 e o callback das questões.

Abra outro terminal PowerShell e carregue as contas. A opção `-ConfiarCertificadoLocal` se aplica somente à instância local de demonstração com certificado não confiável. O script exige o prefixo `educria_demo_`, o marcador de base descartável e autenticação integrada. A transação e o SQL de carga são compartilhados com o launcher Bash.

```powershell
.\scripts\sqlserver\carregar-demo.ps1 -Banco educria_demo_windows -Servidor 'tcp:localhost,1433' -ConfiarCertificadoLocal
curl.exe -sS -o NUL -w '%{http_code}' http://127.0.0.1:8080/api/aluno/desempenho
```

Resultado esperado da consulta anônima é `403`. Use as contas fictícias e o [roteiro HTTP da API](../../backend/README.md) para os fluxos autenticados.

## Provas acadêmicas

Crie `educria_sqlserver_teste_academico_windows` com o mesmo script `-Etapa Criar`. Altere `EDUCRIA_SQLSERVER_URL` para essa base. Inicie o JAR com `--spring.profiles.active=default` para aplicar V1–V5, encerre-o após a inicialização e carregue o conjunto acadêmico. A instância e as bases precisam permanecer isoladas do ambiente operacional.

```powershell
$bancoAcademico = 'educria_sqlserver_teste_academico_windows'
$conteudo = Get-Content -Raw -Encoding UTF8 backend/src/test/sql/criar_professor_demo.sql
$env:EDUCRIA_DEMO_BCRYPT = [regex]::Match($conteudo, '\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}').Value
.\scripts\sqlserver\academico.ps1 -Banco $bancoAcademico -Etapa Demonstracao
Remove-Item Env:\EDUCRIA_DEMO_BCRYPT
.\scripts\sqlserver\academico.ps1 -Banco $bancoAcademico -Etapa Verificar
.\scripts\sqlserver\academico.ps1 -Banco $bancoAcademico -Etapa Dicionario
```

A extração acima lê somente o BCrypt já publicado de uma conta fictícia. Os testes Java usam outra base marcada `educria_sqlserver_teste_<sufixo>`, com `EDUCRIA_SQLSERVER_TEST_URL` no loopback e o mesmo parâmetro `integratedSecurity=true` da conexão acima. [Comandos da suíte e salvaguardas](README.md#testes-de-integração)

**Validação pendente nesta revisão**

A execução no Windows nativo com SQL Server Express requer uma máquina Windows disponível para confirmar instalação do driver, política de execução do PowerShell, autenticação integrada, carga repetida e validação por HTTP. As evidências do Codespaces usam SQL Server 2022 Developer.
