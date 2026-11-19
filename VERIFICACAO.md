# EDUCRIA · Desempenho por assunto

O modelo já está integrado a este projeto. Para iniciar os serviços e fazer os testes pela porta 3000, siga [INICIAR-CODESPACES.md](INICIAR-CODESPACES.md). Os comandos Maven e PostgreSQL abaixo partem da pasta `backend/`.

## Decisões para validação da equipe

| Tema | Evidência nos anexos | Premissa adotada |
|---|---|---|
| Catálogo | No protótipo, Matemática tem oito assuntos. As outras quatro matérias aparecem em diferentes estágios de desenvolvimento. | A V2 cadastra Matemática e os oito assuntos com a grafia de `CONCEITOS`. A equipe pode expandir o catálogo por migração posterior. |
| Escala e progressão | O protótipo usa 1–5 e dois acertos para subir. O PDF define 1–10 e protege o primeiro erro. O Spring Boot sobe um nível por acerto. | Cada assunto usa 1–10. Um acerto zera a sequência de erros e sobe até o teto. Dois erros consecutivos rebaixam um nível quando há espaço para descer. No piso, os erros continuam sendo contados. |
| Distribuição do histórico | A V1 identifica o aluno nas respostas antigas, mas a questão carece de vínculo com assunto. | Cada par aluno–assunto começa em 1, com zero respostas. Esse estado indica o ponto de partida. Atribuir respostas antigas exige classificação pedagógica das questões. |
| Indicador do dashboard | O protótipo calcula a média dos assuntos. O dashboard Next.js lê o diagnóstico global 1:1. | O indicador global conserva sua fonte e seu significado. Uma futura média exige decidir como tratar assuntos ainda sem respostas. |

## Modelo e migração

Uma matéria pode ter vários assuntos. A combinação de aluno e assunto é única em `desempenho_por_assunto`. Cada registro guarda nível, sequência de erros, totais de acertos e erros e uma versão para o controle de atualizações concorrentes pelo Hibernate. O diagnóstico individual fica separado dos pontos da turma.

A V2 cria o catálogo, instala um gatilho de cadastro e inicializa os pares dos alunos existentes em uma única transação PostgreSQL. O gatilho cobre cadastros feitos durante a convivência entre versões da aplicação. A carga cria oito linhas por aluno e a instalação do gatilho pode fazer cadastros aguardarem o fim da transação. Para uma implantação, estime o volume e o tempo em uma cópia da base. Ao incluir um assunto no futuro, a migração correspondente também deve inicializá-lo para os alunos já cadastrados.

O repositório oferece `findByAlunoIdAndAssuntoId`. O método `registrarResposta` altera apenas a entidade em que é chamado. O fluxo atual do ZIP continua atualizando o global quando conhece apenas aluno e acerto. A associação de questões a assuntos e a entrega de respostas pelo motor de sessões pertencem à próxima etapa.

## 1. Executar os testes unitários

Com JDK 21 e Maven 3.9+, rode:

```sh
mvn clean test
```

A classe `DesempenhoPorAssuntoTest` cobre piso, teto, rejeição de 0 e 11, primeiro e segundo erros, interrupção de sequência, independência entre assuntos e indicadores globais de um aluno com histórico. O comando recompila as fontes do pacote.

## 2. Testar a migração com um aluno legado e um cadastro novo

Em um PostgreSQL 15+ de teste, use uma base em que seu usuário possa criar esquemas. Substitua o nome da base conforme sua configuração.

```sh
psql -X -U educria -d educria_teste -f src/test/sql/verificar_v2.sql
```

O script abre uma transação, cria um esquema isolado, aplica o SQL original da V1, cadastra um aluno com nível global 7 e executa a V2. Em seguida, cadastra outro aluno e verifica os 16 registros iniciais, a preservação integral do registro global, a unicidade do par, os limites do banco e o isolamento entre alunos e assuntos. O resultado esperado é `Verificação da V2 concluída.` A transação termina em `ROLLBACK`.

## 3. Verificar Flyway e dashboard pela aplicação

1. Em uma cópia de teste da base que já esteja na V1, com a aplicação original, faça login como um aluno com histórico. Abra `/aluno` e salve o JSON de `GET /api/aluno/desempenho` na aba Rede do navegador. Pare o backend.
2. Use esta versão e aponte o backend para essa mesma cópia via `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`. Carregue também as variáveis de desenvolvimento do arquivo `.env` da raiz antes de executar `mvn clean spring-boot:run`. A aplicação deve aplicar a V2 e concluir a validação do Hibernate. No SQL, confira o histórico abaixo. As versões 1 e 2 devem aparecer com sucesso. Reiniciar a aplicação deve preservar a contagem de registros por aluno.
3. Com o frontend consolidado em execução, entre como o mesmo aluno e recarregue `/aluno`. Confira HTTP 200 e os mesmos cinco campos e valores na resposta. O cartão deve mostrar o nível global e os totais anteriores. Confira a resposta HTTP diretamente, pois o cartão também exibe valores padrão em algumas situações.
4. Cadastre um novo aluno pelo formulário e consulte a rota com esse login. Espere `{"nivelAtual":1,"errosConsecutivos":0,"totalAcertos":0,"totalErros":0,"nivelMaximo":false}`. Na segunda consulta SQL abaixo, use o e-mail desse aluno. Devem aparecer os oito assuntos, todos com nível 1 e contadores zerados.
5. Para testar uma instalação nova, repita a inicialização em uma base vazia de teste. O histórico deve conter V1 e V2, e o primeiro cadastro deve produzir o mesmo resultado do passo 4.

```sql
SELECT version, description, success
FROM flyway_schema_history
WHERE version IN ('1', '2')
ORDER BY installed_rank;

SELECT m.nome AS materia, s.nome AS assunto, d.nivel_atual,
       d.erros_consecutivos, d.total_acertos, d.total_erros
FROM desempenho_por_assunto d
JOIN assuntos s ON s.id = d.assunto_id
JOIN materias m ON m.id = s.materia_id
JOIN usuarios u ON u.id = d.aluno_id
WHERE u.email = 'aluno-de-teste@example.test'
ORDER BY s.id;
```

## Arquivos do pacote

| Caminho em `backend/src/` | Alteração |
|---|---|
| `main/resources/db/migration/V2__desempenho_por_assunto.sql` | Nova migração Flyway. |
| `main/java/com/educria/domain/entity/Materia.java` | Entidade de matéria. |
| `main/java/com/educria/domain/entity/Assunto.java` | Entidade de assunto vinculado a uma matéria. |
| `main/java/com/educria/domain/entity/DesempenhoPorAssunto.java` | Estado privado do aluno em um assunto. |
| `main/java/com/educria/domain/NivelAdaptativo.java` | Regra de progressão compartilhada. |
| `main/java/com/educria/domain/entity/DesempenhoAdaptativo.java` | Entidade global existente passa a usar a regra compartilhada. |
| `main/java/com/educria/repository/DesempenhoPorAssuntoRepository.java` | Acesso ao registro de um aluno em um assunto. |
| `test/java/com/educria/domain/entity/DesempenhoPorAssuntoTest.java` | Testes JUnit de limites e regressão. |
| `test/sql/verificar_v2.sql` | Verificação transacional da migração. |
