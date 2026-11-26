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
| Persistência  | SQL Server + Spring Data JPA + Flyway |
| Mensageria    | RabbitMQ                            |
| Resiliência   | Spring Retry                        |

## Como rodar

O backend usa SQL Server e aplica as migrations de `db/sqlserver/migration`. O iniciador do Codespaces prepara o SQL Server de desenvolvimento e executa o backend em Java 21. [Codespaces](../INICIAR-CODESPACES.md) · [Windows e testes de integração](../docs/sqlserver/README.md)

### Endpoints principais

| Método | Endpoint                        | Perfil   | Descrição                    |
|--------|---------------------------------|----------|------------------------------|
| POST   | /api/auth/register              | Público  | Cadastro de aluno            |
| POST   | /api/auth/login                 | Público  | Login (retorna JWT)          |
| GET    | /api/aluno/desempenho           | ALUNO    | Meu nível adaptativo         |
| GET    | /api/aluno/turma/pontos         | ALUNO    | Pontos coletivos da turma    |
| GET    | /api/professor/turmas/{turmaId}/rankings | PROFESSOR | Rankings privados da turma na matéria do professor |
| GET    | /api/professor/grupos           | PROFESSOR | Grupos e projetos próprios com contexto autorizado |
| POST   | /api/admin/turmas               | ADMIN    | Criar turma                  |
| POST   | /api/admin/turmas/{id}/pontos   | ADMIN    | Lançar pontos coletivos      |
| DELETE | /api/admin/turmas/{turmaId}/alunos/{alunoId} | ADMIN | Remover o vínculo atual do aluno com a turma |

O DELETE retorna 204 quando remove o vínculo e 404 quando ele está ausente ou aponta para outra turma. A conta, as sessões, as respostas e a pontuação coletiva permanecem disponíveis. `Usuario` é a entidade abstrata comum a `Aluno`, `Professor` e `Admin`.

O cadastro de aluno aceita e-mail de até 100 caracteres, matrícula opcional de até 30 caracteres e senha de 6 caracteres a 72 bytes em UTF-8. Dados fora desses limites retornam `400`. Cadastros concorrentes com o mesmo e-mail retornam `201` para a conta criada e `409` para o segundo pedido. O login de contas existentes conserva o contrato atual.

Em `GET /api/professor/turmas/{turmaId}/alunos/{alunoId}/recados`, o histórico contém os recados enviados pelo professor autenticado ao aluno daquela turma. Em `GET /api/aluno/recados`, o aluno recebe os recados endereçados a ele.

A exportação do banco de questões inclui as questões ativas da matéria com dificuldade, quatro alternativas preenchidas e gabarito A–D. Questões legadas incompletas permanecem no banco e ficam fora desse arquivo. A importação valida a faixa dos números inteiros antes de convertê-los.

Lançamentos coletivos que excederiam o limite do saldo inteiro retornam `409`. A transação preserva o saldo e qualquer atividade associada ao pedido rejeitado fica sem registro.

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

1. **DesempenhoPorAssunto.registrarResposta(boolean)**
   - Acerto → sobe nível (máx 10) e zera erros consecutivos  
   - Erro → só rebaixa no **2º erro consecutivo**

2. **RepeticaoEspacada**  
   - Acerto → avança no intervalo (1→3→7→16→35→70)  
   - Erro → volta para 1 dia

3. **Isolamento**  
   Não existe FK nem serviço que una `DesempenhoAdaptativo` com `PontuacaoColetiva`.

## Questões de demonstração de Matemática

Inicie somente o backend no perfil `dev`, apontado para uma base descartável marcada. [Configuração no Codespaces e Windows](../docs/sqlserver/README.md)

Uma base limpa com o perfil `dev` recebe **160 questões de Matemática**. Cada um dos oito assuntos originais tem duas questões em cada dificuldade de 1 a 10, somando 20 por assunto. As ordens 1 a 80 vêm do protótipo. As ordens 81 a 160 acrescentam dez questões por assunto nas dificuldades 6 a 10. Português e Ciências continuam com o catálogo existente.

As dificuldades 6 e 7 combinam interpretação e operações encadeadas. A dificuldade 8 pede que o estudante escolha como organizar relações e quantidades. As dificuldades 9 e 10 acrescentam condições simultâneas, cálculos inversos, divisão com restrições ou comparação de cenários. Cada questão tem quatro alternativas, uma resposta correta e uma resolução. `GabaritoDemoMatematicaTest` confere os 160 resultados e detecta alternativas numericamente equivalentes. Em novas inserções, a questão 26 ganha quatro alternativas distintas e as questões 71 e 140 explicitam as condições de seus cálculos. A revisão pedagógica humana continua necessária antes do uso com estudantes reais.

