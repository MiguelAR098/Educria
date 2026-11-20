# EDUCRIA · Reaprendizagem de questões e atalhos de teclado

## Aplicar no Windows pelo VS Code

1. Abra a pasta do seu EDUCRIA que contém `backend` e `frontend`. Coloque `educria-reaprendizagem.zip` ao lado delas. O pacote inclui a melhoria de teclado da etapa 15 parte 1.
2. Pare o frontend e o backend com Ctrl+C nos terminais em que estão rodando. Mantenha o PostgreSQL em execução. Na raiz do projeto, abra um terminal PowerShell e execute o bloco abaixo. Ele verifica o diretório antes de extrair os arquivos.

```powershell
if ((Test-Path .\backend\pom.xml) -and (Test-Path .\frontend\package.json)) {
    Expand-Archive -LiteralPath .\educria-reaprendizagem.zip -DestinationPath . -Force
} else {
    throw 'Abra o terminal na pasta que contém backend e frontend.'
}
```

3. No mesmo terminal da raiz, com as credenciais de desenvolvimento que você já usa para o banco e o JWT, inicie o backend. A V9 é aplicada ao iniciar. O perfil dev disponibiliza a carga de Matemática.

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
mvn -f .\backend\pom.xml spring-boot:run
```

4. Aguarde a mensagem `Started EducriaApplication`. Abra outro terminal PowerShell na raiz e inicie o frontend já instalado.

```powershell
Set-Location .\frontend
npm.cmd run dev
```

5. Abra [o EDUCRIA local](http://localhost:3000). Em uma instalação de desenvolvimento já configurada, esses dois processos e o banco são suficientes. No computador de 4 GB, faça compilações e testes em sequência. No Codespaces, o mesmo ZIP se aplica à raiz e o iniciador existente continua disponível com `SPRING_PROFILES_ACTIVE=dev bash scripts/iniciar.sh`.

## Verificar pelo site

- Entre como aluno, abra **Fazer atividade**, escolha **Somar e subtrair** e inicie. Clique na margem cinza à direita do cartão. Pressione 2 para marcar B e depois 3 para trocar para C. A correção começa quando você aciona **Enviar resposta**.
- Envie uma alternativa incorreta. A página apresenta uma dica e indica uma questão para reaprender. Pause e saia. Entre novamente, abra a sessão e clique em **Retomar sessão**. A pendência deve continuar lá.
- Clique em **Próxima questão** e responda às seguintes. Havendo material, duas outras apresentações separam o erro da repetição. Quando voltar à mesma questão, a seleção estará vazia. Você pode errar novamente. Ao acertar, a pendência sai e a primeira revisão vence no dia seguinte, no calendário de São Paulo.
- Abra **Histórico de sessões** no painel. O resultado agrupa as tentativas pela questão. Em **Minhas revisões** aparece o agendamento vigente. Uma sessão com pendências pode ser pausada e retomada.

## Decisões e persistência

A sequência de apresentações tem um número crescente chamado `passo`. A lista original de até dez questões distintas continua em `sessao_questoes`. Uma nova apresentação de questão já vista avança o passo, mantendo a mesma contagem de questões distintas. A questão atual e o passo sobrevivem a uma pausa, login ou reinicialização.

A fila é reconstruída no PostgreSQL pelas respostas com erro sob a nova regra que ainda não tiveram acerto na sessão. A pendência cujo último erro ocorreu há mais tempo tem prioridade. A separação escolhida foi de duas apresentações quando há conteúdo disponível. Se o assunto se esgotar ou o limite de dez for atingido, entra a pendência mais antiga, inclusive imediatamente se for a única opção. Cada avanço exige uma ação do aluno. Enquanto existir pendência, o aluno pode pausar e sair. O encerramento definitivo fica disponível quando as pendências forem resolvidas, conforme o fluxo de encerramento da atividade.

A primeira tentativa de cada questão na sessão mantém a regra de desempenho individual vigente, inclusive a proteção do primeiro erro. As demais tentativas ficam no histórico, com o nível que o aluno tinha naquele momento. A pontuação coletiva não participa desse fluxo. No erro, o serviço retira o agendamento corrente daquela questão e devolve uma dica sobre o assunto. No acerto, devolve a explicação completa. Um primeiro acerto ou um acerto após reaprendizagem agenda a revisão para amanhã. Acertos nas revisões avançam por 3, 7, 16, 35 e 70 dias, com teto de 70.

A V9 mantém as migrations anteriores e preenche o passo das sessões e respostas existentes usando a posição original da questão. Erros gravados antes da atualização conservam a agenda anterior e não criam pendências retroativas. Revisões legadas por atividade continuam vinculadas à atividade. Respostas em novos passos são únicas por sessão e passo. O bloqueio do aluno, a validação de titularidade e a transação já existentes abrangem resposta, desempenho e agenda.

## Contratos da API

| Operação | Comportamento |
|---|---|
| Iniciar, consultar ou retomar sessão | O estado ganha `passo`, `repetindo` e `pendencias`. O campo `sessao.questoesApresentadas` continua contando questões distintas. |
| `PUT /api/aluno/sessoes/{id}/questoes/{posicao}` | O cliente pede o próximo passo da apresentação, que pode exceder dez. Repetir um passo já alcançado devolve o estado atual. |
| `PUT /api/aluno/sessoes/{id}/questoes/{questaoId}/resposta` | O cliente envia, por exemplo, `{"alternativa":"B","passo":4}`, usando o passo recebido do servidor. Repetir o mesmo envio devolve o feedback registrado. Trocar a alternativa de um passo já respondido devolve 409. Clientes antigos que omitem o passo só conseguem registrar ou recuperar a primeira resposta àquela questão na sessão. |
| Feedback | `reaprender` identifica o erro sujeito à nova regra. `explicacao` traz uma dica no erro e a explicação completa no acerto. |
| Encerrar | Uma pendência de reaprendizagem impede o encerramento com HTTP 409. O endpoint de interrupção preserva a posição e o feedback. |
| Resultado e histórico | `apresentadas` e `respondidas` contam questões distintas. `acertos` e `erros` contam tentativas. O resultado detalha o `passo` de cada resposta e conserva o mesmo número da questão durante a repetição. A agenda lista cada questão uma vez. |

## Arquivos no pacote

Caminhos Java de produção a partir de `backend/src/main/java/com/educria/`.

| Arquivos | Finalidade |
|---|---|
| `domain/entity/SessaoEstudo.java`, `domain/entity/RespostaAluno.java` | Passo atual, questão reapresentada e registro individual da tentativa. |
| `domain/entity/RepeticaoEspacada.java`, `service/AgendaRevisoesService.java` | Retirar o agendamento no erro e aplicar a progressão oficial no acerto. |
| `service/SessaoEstudoService.java`, `repository/RespostaAlunoRepository.java` | Escolher a próxima questão ou pendência, validar o passo e devolver o feedback sob o bloqueio do titular. |
| `controller/RespostaSessaoController.java`, `dto/EnviarResposta.java`, `dto/EstadoSessao.java`, `dto/FeedbackResposta.java` | Receber o passo e expor o estado necessário para a tela. |
| `repository/HistoricoSessaoRepository.java`, `dto/ResultadoSessao.java` | Separar contagens, ordenar tentativas e evitar duplicar agendamentos no resultado. |

Demais caminhos a partir da raiz do projeto.

| Arquivos | Finalidade |
|---|---|
| `backend/src/main/resources/db/migration/V9__tentativas_da_sessao.sql` | Migrar sessões legadas e garantir unicidade no PostgreSQL. |
| `frontend/app/aluno/atividade/[id]/page.tsx`, `frontend/lib/estudo.ts` | Atalhos após clique na margem, dicas, contagem de pendências, passo no envio e seleção vazia em cada nova tentativa. |
| `frontend/app/aluno/historico/page.tsx`, `frontend/app/aluno/historico/[id]/page.tsx`, `frontend/lib/historico.ts` | Exibir questões distintas, total de tentativas e respostas agrupadas por questão. |
| `backend/src/test/java/com/educria/service/SessaoEstudoIT.java`, `backend/src/test/sql/verificar_v9.sql`, `frontend/tests/test_dashboard.py` | Cobrir a nova regra, o upgrade V8 → V9 e a interação no navegador. |
| `ATUALIZACAO-REAPRENDIZAGEM.md` | Este relatório e o guia de aplicação. |

## Validação desta entrega

- Java 21 e PostgreSQL 16. O pacote Spring Boot compilou. A suíte Maven completa de classes `*Test` e `*IT` executou 38 testes, todos aprovados. Com relógio controlado, a agenda percorreu os seis intervalos, permaneceu no teto, saiu da agenda após erro de revisão e reiniciou em um dia após o acerto.
- Um caso integrado percorreu 14 tentativas com 10 questões distintas e duas pendências simultâneas. Verificou novos erros, acerto posterior, pausa e retomada, conflito em reenvio alterado, idempotência, histórico e desempenho. A suíte também verificou isolamento entre alunos e concorrência de envios.
- Next.js e TypeScript. Build de produção e verificação de tipos aprovados. Sete testes no Chromium passaram, cinco com API simulada e dois com Spring Boot e PostgreSQL reais. O fluxo de reaprendizagem incluiu clique na margem, novo login em contexto limpo, retomada, reapresentação, acerto, recarga e consulta do histórico.
- Os três scripts SQL passaram no PostgreSQL, incluindo a aplicação da V9 sobre uma base V8 de teste com sessão interrompida, erro antigo e revisão agendada.
- O ambiente de execução dos testes foi Linux. Windows com 4 GB, Codespaces do usuário, aparelho físico e leitor de tela não foram testados nesta entrega.
