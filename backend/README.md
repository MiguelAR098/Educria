# EDUCRIA Backend – Stack B (Spring Boot / POO)

Sistema de acompanhamento de aprendizagem **adaptativo e gamificado** para Ensino Fundamental II.

## Destaques de Arquitetura

- **Economia Dupla**: isolamento total entre `DesempenhoAdaptativo` (privado) e `PontuacaoColetiva` (público/turma)
- **Motor de nivelamento** (1–10) com proteção contra rebaixamento no 1º erro
- **Repetição espaçada** com intervalos fixos: 1 → 3 → 7 → 16 → 35 → 70 dias
- **Spring Security + JWT** (stateless) + BCrypt
- **Spring Data JPA + Flyway**
- **Spring Retry + RabbitMQ** (preparado para e-mails)
- **@Transactional** nas operações críticas

## Stack

| Camada        | Tecnologia                          |
|---------------|-------------------------------------|
| Backend       | Spring Boot 3.3 + Java 21           |
| Segurança     | Spring Security + JWT (JJWT)        |
| Persistência  | PostgreSQL + Spring Data JPA + Flyway |
| Mensageria    | RabbitMQ                            |
| Resiliência   | Spring Retry                        |

## Como rodar

No Codespaces, siga o [guia de inicialização](../INICIAR-CODESPACES.md). O iniciador cria as credenciais de desenvolvimento, sobe o PostgreSQL e executa o backend em Java 21.

### Endpoints principais

| Método | Endpoint                        | Perfil   | Descrição                    |
|--------|---------------------------------|----------|------------------------------|
| POST   | /api/auth/register              | Público  | Cadastro de aluno            |
| POST   | /api/auth/login                 | Público  | Login (retorna JWT)          |
| GET    | /api/aluno/desempenho           | ALUNO    | Meu nível adaptativo         |
| GET    | /api/aluno/turma/pontos         | ALUNO    | Pontos coletivos da turma    |
| POST   | /api/admin/turmas               | ADMIN    | Criar turma                  |
| POST   | /api/admin/turmas/{id}/pontos   | ADMIN    | Lançar pontos coletivos      |

## Estrutura de pastas

```
com.educria
├── config/          → Security, RabbitMQ
├── domain/
│   ├── entity/      → Entidades com regras de negócio encapsuladas
│   └── enums/
├── repository/
├── service/
├── controller/
├── security/        → JWT + UserDetails
├── dto/
└── exception/
```

## Regras de negócio importantes

1. **DesempenhoAdaptativo.registrarResposta(boolean)**  
   - Acerto → sobe nível (máx 10) e zera erros consecutivos  
   - Erro → só rebaixa no **2º erro consecutivo**

2. **RepeticaoEspacada**  
   - Acerto → avança no intervalo (1→3→7→16→35→70)  
   - Erro → volta para 1 dia

3. **Isolamento**  
   Não existe FK nem serviço que una `DesempenhoAdaptativo` com `PontuacaoColetiva`.

## Questões de demonstração de Matemática

Na raiz do projeto, inicie o Codespace com o perfil de desenvolvimento. Se o iniciador já estiver rodando, encerre-o com Ctrl+C antes de executar:

```bash
SPRING_PROFILES_ACTIVE=dev bash scripts/iniciar.sh
```

A V3 acrescenta assunto, dificuldade e explicação a `questoes`. As colunas aceitam nulo para preservar questões anteriores à classificação. O banco aceita dificuldade de 1 a 10. As 80 questões do protótipo usam as dificuldades originais de 1 a 5, com duas questões em cada dificuldade de cada assunto. A ordem 1 a 80 corresponde a q1 até q80. O importador preserva as quatro alternativas, converte o índice correto de 0–3 para A–D e guarda a explicação no backend.

O perfil `dev` habilita um callback do Flyway após a migração. Ele reserva o ID −1 de `atividades` para o conjunto de demonstração e insere questões ausentes pela ordem dentro dessa atividade. O bloqueio da linha da atividade serializa cargas simultâneas. Se esse ID já estiver ocupado por outra atividade, a carga falha e a transação é revertida. Iniciar com o perfil padrão aplica o esquema. Dados de demonstração já carregados permanecem no banco. O cadastro de alunos e o dashboard continuam usando as mesmas APIs.