O Flyway versiona o esquema de V1 a V5. O perfil `dev` habilita o callback `afterMigrate__questoes_de_matematica.sql`, que usa a atividade de ID −1. Em uma transação protegida por `sp_getapplock`, insere as ordens ausentes. Uma questão já presente conserva ID, enunciado, gabarito e estado de ativação. Se a atividade de ID −1 pertencer a outro conteúdo, a carga é revertida. A execução no perfil padrão aplica o esquema e deixa o catálogo de questões como está. A meta pedagógica de seis questões por combinação permanece em aberto.

### Conferir a distribuição no Codespace

Com os serviços em execução, abra outro terminal na raiz:

```bash
docker compose exec -T db sh -c 'export SQLCMDPASSWORD="$MSSQL_SA_PASSWORD"; exec /opt/mssql-tools18/bin/sqlcmd -S localhost -C -U sa -d educria_demo_apresentacao -b' <<'SQL'
SELECT s.nome AS assunto, q.dificuldade, count(*) AS total
FROM dbo.questoes q JOIN dbo.assuntos s ON s.id = q.assunto_id
WHERE q.atividade_id = -1
GROUP BY s.id, s.nome, q.dificuldade ORDER BY s.id, q.dificuldade;
GO
SQL
```

O resultado esperado para a carga original ampliada são 80 linhas, cada uma com total 2. A matriz da API conta questões elegíveis, portanto pode divergir depois de edições, desativações ou inclusão de questões próprias.

### Repetir os testes

Na raiz do projeto:

```bash
mvn -B -f backend/pom.xml test
```

O teste `QuestaoJsonTest` verifica que a serialização omite o gabarito e a explicação. Para rodar os testes integrados, configure `EDUCRIA_SQLSERVER_TEST_URL` com uma base local `educria_sqlserver_teste_<sufixo>` marcada com a propriedade `EducriaAmbiente=descartavel`. Configure `EDUCRIA_SQLSERVER_TEST_USER` e `EDUCRIA_SQLSERVER_TEST_PASSWORD` conforme a autenticação local. Execute `mvn -B -f backend/pom.xml '-Dtest=*Test,*IT' test`. Confira que os `*IT` foram executados no resumo.

## Rankings pedagógicos privados

`GET /api/professor/turmas/{turmaId}/rankings` exige JWT de professor vinculado à turma. O servidor obtém a matéria do perfil do professor. A resposta tem `turmaId`, `materia` com `id` e `nome`, `periodoEvolucaoDias` e as listas `rankingPontuacao` e `rankingEvolucao`. Cada linha contém `posicao`, `alunoId`, `nome` e a medida `pontos` ou `evolucao30d`. Uma turma sem estudantes retorna duas listas vazias. A resposta usa `Cache-Control: no-store`.

O ranking de pontos soma as respostas que registraram pontuação, pelo assunto histórico da sessão na matéria do professor. Zero registrado é um valor válido. O ranking de evolução reutiliza o `evolucao30d` do panorama, com a mesma janela e fórmula. Respostas de sessão seguem o assunto histórico. Respostas legadas sem sessão mantêm a associação à questão. Pontos coletivos ficam fora dos dois rankings.

Os valores aparecem em ordem decrescente, incluindo zero e valores negativos na evolução. Empates compartilham a posição (`1, 1, 3`). Dentro do empate, a exibição ordena por nome sem distinguir caixa e depois por ID. Estudantes sem dados aparecem ao final com medida e posição nulas.

| HTTP | Situação |
|---|---|
| `200` | Professor vinculado à turma |
| `400` | `turmaId` inválido para o tipo numérico |
| `401` | JWT inválido ou expirado |
| `403` | Ausência de JWT, perfil sem acesso ou turma fora dos vínculos do professor, inclusive ID inexistente |
| `404` | Professor autenticado sem registro correspondente |

### Demonstração com curl

Com o backend no perfil `dev` apontando para `educria_demo_apresentacao`, execute `bash scripts/sqlserver/carregar-demo.sh --desenvolvimento --banco educria_demo_apresentacao` na raiz. Use o nome da base descartável marcada à qual a API está conectada. O roteiro usa as contas de demonstração e `jq`. Informe as senhas dessas contas quando o terminal solicitar. O backend roda no loopback e os JWTs ficam em variáveis da sessão de terminal.

