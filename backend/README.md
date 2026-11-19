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
- `proximasRevisoes` associa cada questão já vista à data da próxima revisão. Questões ausentes do mapa são tratadas como novas. A tabela de repetição existente agenda atividades inteiras, por isso essa seleção recebe explicitamente o calendário por questão.
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

A V5 persiste a sessão e a sequência de IDs apresentados. Cada aluno tem até uma sessão aberta (ativa ou interrompida). Repetir o início para o mesmo assunto ou por recomendação devolve a sessão aberta com 200. Mudar de assunto exige encerrar a sessão anterior. Esgotamento, posição fora de sequência e operações incompatíveis com o estado retornam 409. O contador indica questões apresentadas. O avanço consulta o nível atual no servidor e não grava respostas, pontos ou acertos. As revisões por questão continuam sem calendário persistido nesta etapa.

Para realizar testes com questões de demonstração, inicie com `SPRING_PROFILES_ACTIVE=dev bash scripts/iniciar.sh`. Em uma base PostgreSQL de teste com as variáveis do backend configuradas, execute `mvn -f backend/pom.xml -Dtest=SessaoEstudoIT test` para verificar autenticação, isolamento, concorrência, limite, interrupção e retomada.

## Enviar uma resposta

Use `PUT /api/aluno/sessoes/{id}/questoes/{questaoId}/resposta` com o JWT do aluno e um corpo como `{"alternativa":"B"}`. Use o ID de `questaoAtual` retornado pela sessão. O primeiro envio exige a questão atual de uma sessão ativa. O retorno HTTP 200 contém a alternativa enviada, o booleano `correta` e a `explicacao` cadastrada. A consulta da sessão inclui o campo opcional `feedback` somente quando o aluno já respondeu à questão atual naquela sessão.

Reenviar a mesma letra recupera a correção gravada, inclusive após avançar ou encerrar. Tentar trocá-la retorna 409. Letras fora de A–D e opções indisponíveis retornam 400. Uma sessão de outro aluno ou uma questão que não foi apresentada nela retorna 404. A V6 mantém as respostas anteriores e garante uma resposta por questão em cada sessão.

Na raiz do projeto, com PostgreSQL de teste preparado, rode `mvn -f backend/pom.xml -Dtest=SessaoEstudoIT test`. A suíte cobre envios simultâneos, isolamento por aluno, validação e recuperação do feedback.

## Próximos passos sugeridos

- CRUD de atividades/questões (admin)
- Scheduler para notificar revisões pendentes
- Testes unitários das regras de negócio
- Integração futura com RabbitMQ no ambiente de desenvolvimento