### Conferir a distribuição no Codespace

Com os serviços em execução, abra outro terminal na raiz:

```bash
docker compose exec -T db psql -U educria -d educria <<'SQL'
SELECT s.nome AS assunto, count(*) AS total,
       count(*) FILTER (WHERE q.dificuldade = 1) AS d1,
       count(*) FILTER (WHERE q.dificuldade = 2) AS d2,
       count(*) FILTER (WHERE q.dificuldade = 3) AS d3,
       count(*) FILTER (WHERE q.dificuldade = 4) AS d4,
       count(*) FILTER (WHERE q.dificuldade = 5) AS d5
FROM questoes q JOIN assuntos s ON s.id = q.assunto_id
WHERE q.atividade_id = -1
GROUP BY s.id, s.nome ORDER BY s.id;
SQL
```

O resultado esperado são oito linhas, com total 10 e contagens `2, 2, 2, 2, 2` em cada uma.

### Repetir os testes

Na raiz do Codespace, com o PostgreSQL iniciado:

```bash
mvn -f backend/pom.xml test
docker compose exec -T db mkdir -p /tmp/educria-teste
docker compose cp backend/src/. db:/tmp/educria-teste/
docker compose exec -T db psql -X -U educria -d educria -f /tmp/educria-teste/test/sql/verificar_carga_matematica.sql
```

O teste SQL cria um esquema temporário de teste dentro de uma transação, cria uma questão legada e executa o mesmo callback duas vezes. Ele compara todas as linhas e seus IDs antes e depois, confere a distribuição por assunto e dificuldade e termina em `ROLLBACK`. O teste Java `QuestaoJsonTest` verifica que a serialização omite tanto o gabarito quanto a explicação, que pode revelar a resposta.

Na validação desta entrega, o PostgreSQL 16 também iniciou com zero questões em uma base nova no perfil padrão e com 80 após ativar `dev`. Reiniciar em `dev` preservou o conteúdo e os IDs. Duas cargas simultâneas produziram as mesmas 80 questões. Enunciados, alternativas, gabaritos, explicações, assuntos, dificuldades e ordens foram comparados com o protótipo.

## Catálogo do aluno

O dashboard em `/aluno` consulta duas rotas privadas com o JWT de perfil ALUNO:

| GET | Resposta |
|---|---|
| `/api/aluno/materias` | Lista de matérias com `id`, `nome` e `totalAssuntos`. |
| `/api/aluno/materias/{materiaId}/assuntos` | Lista de assuntos com `id`, `nome` e `nivelAtual` do aluno autenticado. Uma matéria sem assuntos retorna uma lista vazia. Um ID de matéria inexistente retorna 404. |

Matemática mostra os oito níveis individuais, inclusive em uma base sem as questões de demonstração. A V4 cadastra Português e Ciências. Matérias sem assuntos aparecem como indisponíveis. Para verificar as rotas e a exigência de autenticação após iniciar a aplicação, rode `node scripts/verificar.mjs` na raiz.

## Seleção adaptativa de questões

O serviço Java `SelecaoAdaptativaService.selecionar(assuntoId, nivel, contexto)` retorna um `Optional<QuestaoParaAluno>`. O chamador informa o assunto escolhido, o nível atual de 1 a 10 e o contexto deste aluno:

- `usadas` reúne os IDs das questões já apresentadas nesta sequência.
- `proximasRevisoes` associa cada questão já vista à data da próxima revisão. Questões ausentes do mapa são tratadas como novas. A seleção recebe a agenda individual persistida para o assunto.
- `revisoesUsadas` conta as questões de revisão já apresentadas na sequência. `hoje` é a data de referência para verificar o vencimento.

Só participam questões classificadas, do assunto escolhido e de atividades ativas. Questões já usadas e revisões futuras ficam de fora. Até três revisões podem ser escolhidas por sequência. Dentro desse limite, uma revisão devida hoje ou antes tem prioridade sobre uma questão nova. Dentro de cada grupo, a seleção procura a dificuldade mais próxima do nível. Empates favorecem primeiro a dificuldade menor e depois o menor ID.