```bash
API=http://127.0.0.1:8080
read -rsp 'Senha do professor de demonstração: ' EDUCRIA_DEMO_SENHA_PROFESSOR
printf '\n'
read -rsp 'Senha do aluno de demonstração: ' EDUCRIA_DEMO_SENHA_ALUNO
printf '\n'
login() {
  jq -n --arg email "$1" --arg senha "$2" '{email:$email, senha:$senha}' |
    curl -fsS "$API/api/auth/login" -H 'Content-Type: application/json' --data-binary @- |
    jq -er .token
}

PROFESSOR=$(login professor@educria.com "$EDUCRIA_DEMO_SENHA_PROFESSOR")
PERFIL=$(curl -fsS "$API/api/professor/perfil" -H "Authorization: Bearer $PROFESSOR")
jq '.turmas' <<< "$PERFIL"
TURMA=$(jq -er '.turmas[] | select(.codigo == "DEMO-8A") | .id' <<< "$PERFIL")
RANKINGS="$API/api/professor/turmas/$TURMA/rankings"
curl -fsS "$RANKINGS" -H "Authorization: Bearer $PROFESSOR" | jq
curl -fsS "$API/api/professor/questoes/cobertura" -H "Authorization: Bearer $PROFESSOR" | jq

ALUNO=$(login diego.demo@educria.com "$EDUCRIA_DEMO_SENHA_ALUNO")
ESTADO=$(curl -fsS "$API/api/aluno/sessoes" -H "Authorization: Bearer $ALUNO" \
  -H 'Content-Type: application/json' -d '{}')
jq '{sessao, questaoAtual, pontosSessao}' <<< "$ESTADO"
SESSAO=$(jq -er '.sessao.id' <<< "$ESTADO")
QUESTAO=$(jq -er '.questaoAtual.id' <<< "$ESTADO")
PASSO=$(jq -er '.passo' <<< "$ESTADO")
read -r -p 'Escolha a letra da alternativa (A–D): ' LETRA
jq -n --arg alternativa "$LETRA" --argjson passo "$PASSO" '{alternativa:$alternativa, passo:$passo}' |
  curl -fsS -X PUT "$API/api/aluno/sessoes/$SESSAO/questoes/$QUESTAO/resposta" \
    -H "Authorization: Bearer $ALUNO" -H 'Content-Type: application/json' --data-binary @- |
  jq '{correta, pontosConcedidos}'
curl -fsS "$API/api/aluno/sessoes/$SESSAO/resultado" -H "Authorization: Bearer $ALUNO" |
  jq '{sessao: .sessao.pontosIndividuais, pontosIndividuaisNoAssunto, pontosIndividuaisNaMateria}'
curl -fsS "$RANKINGS" -H "Authorization: Bearer $PROFESSOR" | jq '.rankingPontuacao'
curl -sS -o /dev/null -w 'Ranking como aluno: HTTP %{http_code}\n' \
  "$RANKINGS" -H "Authorization: Bearer $ALUNO"
```

O comando abaixo também verifica o ranking e a cobertura em uma demonstração limpa, através do proxy local já iniciado. Execute-o após informar a senha do professor no mesmo terminal.

```bash
EDUCRIA_VERIFICAR_DEMO=1 EDUCRIA_DEMO_SENHA_PROFESSOR="$EDUCRIA_DEMO_SENHA_PROFESSOR" node scripts/verificar.mjs
```
 `EDUCRIA_VERIFICAR_BANCO` permite apontar as verificações SQL para uma base de demonstração descartável correspondente à API.

## Grupos e projetos pedagógicos

As rotas usam `Authorization: Bearer <JWT>` de professor ativo. O servidor identifica o responsável e sua matéria pelo titular autenticado. Um grupo pode abranger até 50 turmas em que o responsável leciona. Tipo, responsável e turmas ficam fixos. O histórico de participação registra a turma de origem e as datas de entrada e saída. Uma nova entrada após a saída cria outro vínculo histórico.

| Método | Caminho a partir de `/api/professor/grupos` | Entrada e retorno |
|---|---|---|
| `POST` | `/` | `{"nome":"Monitoria","descricao":"Opcional","tipo":"PROJETO","turmasIds":[7]}` → `201` com resumo |
| `GET` | `/` | Filtros opcionais `?tipo=GRUPO&arquivado=false` → `200` com resumos |
| `GET` | `/{id}` | `200` com `grupo` e `participantes` |
| `PUT` | `/{id}` | `{"nome":"Monitoria de Matemática","descricao":null}` → `200` com resumo |
| `POST` | `/{id}/participantes` | `{"alunosIds":[11,12]}` → `200` com detalhe, até 100 IDs por lote |
| `DELETE` | `/{id}/participantes/{alunoId}` | `204` após encerrar o vínculo ativo |
| `PATCH` | `/{id}/arquivar` | `200` com resumo e `arquivadoEm` preenchido |
| `GET` | `/{id}/indicadores` | `200` com os agregados privados da matéria do responsável |

