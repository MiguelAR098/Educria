# Guia de leitura do backend EDUCRIA

O backend em Java 21 recebe requisições HTTP, aplica as regras pedagógicas e registra os resultados no SQL Server. O Maven compila o projeto e executa os testes. O Flyway aplica as versões do esquema. As consultas usam Spring Data JPA, Hibernate e JDBC conforme o tipo de operação.

## Onde cada responsabilidade fica

Os caminhos abaixo partem de `backend/src/main/java/com/educria/`.

| Pasta | Responsabilidade | Exemplo |
|---|---|---|
| `controller` | Receber parâmetros e JSON, identificar o usuário autenticado e devolver status e corpo HTTP. | `controller/RespostaSessaoController.java` |
| `service` | Aplicar regras pedagógicas, verificar o acesso ao recurso e controlar as transações. | `service/SessaoEstudoService.java` |
| `repository` | Ler e gravar no SQL Server. JPQL consulta entidades e relacionamentos. SQL nativo usa recursos do banco, como funções de janela e bloqueios. | `repository/AlunoRepository.java`, `repository/EvolucaoAprendizagemRepository.java` |
| `domain` | Representar o estado do sistema e operações que o alteram. | `domain/entity/SessaoEstudo.java`, `domain/NivelAdaptativo.java` |
| `dto` | Definir os campos aceitos e devolvidos pela API, com validações de entrada quando aplicáveis. | `dto/EnviarResposta.java`, `dto/QuestaoParaAluno.java` |
| `config`, `security`, `exception` | Configurar a autenticação e traduzir erros em respostas HTTP. | `config/SecurityConfig.java`, `exception/GlobalExceptionHandler.java` |

As migrations estão em [backend/src/main/resources/db/sqlserver/migration](../backend/src/main/resources/db/sqlserver/migration). O [guia do esquema SQL Server](sqlserver/ESQUEMA-E-MIGRACOES.md) explica chaves, restrições, relacionamentos, índices e as versões V1 a V5.

## POO na prática

- **Classes e objetos.** [SessaoEstudo.java](../backend/src/main/java/com/educria/domain/entity/SessaoEstudo.java) define o estado e as operações de uma sessão. Cada sessão aberta é uma instância com o próprio assunto, titular e passo.
- **Abstração, herança e polimorfismo.** [Usuario.java](../backend/src/main/java/com/educria/domain/entity/Usuario.java) reúne a identidade comum. `Aluno`, `Professor` e `Admin` especializam essa classe. O login trabalha com o tipo `Usuario`, e o Hibernate pode devolver qualquer um dos três subtipos. Com `JOINED`, a tabela de cada subtipo usa o mesmo identificador da linha de `usuarios`.
- **Encapsulamento e composição.** A sessão concentra as transições de estado em métodos do domínio. O aluno vincula seu diagnóstico em `Aluno.vincularDesempenho`, que mantém os dois lados da associação. O JPA aplica a cascata e a remoção de órfãos configuradas para essa associação.
- **Interfaces e injeção de dependências.** O serviço declara a interface do repositório como dependência. O Spring Data cria sua implementação e o Spring a fornece pelo construtor. O relógio injetado pela aplicação também pode ser substituído por um relógio fixo nos testes.
- **Anotações.** `@Entity` e `@Table` ligam uma classe ao mapeamento do banco. `@Id` indica a chave primária e `IDENTITY` delega a geração ao SQL Server. `@JoinColumn` identifica uma coluna de vínculo. `@Version` permite ao Hibernate detectar atualizações concorrentes de um diagnóstico, enquanto o bloqueio do titular serializa o fluxo de escrita da sessão.

## Da requisição à resposta

1. [SecurityConfig.java](../backend/src/main/java/com/educria/config/SecurityConfig.java) define o perfil exigido por rota. No login, [AuthService.java](../backend/src/main/java/com/educria/service/AuthService.java) delega a verificação ao Spring Security com BCrypt. [JwtService.java](../backend/src/main/java/com/educria/security/JwtService.java) assina o token após a autenticação.
2. Em chamadas protegidas, [JwtAuthFilter.java](../backend/src/main/java/com/educria/security/JwtAuthFilter.java) verifica o Bearer e recarrega a conta ativa e suas autoridades. O controller recebe essa identidade por `Principal` ou `@AuthenticationPrincipal`.
3. O controller converte a entrada e usa `@Valid` quando exigido pela rota. O serviço verifica o acesso a cada recurso pelo titular ou pelo vínculo do professor. Um ID recebido indica o recurso pedido.
4. O repositório executa a operação dentro da transação. O retorno usa DTOs específicos para a tela. Por exemplo, a questão apresentada ao estudante traz as alternativas, e a correção é tratada pelo fluxo de resposta.