O retorno vazio indica esgotamento. A operação é de leitura. O DTO contém ID, assunto, dificuldade, enunciado e as quatro alternativas na ordem original. O repositório projeta diretamente esses campos, sem carregar gabarito ou explicação.

**Testes.** `mvn -f backend/pom.xml test` executa os casos de limites, empates, revisões, esgotamento e campos serializados. Para verificar também a consulta real com PostgreSQL, configure as variáveis do backend para uma base de testes e rode `mvn -f backend/pom.xml -Dtest=SelecaoAdaptativaIT test`. O Spring aplica as migrations e a transação do teste desfaz as linhas de exemplo.

## Sessões de estudo

Rotas sob `/api/aluno/sessoes` usam JWT de perfil ALUNO. O servidor obtém o titular do token. Uma sessão de outro aluno recebe a mesma resposta 404 de uma sessão inexistente.

| Método e caminho | Efeito |
|---|---|
| POST na coleção | `{"assuntoId":1}` escolhe assunto. `{}` solicita recomendação pelo menor nível pessoal entre assuntos com questões ativas, com desempate pelo menor ID. Retorna 201 com a primeira questão. |
| GET na coleção | Lista resumos do titular, mais recentes primeiro. |
| GET `/{id}` | Retorna `sessao` e `questaoAtual`, com as alternativas A–D. |
| PUT `/{id}/questoes/{posicao}` | Solicita a próxima posição (1 a 10). Em uma sessão ativa, repetir uma posição já apresentada devolve o estado atual. |
| PUT `/{id}/interromper` | Pausa e conserva a posição. |
| PUT `/{id}/retomar` | Reativa a sessão na mesma questão. |
| PUT `/{id}/encerrar` | Encerra definitivamente, inclusive se a sessão estiver pausada. |

A V5 persiste a sessão e a sequência de IDs apresentados. Cada aluno tem até uma sessão aberta (ativa ou interrompida). Repetir o início para o mesmo assunto ou por recomendação devolve a sessão aberta com 200. Mudar de assunto exige encerrar a sessão anterior. Esgotamento, posição fora de sequência e operações incompatíveis com o estado retornam 409. O contador indica questões apresentadas. O avanço consulta o nível atual no servidor e não grava respostas, pontos ou acertos. A seleção considera o vencimento das revisões por questão.

Para realizar testes com questões de demonstração, inicie com `SPRING_PROFILES_ACTIVE=dev bash scripts/iniciar.sh`. Em uma base PostgreSQL de teste com as variáveis do backend configuradas, execute `mvn -f backend/pom.xml -Dtest=SessaoEstudoIT test` para verificar autenticação, isolamento, concorrência, limite, interrupção e retomada.

## Enviar uma resposta

Use `PUT /api/aluno/sessoes/{id}/questoes/{questaoId}/resposta` com o JWT do aluno e um corpo como `{"alternativa":"B"}`. Use o ID de `questaoAtual` retornado pela sessão. O primeiro envio exige a questão atual de uma sessão ativa. O retorno HTTP 200 contém a alternativa enviada, o booleano `correta` e a `explicacao` cadastrada. A consulta da sessão inclui o campo opcional `feedback` somente quando o aluno já respondeu à questão atual naquela sessão.

Reenviar a mesma letra recupera a correção gravada, inclusive após avançar ou encerrar. Tentar trocá-la retorna 409. Letras fora de A–D e opções indisponíveis retornam 400. Uma sessão de outro aluno ou uma questão que não foi apresentada nela retorna 404. A V6 mantém as respostas anteriores e garante uma resposta por questão em cada sessão.

Na raiz do projeto, com PostgreSQL de teste preparado, rode `mvn -f backend/pom.xml -Dtest=SessaoEstudoIT test`. A suíte cobre envios simultâneos, isolamento por aluno, validação e recuperação do feedback.

## Evolução individual por assunto