O resumo inclui `id`, `nome`, `descricao`, `tipo`, `criadoEm`, `arquivadoEm` e `turmas` com `id` e `nome`. O detalhe lista apenas participantes ativos que ainda pertencem ao contexto autorizado, com `alunoId`, `nome`, `turmaOrigemId`, `turmaAtualId` e `entradaEm`. Um grupo vazio retorna uma lista vazia. A inclusão é atômica e repetir a inclusão de um aluno elegível mantém o mesmo vínculo. Repetir uma saída de aluno ainda elegível retorna `204`. Arquivar é idempotente e mantém o histórico e as consultas. Grupos arquivados rejeitam edições e alterações de participantes.

Corpos inválidos retornam `400`. JWT inválido, expirado ou de conta desativada retorna `401`. Perfil sem acesso, ausência de JWT e seleção de turma alheia na criação retornam `403`. Um ID inexistente, contexto que perdeu autorização ou grupo de outro professor retorna `404`, assim como aluno inelegível em um lote. Alterar um grupo arquivado retorna `409`. A perda de vínculo com qualquer turma do contexto oculta o grupo da listagem e impede o acesso direto. Alunos transferidos para fora ou desvinculados ficam fora das respostas e métricas.

Os indicadores trazem `grupoId`, `nome`, `tipo`, `materia`, `periodo`, `comparacaoEscopo`, `turmasDeOrigem`, `participantes` e `naoParticipantes`. Cada turma de origem tem `id`, `nome` e quantidade de participantes. Cada recorte tem `total` e medidas `nivel` e `evolucao30d`. Exemplo de um recorte com um aluno sem dados:

```json
{
  "total": 2,
  "nivel": { "media": 4.5, "comDados": 1, "semDados": 1 },
  "evolucao30d": { "media": null, "comDados": 0, "semDados": 2 }
}
```

O nível médio considera os assuntos praticados na matéria do professor e dá o mesmo peso a cada estudante com dados. A evolução reutiliza a regra do panorama, que divide a soma das variações pelo total de assuntos da matéria e exige resposta na janela e pelo menos um assunto com nível comparável. O período explicita o início em `hoje − 30 dias`, o início de amanhã como limite exclusivo e o fuso `America/Sao_Paulo`, como no panorama. A média de cada recorte usa somente estudantes com dados para a medida. Zero é uma observação válida. `null` indica ausência de dados.

A comparação `GRUPOS_ATIVOS_DO_PROFESSOR_NAS_TURMAS_DO_GRUPO` considera alunos ativos das mesmas turmas sem participação elegível em qualquer grupo ativo desse professor. Um aluno em mais de um grupo conta uma vez. O resultado é descritivo e fica restrito ao recorte do responsável. Na consulta de um grupo arquivado, seus participantes elegíveis continuam em seu recorte e ficam fora do comparador. A criação e a participação mantêm os saldos de pontos existentes.

## Apresentação pelo backend

Prepare uma base descartável marcada e use o perfil `dev` conforme o [guia de execução SQL Server](../docs/sqlserver/README.md). Confirme no log do Spring Boot que o Flyway aplicou ou validou V1 a V5. Carregue as contas de demonstração nessa mesma base. O roteiro usa apenas `curl`, `jq` e o backend. Informe as senhas das contas de demonstração quando o terminal solicitar.

