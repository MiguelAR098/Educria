# Comparação de sessões do mesmo assunto

Base utilizada: `MiguelAR098/Educria`, branch `main`, commit `df991636c733f4d6c28dfca230902d5db37d0956`.

## O que mudou

Em **Histórico de sessões → Resultado da sessão**, uma sessão concluída compara seus indicadores com a última sessão concluída daquele aluno no mesmo assunto. Por exemplo, se o aluno concluiu Divisão, depois Soma e depois Divisão, a terceira sessão usa a primeira como referência.

Na primeira conclusão do assunto, a tela orienta o aluno a fazer outra sessão. Em sessões ativas ou interrompidas, o resultado continua parcial e a tela explica quando a comparação ficará disponível.

## Como o banco escolhe a referência

O endpoint existente valida o titular da sessão pelo e-mail do usuário autenticado. Uma subconsulta SQL parte do ID da sessão atual e seleciona candidatas com o mesmo `aluno_id`, o mesmo `assunto_id` e ID menor que o atual, seguindo a ordem do histórico existente. Exige `ENCERRADA` nas duas sessões e usa `ORDER BY candidata.id DESC LIMIT 1`. A busca cobre o histórico inteiro. Em seguida a consulta de indicadores, também restrita ao e-mail do titular, agrega apenas a sessão selecionada. Se nenhuma candidata satisfizer os filtros, a API devolve `anterior: null`.

`apresentadas` e `respondidas` continuam sendo contagens de questões distintas. `acertos` e `erros` contam tentativas. A sequência de respostas por `passo` continua disponível no detalhe. A atualização usa o esquema e o contrato de API existentes.

## Arquivos do pacote

- `backend/src/main/java/com/educria/repository/HistoricoSessaoRepository.java` — consulta da referência comparável.
- `backend/src/main/java/com/educria/service/HistoricoSessaoService.java` — uso da consulta no resultado.
- `frontend/app/aluno/historico/[id]/page.tsx` — título da comparação e mensagens para resultado parcial e primeira conclusão.
- `backend/src/test/java/com/educria/service/SessaoEstudoIT.java` e `frontend/tests/test_dashboard.py` — testes da consulta, do isolamento e dos estados da tela.
- `ATUALIZACAO-COMPARACAO.md` — este guia.

## Aplicar no Windows com o VS Code

1. Coloque `educria-comparacao.zip` na pasta do projeto ao lado de `backend` e `frontend`. Pare os processos antigos do backend e do frontend com Ctrl+C. Mantenha o PostgreSQL em execução.
2. Abra um PowerShell **na pasta que contém o ZIP** e rode o bloco abaixo.

```powershell
if (!(Test-Path .\backend\pom.xml) -or !(Test-Path .\frontend\package.json)) {
    throw 'Abra o terminal na pasta que contém backend e frontend.'
}
Expand-Archive -LiteralPath .\educria-comparacao.zip -DestinationPath . -Force
```

3. Para quem inicia o EDUCRIA pelo JAR, use o terminal que já tem as variáveis do banco e do JWT configuradas. Partindo novamente da raiz, gere o JAR atualizado. Depois de aparecer **BUILD SUCCESS**, inicie-o.

```powershell
mvn -f .\backend\pom.xml package
```

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
java -jar .\backend\target\educria-backend-1.0.0-SNAPSHOT.jar
```

4. Aguarde `Started EducriaApplication`. Em um segundo terminal na raiz, execute `cd frontend` e depois `npm.cmd run dev`. Abra [o site local](http://localhost:3000).
5. Para verificar, consulte o resultado de uma sessão interrompida e de duas sessões concluídas do mesmo assunto. A interrompida mostra o aviso. A primeira conclusão mostra a orientação inicial. A segunda mostra a tabela com a primeira, mesmo quando há uma sessão de outro assunto entre elas.

## Testes executados

- Java 21 e PostgreSQL 16: 40 testes Maven (unitários e de integração), todos aprovados. Abrangem sessões consecutivas e intercaladas, 25 sessões intermediárias, assuntos homônimos com IDs diferentes, candidatos incompletos, sessão atual parcial, primeira conclusão e separação entre alunos. Uma sessão de 14 tentativas e 10 questões distintas também foi recuperada como referência com os mesmos indicadores do seu resultado original.
- Next.js: build de produção com verificação TypeScript aprovado.
- Chromium: oito testes aprovados, seis com API simulada e dois com backend real. O teste específico da comparação usou a API simulada para conferir mensagens e tabela; a escolha efetiva da sessão foi testada pelos testes Java com PostgreSQL.
- Execução local em Linux. O procedimento PowerShell não foi executado em Windows.
