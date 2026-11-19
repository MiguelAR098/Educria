# EDUCRIA · Alterações e verificação

A base é `educria.zip`. O conteúdo de `educria-modelo.zip` foi aplicado nos caminhos equivalentes. O ZIP consolidado abre diretamente na raiz do projeto.

## Herdado do pacote de desempenho

Os caminhos Java abaixo partem de `backend/src/main/java/com/educria/`.

| Arquivo | Alteração herdada |
|---|---|
| `domain/entity/Materia.java`, `domain/entity/Assunto.java` | Entidades do catálogo. |
| `domain/entity/DesempenhoPorAssunto.java` | Estado privado de cada aluno por assunto. |
| `domain/NivelAdaptativo.java`, `domain/entity/DesempenhoAdaptativo.java` | Regra de progressão de 1 a 10 compartilhada com o desempenho global. |
| `repository/DesempenhoPorAssuntoRepository.java` | Consulta por aluno e assunto. |
| `backend/src/main/resources/db/migration/V2__desempenho_por_assunto.sql` | Cadastra Matemática e os oito assuntos. Inicializa os alunos existentes e os novos. |
| `backend/src/test/java/com/educria/domain/entity/DesempenhoPorAssuntoTest.java`, `backend/src/test/sql/verificar_v2.sql` | Testes dos limites de nível, dos indicadores globais e da migração com histórico. |
| `VERIFICACAO.md` | Guia técnico do modelo. As instruções de aplicação do pacote foram ajustadas para a versão consolidada. |

A comparação byte a byte confirmou a V1 original e a V2 do pacote de modelo. O dashboard continua lendo o registro global em `GET /api/aluno/desempenho`. Cada assunto começa em nível 1, com contadores zerados.

## Exclusivo da adaptação para Codespaces

| Ação | Arquivo | Finalidade |
|---|---|---|
| Adicionado | `.devcontainer/devcontainer.json` | Java 21, Maven 3.9.9, Node 22 e Docker com Compose. Encaminhamento da porta 3000. |
| Adicionado | `compose.yaml` | PostgreSQL 16 com verificação de prontidão, volume nomeado e porta 5432 vinculada ao loopback. |
| Adicionados | `.env.example`, `.gitignore` | Exemplo da configuração local e exclusão de segredos, dependências e saídas de build. |
| Adicionado | `scripts/iniciar.sh` | Gera senha do banco e chave JWT no primeiro uso, prepara o banco, compila e inicia os serviços. |
| Adicionado | `scripts/verificar.mjs` | Testa cadastro, login, perfil, dados do dashboard, Flyway, origem do navegador e inicialização dos oito assuntos. |
| Alterado | `backend/src/main/resources/application.yml` | Banco e JWT recebem credenciais do ambiente. O Spring escuta em `127.0.0.1:8080`. Foram eliminados os valores redundantes de configuração local do RabbitMQ. |
| Alterado | `frontend/lib/api.ts` | O cliente usa a origem do site para chamar `/api/*`. O interceptor JWT original continua em uso. |
| Adicionado | `frontend/app/api/[...path]/route.ts` | O servidor Next verifica a origem, encaminha a requisição ao Spring e devolve status e corpo ao cliente. |
| Alterado | `frontend/app/aluno/page.tsx` | A mensagem de erro do dashboard passou a indicar a indisponibilidade da API sem orientar o aluno a abrir localhost. |
| Removido | `frontend/.env.local.example` | O exemplo antigo apontava o navegador para a porta 8080. |
| Alterados | `backend/README.md`, `frontend/README.md` | Instruções de execução direcionam para o guia do Codespaces. |
| Adicionados | `INICIAR-CODESPACES.md`, `ALTERACOES.md` | Primeira execução, testes e registro das mudanças. |
| Excluídos do ZIP | `frontend/node_modules/`, `frontend/.next/`, `backend/target/` | Dependências e resultados de compilação são recriados pelo iniciador. |

**Correção do cadastro.** O teste de terminal anterior montava o cabeçalho `Origin` com as mesmas variáveis usadas pelo Next para verificá-lo. Com isso, ele podia passar enquanto o formulário recebia “Origem não permitida”. O cliente Axios agora envia `X-Educria-Client: web` nas chamadas a `/api/*`. O Next aceita a chamada marcada e recusa todo preflight CORS, que o navegador exige para enviar esse cabeçalho a partir de outro site. Essa chamada do formulário funciona mesmo quando o endereço público difere do endereço inferido no servidor. Para clientes sem a marca, continuam valendo as verificações anteriores de origem.

O Spring recebe o método, o corpo, `Authorization`, `Content-Type` e `Accept`. O endereço do destino é fixo em loopback. A autenticação continua dependendo de JWT e as permissões continuam sendo verificadas pelo Spring Security. [Referência técnica sobre cabeçalhos personalizados em chamadas de navegador (OWASP)](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#employing-custom-request-headers-for-ajaxapi).

## O que foi testado

| Verificação | Resultado |
|---|---|
| Backend | JDK 21 e Maven 3.9.9 compilaram o projeto e geraram o JAR executável. Os 7 testes JUnit passaram, sem erros ou testes ignorados. |
| Frontend | Com Node 22, `npm ci`, TypeScript com `--noEmit` e o build de produção do Next passaram. |
| Banco | PostgreSQL 16.16 real iniciou na porta 5432. O Flyway aplicou V1 e V2, nessa ordem, em base vazia. O script SQL também passou no cenário com aluno e histórico criados sob a V1. |
| Serviços e contrato | Next na porta 3000 e Spring na 8080. Cadastro retornou 201. Login retornou JWT. A consulta autenticada ao desempenho retornou os cinco campos esperados. Os dados de pontos da turma também retornaram 200. |
| Controle de acesso | Consulta privada sem token recusada. Conta ALUNO recebeu 403 na rota administrativa. O proxy recusou uma origem externa. |
| Reprodução do CORS | O mesmo POST, com a mesma origem HTTPS de teste, retornou `403 Invalid CORS request` no acesso direto ao Spring e 201 no acesso pela porta 3000. O banco criou os oito registros de desempenho. |
| Fluxo visual | Chromium executou o formulário de cadastro, abriu o dashboard com o nível inicial, saiu da conta e voltou ao painel após login. O encaminhamento HTTPS do GitHub foi simulado com um nome público diferente do nome de Codespace configurado no Next. O teste também confirmou que o preflight para envio do cabeçalho a partir de outra origem recebe 403. Frontend, backend e banco foram reais. |
| Reinício | Após parar e iniciar PostgreSQL e Spring sobre os mesmos dados e com a mesma chave, o JWT do aluno cadastrado continuou dando acesso ao desempenho. |
| Compose e iniciador | A configuração passou em `docker compose config` com o executável Compose v2. A sintaxe do iniciador passou em `bash -n`. Para os testes integrados locais, o PostgreSQL rodou como processo e as consultas de verificação usaram `psql` direto. |

**Pendente no GitHub.** A criação do Dev Container, a inicialização completa pelo script com o daemon Docker, o túnel HTTPS privado e a retomada do volume após parar o próprio Codespace dependem de uma execução hospedada. O guia inclui o roteiro para conferir esses pontos.

As versões do projeto foram preservadas. Durante a instalação, o npm emitiu um aviso de segurança para o Next.js 14.2.5 herdado do ZIP original. Esta entrega configura um ambiente privado de desenvolvimento.