O primeiro envio válido atualiza a resposta e o diagnóstico do aluno no assunto da sessão na mesma transação. O bloqueio do aluno serializa envios concorrentes. A seleção da próxima questão usa o nível atualizado. A regra compartilhada com o diagnóstico global sobe um nível por acerto, até 10, e zera a sequência de erros. O primeiro erro consecutivo conserva o nível. O segundo reduz um nível quando ele está acima de 1. Os contadores e a proteção pertencem a cada aluno e assunto, inclusive entre sessões.

O feedback inclui `nivelAnterior` e `nivelAtual` gravados com a resposta. Um reenvio devolve os níveis daquele momento. A V7 preserva o histórico anterior sem recalculá-lo. Respostas anteriores à migração continuam com o feedback original. Esta operação escreve apenas na resposta e no desempenho por assunto.

**Decisões pendentes da equipe.** O PDF de requisitos estabelece a escala e a proteção do primeiro erro. Exigir dois acertos para subir, penalizar respostas rápidas e definir uma fórmula de pontos privados são propostas do protótipo que aguardam aprovação. Nenhuma delas participa do cálculo atual.

## Revisões por questão

A V8 estende `repeticao_espacada` com `questao_id`. Cada linha identifica exatamente uma atividade legada ou uma questão, com unicidade por aluno e alvo. O assunto vem da própria questão. As datas das atividades antigas são preservadas. A agenda por questão começa com as novas respostas, sem distribuir as datas legadas ou recalcular respostas anteriores. A sessão também registra quantas revisões já apresentou para conservar a cota de três ao pausar e retomar.

A primeira resposta válida agenda a questão para o dia seguinte. Em revisões posteriores, o acerto avança pelos intervalos de **1, 3, 7, 16, 35 e 70 dias** e permanece em 70 no teto. Todo erro agenda novamente em um dia. Os dias são contados a partir da data da resposta em `America/Sao_Paulo`. Resposta, desempenho e agenda são gravados na mesma transação. Reenviar a resposta recupera o feedback e conserva o vencimento original.

O aluno autenticado acessa `GET /api/aluno/revisoes` ou **Minhas revisões** no painel. A resposta contém `hoje`, `proximaVirada`, `pendentes` (vencimento até hoje) e `futuras` (depois de hoje), em ordem de data e ID. Cada item traz questão, enunciado, assunto, intervalo em dias e vencimento. A lista considera questões classificadas em atividades ativas. O servidor decide a elegibilidade e o navegador consulta a agenda novamente na virada do dia.

Em um banco PostgreSQL de teste, execute `mvn -f backend/pom.xml -Dtest=SessaoEstudoIT test`. O teste usa um relógio controlado para atravessar a meia-noite de 31 de dezembro em São Paulo e percorrer toda a sequência, inclusive teto, reinício em um dia, isolamento e reenvio.

## Resultado e histórico

As duas consultas usam o aluno identificado pelo JWT.

- `GET /api/aluno/sessoes/historico` lista as sessões da mais recente para a mais antiga. Cada página traz até 20 itens em `sessoes`. Quando houver `proximoCursor`, passe seu valor em `?antesDe=...` para continuar.
- `GET /api/aluno/sessoes/{id}/resultado` retorna `sessao`, `anterior`, `revisoes` e `questoes`. Cada questão apresentada inclui seu número, enunciado, alternativa enviada e correção registrada. Uma questão sem resposta tem `alternativa` e `correta` nulas. A comparação usa a sessão imediatamente anterior do mesmo titular, inclusive de outro assunto. A primeira tem `anterior: null`. Consultar o ID de outro aluno retorna 404.

Acertos e erros contam somente respostas persistidas. O resultado de uma sessão ativa ou interrompida é parcial. O nível vem do registro da primeira e da última resposta na ordem de apresentação. Extremos sem registro ficam nulos. As questões respondidas com revisão na agenda atual exibem o vencimento vigente, que pode mudar após outra sessão. A fórmula de pontos privados permanece pendente.

Com um PostgreSQL de teste configurado, rode `mvn -f backend/pom.xml -Dtest=SessaoEstudoIT test` para verificar histórico, resultados e isolamento.

## Próximos passos sugeridos

- CRUD de atividades/questões (admin)
- Scheduler para notificar revisões pendentes
- Testes unitários das regras de negócio
- Integração futura com RabbitMQ no ambiente de desenvolvimento