```bash
API=http://127.0.0.1:8080
read -rsp 'Senha do professor de demonstração: ' EDUCRIA_DEMO_SENHA_PROFESSOR
printf '\n'
read -rsp 'Senha do aluno de demonstração: ' EDUCRIA_DEMO_SENHA_ALUNO
printf '\n'
login() {
  jq -n --arg email "$1" --arg senha "$2" '{email:$email, senha:$senha}' |
    curl -fsS "$API/api/auth/login" -H 'Content-Type: application/json' --data-binary @-
}
aluno=$(login diego.demo@educria.com "$EDUCRIA_DEMO_SENHA_ALUNO")
aluno_id=$(jq -er .id <<< "$aluno")
aluno_jwt=$(jq -er .token <<< "$aluno")
materia_id=$(curl -fsS "$API/api/aluno/materias" -H "Authorization: Bearer $aluno_jwt" |
  jq -er '.[] | select(.nome == "Matemática") | .id')
assunto_id=$(curl -fsS "$API/api/aluno/materias/$materia_id/assuntos" -H "Authorization: Bearer $aluno_jwt" |
  jq -er '.[] | select(.nome == "somar e subtrair") | .id')
sessoes="$API/api/aluno/sessoes"
estado=$(jq -n --argjson assuntoId "$assunto_id" '{assuntoId:$assuntoId}' |
  curl -fsS "$sessoes" -H "Authorization: Bearer $aluno_jwt" -H 'Content-Type: application/json' --data-binary @-)
sessao_id=$(jq -er .sessao.id <<< "$estado")

# Escolha um erro na primeira questão, acerte as duas seguintes e então a questão reapresentada.
for tentativa in 1 2 3 4; do
  jq '{questaoAtual, passo, pontosSessao}' <<< "$estado"
  read -r -p 'Alternativa (A–D): ' letra
  questao_id=$(jq -er .questaoAtual.id <<< "$estado")
  passo=$(jq -er .passo <<< "$estado")
  jq -n --arg alternativa "$letra" --argjson passo "$passo" '{alternativa:$alternativa, passo:$passo}' |
    curl -fsS -X PUT "$sessoes/$sessao_id/questoes/$questao_id/resposta" \
      -H "Authorization: Bearer $aluno_jwt" -H 'Content-Type: application/json' --data-binary @- | jq
  if (( tentativa < 4 )); then
    estado=$(curl -fsS -X PUT "$sessoes/$sessao_id/questoes/$((passo+1))" -H "Authorization: Bearer $aluno_jwt")
  fi
done
curl -fsS -X PUT "$sessoes/$sessao_id/encerrar" -H "Authorization: Bearer $aluno_jwt" | jq .sessao
curl -fsS "$sessoes/$sessao_id/resultado" -H "Authorization: Bearer $aluno_jwt" | jq .sessao
for rota in sessoes/historico evolucao revisoes turma/pontos; do
  curl -fsS "$API/api/aluno/$rota" -H "Authorization: Bearer $aluno_jwt" | jq
done

professor_jwt=$(login professor@educria.com "$EDUCRIA_DEMO_SENHA_PROFESSOR" | jq -er .token)
outro_jwt=$(login portugues@educria.com "$EDUCRIA_DEMO_SENHA_PROFESSOR" | jq -er .token)
professor="$API/api/professor"
perfil=$(curl -fsS "$professor/perfil" -H "Authorization: Bearer $professor_jwt")
jq '.turmas' <<< "$perfil"
turma_id=$(jq -er '.turmas[] | select(.codigo == "DEMO-8A") | .id' <<< "$perfil")
curl -fsS "$professor/turmas/$turma_id/rankings" -H "Authorization: Bearer $professor_jwt" | jq
curl -fsS "$professor/questoes/cobertura" -H "Authorization: Bearer $professor_jwt" | jq
grupo=$(jq -n --argjson turma "$turma_id" '{nome:"Monitoria",tipo:"PROJETO",turmasIds:[$turma]}' |
  curl -fsS "$professor/grupos" -H "Authorization: Bearer $professor_jwt" \
    -H 'Content-Type: application/json' --data-binary @-)
grupo_url="$professor/grupos/$(jq -er .id <<< "$grupo")"
jq -n --argjson aluno "$aluno_id" '{alunosIds:[$aluno]}' |
  curl -fsS "$grupo_url/participantes" -H "Authorization: Bearer $professor_jwt" \
    -H 'Content-Type: application/json' --data-binary @- | jq
curl -fsS "$grupo_url/indicadores" -H "Authorization: Bearer $professor_jwt" | jq
curl -sS -o /dev/null -w 'Outro professor: HTTP %{http_code}\n' "$grupo_url/indicadores" \
  -H "Authorization: Bearer $outro_jwt"
```

Após substituir `@grupo` e `@sessao` pelos IDs exibidos, confira na base de demonstração:

```sql
SELECT g.id, g.nome, p.aluno_id, p.turma_origem_id, p.entrada_em, p.saida_em
FROM dbo.grupos_pedagogicos g
JOIN dbo.grupo_participacoes p ON p.grupo_id = g.id
WHERE g.id = @grupo;

SELECT r.sessao_id, SUM(CONVERT(bigint, r.pontos_individuais)) AS pontos_privados
FROM dbo.respostas_aluno r WHERE r.sessao_id = @sessao GROUP BY r.sessao_id;

SELECT pc.turma_id, pc.pontos_totais AS pontos_coletivos
FROM dbo.pontuacao_coletiva pc JOIN dbo.grupo_turmas gt ON gt.turma_id = pc.turma_id
WHERE gt.grupo_id = @grupo;
```

Na apresentação, mostre o fluxo Controller → Service → Repository → SQL Server. O filtro JWT estabelece o usuário em cada requisição. A autorização por turma acontece no serviço. A transação agrupa resposta, desempenho, revisão e pontuação privada. O Flyway tem a responsabilidade de versionar o schema. `Aluno`, `Professor` e `Admin` especializam `Usuario` com tabelas ligadas à identidade de `usuarios`.