## Sessão de estudo e correção

Comece em [SessaoEstudoController.java](../backend/src/main/java/com/educria/controller/SessaoEstudoController.java) e siga para [SessaoEstudoService.java](../backend/src/main/java/com/educria/service/SessaoEstudoService.java). O serviço recupera uma sessão aberta ou inicia outra com o assunto escolhido ou recomendado. [SelecaoAdaptativaService.java](../backend/src/main/java/com/educria/service/SelecaoAdaptativaService.java) prioriza revisões pendentes, respeita a cota de três e procura a dificuldade mais próxima do nível atual. A sessão guarda até dez questões distintas. Uma reapresentação ocupa um novo passo e pode ocorrer quando faltam questões novas. A pausa preserva o estado. O encerramento exige resolver as pendências de reaprendizagem.

[RespostaSessaoController.java](../backend/src/main/java/com/educria/controller/RespostaSessaoController.java) encaminha questão, passo e alternativa. O serviço bloqueia o titular, verifica a sessão e corrige pelo gabarito. Reenviar a mesma tentativa recupera o feedback gravado. Uma alternativa diferente no mesmo passo gera conflito. A mesma transação grava tentativa, pontos, diagnóstico e agenda conforme o resultado.

O diagnóstico por assunto vai de 1 a 10. O acerto sobe um nível até o teto. O primeiro erro consecutivo preserva o nível e o segundo pode rebaixá-lo. Tentativas de reaprendizagem registram a resposta e podem conceder pontos, com o diagnóstico preservado. A base dos pontos é dificuldade × 10. O acerto inédito vale 100%, o acerto após erro vale 60%, o acerto após acerto anterior vale 30% e o erro vale zero. O saldo coletivo pertence à turma e tem fluxo próprio.

[AgendaRevisoesService.java](../backend/src/main/java/com/educria/service/AgendaRevisoesService.java) usa a data local de estudo. O primeiro acerto sem agenda inicia em D1. Acertos posteriores avançam por 1, 3, 7, 16, 35 e 70 dias. O erro remove a revisão agendada daquela questão. Um acerto posterior sem agenda volta a D1.

## Ordem sugerida para estudar e apresentar

1. **Entrada e segurança.** Leia `EducriaApplication.java`, `controller/AuthController.java`, `service/AuthService.java` e `security/JwtAuthFilter.java`. Na apresentação, mostre como o e-mail autenticado chega ao controller.
2. **Modelo.** Leia `domain/entity/Usuario.java` e seus três subtipos. Compare com o esquema do SQL Server e explique a chave compartilhada.
3. **Aprendizagem.** Leia `service/SessaoEstudoService.java`, `domain/entity/SessaoEstudo.java`, `domain/NivelAdaptativo.java` e `service/PontuacaoIndividual.java`. Mostre a transação de correção e o reenvio do mesmo passo.
4. **Histórico.** Leia `service/HistoricoSessaoService.java` e `repository/EvolucaoAprendizagemRepository.java`. A comparação do resultado usa a sessão encerrada anterior do mesmo assunto. A evolução individual considera hoje e os 29 dias anteriores. Respostas legadas podem contar nas tentativas com nível ou pontuação ausentes.
5. **Professor.** Leia `service/ProfessorService.java`, `service/PanoramaDaTurmaService.java`, `service/BancoDeQuestoesService.java` e `service/IndicadoresDoGrupoService.java`. Mostre como turma e matéria delimitam o acesso, como a cobertura conta questões elegíveis e como os indicadores de grupos agregam os recortes de participantes e demais estudantes.
6. **Testes.** Em `backend/src/test/java/com/educria/`, compare `service/PontuacaoIndividualTest.java` com `service/SessaoEstudoIT.java`. O primeiro verifica a fórmula em memória. O segundo exercita HTTP simulado, autorização, concorrência e persistência em SQL Server descartável.
