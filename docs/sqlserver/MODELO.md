# Modelo lógico e físico do SQL Server

O schema de domínio da V5 contém **22 tabelas e 126 atributos**. A tabela `flyway_schema_history` é de controle e fica fora dessa contagem. O DER fonte lista todas as colunas e destaca vínculos e chaves. O dicionário executável consulta `sys.tables`, `sys.columns`, `sys.foreign_keys`, `sys.check_constraints` e `sys.indexes` para obter as definições físicas da instância. [DER em Mermaid](DER.mmd) · [Dicionário SQL](../../scripts/sqlserver/academico/05-dicionario.sql)

| Tabela | Atributos | Chaves e domínio |
|---|---:|---|
| `usuarios` | 8 | PK `id`, e-mail único e perfil ALUNO, PROFESSOR ou ADMIN. `senha` contém o BCrypt e fica no banco. |
| `alunos` | 3 | PK e FK `usuario_id`. Turma atual opcional. |
| `professores` | 2 | PK e FK `usuario_id`. Referência obrigatória à matéria. |
| `admins` | 1 | PK e FK `usuario_id`. |
| `turmas` | 5 | PK `id`. |
| `professor_turmas` | 2 | PK composta `(professor_id,turma_id)` e FKs dos dois lados do N:N. |
| `materias` | 2 | PK `id` e nome único. |
| `assuntos` | 3 | PK `id`, FK à matéria e nome único por matéria. |
| `desempenho_adaptativo` | 7 | PK `id`, FK única ao aluno e nível 1–10. |
| `desempenho_por_assunto` | 8 | PK `id` e par aluno/assunto único. Nível 1–10, contadores não negativos e versão de atualização. |
| `pontuacao_coletiva` | 4 | PK `id` e FK única à turma. |
| `atividades` | 6 | PK `id`. A atividade de demonstração é reservada no perfil `dev`. |
| `questoes` | 14 | PK `id` e FKs à atividade, ao assunto e ao autor. Dificuldade 1–10 quando preenchida. |
| `sessoes_estudo` | 9 | PK `id` e FKs ao aluno e ao assunto. Estado ATIVA, INTERROMPIDA ou ENCERRADA e no máximo uma sessão aberta por aluno. |
| `sessao_questoes` | 3 | PK `(sessao_id,indice)` e par sessão/questão único. Índices de questão 0–9. |
| `respostas_aluno` | 12 | PK `id` e FKs compostas para titular e questão apresentada. Pontos individuais 0–100 nas respostas de sessão, `NULL` no legado. |
| `repeticao_espacada` | 8 | PK `id`, alvo exclusivo entre atividade legada e questão e intervalos D1, D3, D7, D16, D35 e D70. |
| `atividades_coletivas` | 8 | PK `id` e FKs à turma e ao professor. Pontos 20, 40, 60 ou 80. |
| `recados` | 6 | PK `id` com `IDENTITY(1000,10)` e FKs a aluno e professor. |
| `grupos_pedagogicos` | 7 | PK `id` e FK ao professor responsável. Tipo GRUPO ou PROJETO, nome obrigatório e status derivado de `arquivado_em`. |
| `grupo_turmas` | 2 | PK `(grupo_id,turma_id)` e FKs aos dois lados do N:N. |
| `grupo_participacoes` | 6 | PK `id`, FK ao aluno e FK composta `(grupo_id,turma_origem_id)` ao contexto. O índice filtrado permite apenas uma entrada aberta por aluno e grupo. |

## Integridade e consultas

- A herança Java de usuários usa entidades com tabelas por subtipo e PK compartilhada. As FKs protegem a existência do usuário. O serviço de cadastro mantém o perfil coerente com o subtipo.
- O contexto de grupo registra uma ou mais turmas na criação. O banco garante que a turma de origem da participação esteja nesse contexto. A API valida que o professor ainda leciona nas turmas cadastradas e que o aluno ainda pertence ao contexto antes de exibir seus dados. A saída fecha uma participação. Uma reentrada gera outra linha, com nova turma de origem quando aplicável. Arquivar marca o grupo com data. As tabelas da V5 usam `NO ACTION` nas FKs e preservam o histórico relacional.
- As cascatas anteriores pertencem ao modelo existente. Por exemplo, a prova acadêmica demonstra a remoção da pontuação de uma turma temporária e as proteções de sessão e de resposta. A FK composta da questão retomada exige que a questão já tenha sido apresentada. Índices filtrados garantem uma sessão aberta por aluno, uma resposta por passo e a unicidade dos alvos de revisão.
- Os índices da V3 e da V4 atendem busca de tentativa recente e agregação de pontos por aluno e sessão. A V5 acrescenta `idx_grupos_professor`, `idx_grupo_turmas_turma`, `idx_grupo_participacoes_aluno` e `ux_grupo_participacao_ativa`. O dicionário SQL mostra ordem, colunas incluídas e filtros dos índices. A presença dos índices é observável. A velocidade depende do volume, da distribuição dos dados e dos planos escolhidos.
- O trigger da V2 inicializa o nível para cada assunto quando há cadastro de aluno. A função acadêmica calcula erros estatísticos por dia. A procedure acadêmica retorna o resumo de atividades coletivas. A carga acadêmica demonstra esses objetos em uma base descartável. O Flyway mantém os checksums do schema.

## Artefatos de apresentação

Use o DER, a saída do dicionário e as consultas sintéticas para revisar os documentos acadêmicos em equipe. O checkout contém as fontes textuais. Word e PowerPoint não foram editados e podem precisar de revisão para refletir as três tabelas e os quinze atributos adicionais da V5. [Roteiro e scripts acadêmicos](README.md#provas-acadêmicas)