## Catálogo do aluno

O dashboard em `/aluno` consulta duas rotas privadas com o JWT de perfil ALUNO:

| GET | Resposta |
|---|---|
| `/api/aluno/materias` | Lista de matérias com `id`, `nome`, `totalAssuntos` e `pontosIndividuais` do aluno autenticado. |
| `/api/aluno/materias/{materiaId}/assuntos` | Lista de assuntos com `id`, `nome`, `nivelAtual` e `pontosIndividuais` do aluno autenticado. Uma matéria sem assuntos retorna uma lista vazia. Um ID de matéria inexistente retorna 404. |
| `/api/aluno/desempenho` | Indicadores do aluno autenticado, incluindo o total geral em `pontosIndividuais`. |

Matemática mostra os oito níveis individuais, inclusive em uma base sem as questões de demonstração. O catálogo inicial da migration V2 cadastra também Português e Ciências. Matérias sem assuntos aparecem como indisponíveis. Para verificar as rotas e a exigência de autenticação no ambiente de desenvolvimento do Codespaces, rode `node scripts/verificar.mjs` na raiz.

## Seleção adaptativa de questões

O serviço Java `SelecaoAdaptativaService.selecionar(assuntoId, nivel, contexto)` retorna um `Optional<QuestaoParaAluno>`. O chamador informa o assunto escolhido, o nível atual de 1 a 10 e o contexto deste aluno:

- `usadas` reúne os IDs das questões já apresentadas nesta sequência.
- `proximasRevisoes` associa cada questão já vista à data da próxima revisão. Questões ausentes do mapa são tratadas como novas. A seleção recebe a agenda individual persistida para o assunto.
- `revisoesUsadas` conta as questões de revisão já apresentadas na sequência. `hoje` é a data de referência para verificar o vencimento.

Só participam questões classificadas, do assunto escolhido e de atividades ativas. Questões já usadas e revisões futuras ficam de fora. Até três revisões podem ser escolhidas por sequência. Dentro desse limite, uma revisão devida hoje ou antes tem prioridade sobre uma questão nova. Dentro de cada grupo, a seleção procura a dificuldade mais próxima do nível. Empates favorecem primeiro a dificuldade menor e depois o menor ID.

O retorno vazio indica esgotamento. A operação é de leitura. O DTO contém ID, assunto, dificuldade, enunciado e as quatro alternativas na ordem original. O repositório projeta diretamente esses campos, sem carregar gabarito ou explicação.

**Testes.** `mvn -f backend/pom.xml test` executa os casos de limites, empates, revisões, esgotamento e campos serializados. `SelecaoAdaptativaIT` também verifica a consulta real em uma base SQL Server descartável. [Configuração de integração](../docs/sqlserver/README.md)

## Sessões de estudo

Rotas sob `/api/aluno/sessoes` usam JWT de perfil ALUNO. O servidor obtém o titular do token. Uma sessão de outro aluno recebe a mesma resposta 404 de uma sessão inexistente.

| Método e caminho | Efeito |
|---|---|
| POST na coleção | `{"assuntoId":1}` escolhe assunto. `{}` solicita recomendação pelo menor nível pessoal entre assuntos com questões ativas, com desempate pelo menor ID. Retorna 201 com a primeira questão. |
| GET na coleção | Lista resumos do titular, mais recentes primeiro. |
| GET `/{id}` | Retorna `sessao`, `pontosSessao` e `questaoAtual`, com as alternativas A–D. |
| PUT `/{id}/questoes/{posicao}` | Solicita o próximo passo. A sessão admite até dez questões distintas e passos adicionais de reaprendizagem. |
| PUT `/{id}/interromper` | Pausa e conserva a posição. |
| PUT `/{id}/retomar` | Reativa a sessão na mesma questão. |
| PUT `/{id}/encerrar` | Encerra definitivamente, inclusive se a sessão estiver pausada. |

O schema persiste a sessão e a sequência de IDs apresentados. Cada aluno tem até uma sessão aberta (ativa ou interrompida). Repetir o início para o mesmo assunto ou por recomendação devolve a sessão aberta com 200. Mudar de assunto exige encerrar a sessão anterior. Esgotamento, posição fora de sequência e operações incompatíveis com o estado retornam 409. O contador indica questões apresentadas. O avanço consulta o nível atual no servidor e a seleção considera o vencimento das revisões por questão.

Para carregar questões de demonstração, inicie o JAR no perfil `dev` com as variáveis da base descartável. `SessaoEstudoIT` verifica autenticação, isolamento, concorrência, limite, interrupção e retomada em SQL Server. [Comandos para a demonstração e os testes de integração](../docs/sqlserver/README.md)

