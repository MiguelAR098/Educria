# Como ler o esquema SQL Server

O Flyway aplica as migrations no esquema `dbo` em ordem de versão. A tabela `flyway_schema_history` registra o que foi aplicado e os checksums que o Flyway usa para validar os arquivos. O Hibernate compara as entidades JPA com as tabelas no início da aplicação com `ddl-auto: validate`. Consulte este guia para estudar versões já aplicadas, preservando os arquivos SQL. [Configuração do backend](../../backend/src/main/resources/application.yml) · [Modelo físico e DER](MODELO.md)

## O que cada versão acrescenta

| Versão | O que observar |
|---|---|
| [V1 — Esquema](../../backend/src/main/resources/db/sqlserver/migration/V1__esquema_educria.sql) | Cria usuários, subtipos, turmas, catálogo, questões, sessões, respostas, revisões, desempenho, pontuação coletiva e recados. Define chaves e índices que protegem os vínculos e os limites dessas operações. |
| [V2 — Catálogo e níveis](../../backend/src/main/resources/db/sqlserver/migration/V2__catalogo_e_niveis.sql) | Insere Matemática, Português e Ciências, com oito assuntos iniciais em Matemática. O trigger `AFTER INSERT` lê a tabela lógica `inserted` e cria um desempenho por assunto para cada aluno recém-inserido. O `CROSS JOIN` funciona inclusive quando um único `INSERT` cadastra vários alunos. |
| [V3 — Pontuação individual](../../backend/src/main/resources/db/sqlserver/migration/V3__pontuacao_individual.sql) | Adiciona `pontos_individuais` às respostas. `NULL` sinaliza pontuação indisponível no legado. Quando o valor existe, o `CHECK` exige sessão, faixa de 0 a 100 e zero em caso de erro. O índice de tentativas recentes segue aluno, questão, data e ID para encontrar a resposta anterior. |
| [V4 — Índice de pontos da sessão](../../backend/src/main/resources/db/sqlserver/migration/V4__indice_pontuacao_por_sessao.sql) | Indexa aluno e sessão e inclui o valor dos pontos. O filtro seleciona as respostas pontuadas, inclusive as que receberam zero. A consulta pode somar os pontos da sessão usando as colunas do índice. |
| [V5 — Grupos pedagógicos](../../backend/src/main/resources/db/sqlserver/migration/V5__grupos_pedagogicos.sql) | Separa a iniciativa, as turmas do contexto e o histórico de participação. Cada participação guarda a turma de origem. Uma FK composta exige que essa turma pertença ao contexto. O índice único filtrado permite uma participação aberta por aluno e grupo e preserva as participações encerradas. |

Os scripts legados em `db/migration` pertencem à linha PostgreSQL. Compare-os com os testes antigos executados via `psql` em `backend/src/test/sql`. A configuração atual do SQL Server aponta para `db/sqlserver/migration`. [Migrations legadas](../../backend/src/main/resources/db/migration/) · [Testes SQL](../../backend/src/test/sql/)

## Chaves, restrições e relacionamentos

- A **chave primária** identifica uma linha. Em `turmas`, o SQL Server gera o `id` com `IDENTITY(1,1)`. Em `recados`, `IDENTITY(1000,10)` define início e incremento próprios. É possível haver lacunas na sequência, inclusive após uma transação revertida.
- A **chave estrangeira** exige uma linha correspondente. `alunos.usuario_id` é também a chave primária do subtipo. O mesmo ID identifica o usuário e seus dados específicos de aluno. `professor_turmas` usa duas chaves estrangeiras e a chave primária composta `(professor_id, turma_id)` para representar o vínculo N:N.
- **`UNIQUE`** impede valores ou combinações repetidas. Em `assuntos`, `(materia_id, nome)` permite o mesmo nome em matérias diferentes. **`CHECK`** limita valores, como a dificuldade de 1 a 10 quando preenchida. **`DEFAULT`** preenche os campos omitidos no `INSERT`, como o nível inicial de um aluno em um assunto.
- **`ON DELETE CASCADE`** remove linhas dependentes, como a pontuação coletiva de uma turma excluída. **`ON DELETE SET NULL`** preserva a linha e limpa o vínculo opcional, como a autoria de uma questão quando o autor é excluído. Nas relações da V5, as chaves estrangeiras impedem exclusões incompatíveis com o histórico de participação.

## Sessões, índice e concorrência

A tabela `sessao_questoes` registra até dez posições de questões distintas por sessão. O intervalo `0–9` delimita os índices e a combinação `(sessao_id, questao_id)` é única. A resposta referencia o par sessão/questão apresentada e o par sessão/aluno titular. A reaprendizagem permite várias respostas para a mesma questão em passos diferentes. O índice único filtrado sobre sessão e passo evita a segunda resposta no mesmo passo e mantém as respostas sem sessão fora dessa regra. O índice de sessões abertas aplica unicidade ao aluno considerando tanto sessões ativas quanto interrompidas. [Esquema da V1](../../backend/src/main/resources/db/sqlserver/migration/V1__esquema_educria.sql)

Um índice define como certas linhas são organizadas para busca. Os campos depois de `INCLUDE` ficam disponíveis para leitura no índice e não fazem parte da chave. `WHERE` restringe as linhas do índice. Por exemplo, `idx_respostas_erros` contém apenas respostas incorretas, com chave para localizar erros por aluno, questão e data. A escolha do índice depende do plano gerado pelo otimizador. O script de medição mostra como comparar planos com o mesmo predicado. [Medição de índice](../../scripts/sqlserver/academico/06-medicao-indice.sql)

A lógica do Java e as restrições do SQL Server trabalham juntas. O serviço de sessões usa transação para gravar resposta, progresso e revisão como uma operação e bloqueia o aluno para serializar alterações concorrentes. Chaves e índices únicos protegem a integridade mesmo quando o SQL é executado por outro caminho. [Serviço de sessões](../../backend/src/main/java/com/educria/service/SessaoEstudoService.java) · [Provas de integridade](../../scripts/sqlserver/academico/04-integridade.sql)

## Ler dados e preparar a demonstração

Na demonstração acadêmica, uma função escalar retorna a quantidade de questões distintas erradas por aluno e dia. A procedure de resumo usa `LEFT JOIN` para manter turmas sem atividades e devolve saldo coletivo, quantidade de atividades e soma dos pontos lançados. O catálogo `sys.*` alimenta o dicionário sem ler dados pessoais. As provas executáveis validam o efeito do trigger, a integridade dos vínculos e as agregações com dados sintéticos. [Função e procedure](../../scripts/sqlserver/academico/02-automacao.sql) · [Verificação](../../scripts/sqlserver/academico/03-verificacao.sql) · [Dicionário](../../scripts/sqlserver/academico/05-dicionario.sql)

O perfil `dev` executa um callback Flyway separado que carrega 160 questões de Matemática. A carga local de contas reutiliza o catálogo em outra etapa, exige uma base com nome de demonstração e marcador de descarte e serializa a operação com `sp_getapplock`. A carga acadêmica prepara os próprios dados sintéticos em uma base descartável recém-migrada. Os dois roteiros têm finalidades e pré-requisitos diferentes. [Callback de desenvolvimento](../../backend/src/main/resources/db/sqlserver/demo/afterMigrate__questoes_de_matematica.sql) · [Roteiros SQL Server](README.md)
