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

## Próximos passos sugeridos

- Implementar fluxo completo de resposta de questões
- CRUD de atividades/questões (admin)
- Scheduler para notificar revisões pendentes
- Testes unitários das regras de negócio
- Integração futura com RabbitMQ no ambiente de desenvolvimento