## Enviar uma resposta

Use `PUT /api/aluno/sessoes/{id}/questoes/{questaoId}/resposta` com o JWT do aluno e um corpo como `{"alternativa":"B"}`. Use o ID de `questaoAtual` retornado pela sessão. O primeiro envio exige a questão atual de uma sessão ativa. O retorno HTTP 200 contém a alternativa enviada, o booleano `correta` e a `explicacao` cadastrada. A consulta da sessão inclui o campo opcional `feedback` somente quando o aluno já respondeu à questão atual naquela sessão.

Reenviar a mesma letra para o mesmo passo recupera a correção gravada, inclusive após avançar ou encerrar. Tentar trocá-la retorna 409. Letras fora de A–D e opções indisponíveis retornam 400. Uma sessão de outro aluno ou uma questão que não foi apresentada nela retorna 404. O schema mantém as tentativas e garante uma resposta por passo da sessão.

`SessaoEstudoIT` cobre envios simultâneos, isolamento por aluno, validação e recuperação do feedback.

## Evolução individual por assunto

O cadastro inicializa o diagnóstico por assunto pelo trigger da V2. A sessão atualiza o diagnóstico do assunto da sessão em cada primeira tentativa. Reaprendizagem e reenvios preservam esse diagnóstico. O registro de `DesempenhoAdaptativo` fica disponível como legado e só é consultado pelo titular em `GET /api/aluno/desempenho` quando ainda não há assunto praticado.

No resumo privado do aluno, `GET /api/aluno/desempenho` soma acertos e erros do diagnóstico de todos os assuntos praticados. Na lista e no detalhe da turma, o professor recebe a mesma leitura limitada à sua matéria. O `nivelAtual` inteiro exibe a média dos níveis dos assuntos praticados, arredondada para o inteiro mais próximo. `nivelMedio` da turma dá o mesmo peso a cada aluno com dados e usa a média individual antes do arredondamento. O nível inicial é `1` e a taxa é `null` quando faltam tentativas no recorte do professor. `ultimaAtividade` vem da última resposta da matéria, inclusive reaprendizagem, e fica `null` quando não há resposta registrada.

O contador de erros consecutivos pertence ao assunto. Com um assunto praticado, o resumo exibe seu contador. Com vários assuntos, o campo inteiro mantém `0` como valor neutro e a situação usa a taxa agregada segundo os limites existentes. O detalhe por assunto conserva os contadores reais. A definição pedagógica de uma sequência entre assuntos permanece pendente.

Em `GET /api/professor/turmas/{turmaId}/alunos/{alunoId}`, o relatório usa somente sessões e respostas da matéria do professor autenticado. O assunto histórico da sessão continua valendo se a questão foi editada. A rota privada do aluno continua abrangendo todas as suas matérias, com filtro opcional por assunto.

O relatório individual mostra 30 dias de calendário incluindo hoje, contagens de tentativas e níveis observados. O indicador de evolução do panorama, do ranking e dos grupos compara com o nível de referência da janela que começa em `hoje − 30 dias` e divide a variação pelo número de assuntos da matéria. O nível médio considera assuntos praticados. Na evolução individual e no histórico, erros estatísticos contam o primeiro erro por aluno, questão e dia. A lista de assuntos mais errados do panorama usa todas as tentativas incorretas na janela.

O primeiro envio válido atualiza a resposta e o diagnóstico do aluno no assunto da sessão na mesma transação. O bloqueio do aluno serializa envios concorrentes. A seleção da próxima questão usa o nível atualizado. A regra por assunto sobe um nível por acerto, até 10, e zera a sequência de erros. O primeiro erro consecutivo conserva o nível. O segundo reduz um nível quando ele está acima de 1. Os contadores e a proteção pertencem a cada aluno e assunto, inclusive entre sessões.

O feedback inclui `nivelAnterior` e `nivelAtual` gravados com a resposta. Um reenvio devolve os níveis daquele momento. O schema aceita respostas legadas com níveis históricos nulos.

**Decisões pendentes da equipe.** O PDF de requisitos estabelece a escala e a proteção do primeiro erro. O protótipo propõe exigir dois acertos para subir e penalizar respostas rápidas. As duas propostas continuam pendentes.

## Pontuação individual de aprendizagem

A dificuldade vigente da questão no banco define a base de 10 pontos por nível de dificuldade. Um acerto inédito concede 100% da base. Um acerto após a última resposta incorreta do mesmo aluno à questão concede 60%. Um acerto após a última resposta correta concede 30%. Uma resposta incorreta concede zero. Por exemplo, na dificuldade 5 os valores são 50, 30, 15 e zero, respectivamente. A classificação inclui tentativas de outras sessões e usa o histórico, inclusive quando um erro removeu a revisão agendada. O tempo de resposta fica fora da fórmula. [Fórmula do protótipo](https://github.com/i-barbosa/EduCria/blob/98abb3a1b6f90c62795a5590fdf76ef940efa681/index.html#L1156-L1160)

A migration V3 acrescenta `respostas_aluno.pontos_individuais` e a V4 cria o índice filtrado para os totais. A mesma transação que grava a resposta grava os pontos. Reenvios retornam o valor daquela tentativa. Os totais somam as respostas do titular por sessão, pelo assunto da sessão e pela matéria desse assunto. A pontuação coletiva possui seu próprio saldo. Editar a questão depois da resposta preserva os pontos já gravados e o assunto histórico.

As respostas anteriores à V3 conservam `NULL` e permanecem no histórico usado para classificar novas tentativas. Um erro novo grava zero. Os totais do catálogo começam em zero e só somam pontos registrados após a implantação. O histórico e o resultado exibem **Pontuação não registrada** nas sessões sem dados. A comparação só inclui pontos quando as duas sessões têm pontuação registrada em todas as suas respostas, conforme `pontuacaoCompleta`. O estado da atividade informa `pontosSessao`, e o feedback inclui `pontosConcedidos` quando há pontuação registrada.

## Revisões por questão

Cada linha de `repeticao_espacada` identifica exatamente uma atividade legada ou uma questão, com unicidade por aluno e alvo. O assunto vem da própria questão. A agenda por questão começa com as novas respostas. A sessão registra quantas revisões já apresentou para conservar a cota de três ao pausar e retomar.

O primeiro acerto agenda a questão para o dia seguinte. Em revisões posteriores, o acerto avança pelos intervalos de **1, 3, 7, 16, 35 e 70 dias** e permanece em 70 no teto. Um erro remove a revisão agendada da questão, quando houver. Um acerto posterior sem agenda reinicia o intervalo em um dia. Os dias são contados a partir da data da resposta em `America/Sao_Paulo`. Resposta, desempenho e agenda são gravados na mesma transação. Reenviar a resposta recupera o feedback e conserva o vencimento original.

O aluno autenticado acessa `GET /api/aluno/revisoes` ou **Minhas revisões** no painel. A resposta contém `hoje`, `proximaVirada`, `pendentes` (vencimento até hoje) e `futuras` (depois de hoje), em ordem de data e ID. Cada item traz questão, enunciado, assunto, intervalo em dias e vencimento. A agenda e as revisões no resultado da sessão consideram questões ativas e classificadas em atividades ativas. Desativar uma questão a oculta dessas listas e preserva a agenda gravada e as tentativas. Ao reativar a questão, a revisão volta à lista se estiver elegível. O navegador consulta a agenda novamente na virada do dia.

`SessaoEstudoIT` usa um relógio controlado para atravessar a meia-noite de 31 de dezembro em São Paulo e percorrer toda a sequência, inclusive teto, reinício em um dia, isolamento e reenvio.

## Resultado e histórico

As duas consultas usam o aluno identificado pelo JWT.

- `GET /api/aluno/sessoes/historico` lista as sessões da mais recente para a mais antiga. Cada página traz até 20 itens em `sessoes`. Quando houver `proximoCursor`, passe seu valor em `?antesDe=...` para continuar.
- `GET /api/aluno/sessoes/{id}/resultado` retorna `sessao`, `anterior`, `revisoes`, `questoes`, `pontosIndividuaisNoAssunto` e `pontosIndividuaisNaMateria`. Cada questão apresentada inclui seu número, enunciado, alternativa enviada e correção registrada. Uma questão sem resposta tem `alternativa` e `correta` nulas. A comparação usa a sessão encerrada anterior do mesmo titular e assunto. A primeira tem `anterior: null`. Consultar o ID de outro aluno retorna 404.

Acertos e erros contam somente respostas persistidas. O resultado de uma sessão ativa ou interrompida é parcial. O nível vem do registro da primeira e da última resposta na ordem de apresentação. Extremos sem registro ficam nulos. As questões respondidas com revisão na agenda atual exibem o vencimento vigente, que pode mudar após outra sessão. Os indicadores da sessão, da sessão anterior e das linhas do histórico incluem `pontosIndividuais`.

`SessaoEstudoIT` verifica histórico, resultados e isolamento.

## Próximos passos sugeridos

- CRUD de atividades/questões (admin)
- Scheduler para notificar revisões pendentes
- Testes unitários das regras de negócio
- Integração futura com RabbitMQ no ambiente de desenvolvimento
