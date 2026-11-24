# Apresentar o EDUCRIA no GitHub Codespaces

## Abrir a branch oficial de validação

Quando a equipe tiver publicado manualmente a branch oficial de validação, abra essa branch no repositório EDUCRIA e use **Code → Codespaces → Create codespace**. Confirme o nome da branch e o commit no terminal. Um Codespace recebe o conteúdo publicado na branch. [Criação de um Codespace no GitHub](https://docs.github.com/en/codespaces/developing-in-a-codespace/creating-a-codespace-for-a-repository)

```bash
git status --short --branch
git log -1 --oneline
docker compose version
```

O Dev Container configura Java 21, Maven, Node 22 e Docker-in-Docker. O Compose usa a imagem **SQL Server 2022 Developer**, do ramo 16.x. A demonstração Windows anteriormente validada usou **SQL Server Express 2025**, ramo 17.x. [Roteiro Windows](docs/sqlserver/APRESENTAR_WINDOWS.md)

## Subir a demonstração em um Codespace limpo

Para a apresentação, use o perfil `dev`. Em um Codespace limpo, o iniciador cria a base `educria` dentro do contêiner de desenvolvimento e o Flyway aplica V1 e V2. O callback acrescenta as 80 questões de Matemática.

```bash
SPRING_PROFILES_ACTIVE=dev bash scripts/iniciar.sh
```

O modo padrão usa o comando abaixo. Uma base nova recebe schema e catálogo, com zero questões antes de qualquer carga ou cadastro.

```bash
bash scripts/iniciar.sh
```

O iniciador prepara o arquivo privado `.env`, espera o banco ficar saudável, empacota o backend e inicia o Next.js. Mantenha esse terminal aberto e aguarde `Ready`. O Codespace só precisa encaminhar a porta **3000**. Mantenha a visibilidade **Private**. A API usa internamente 8080 e o SQL Server usa 1433 no loopback do Codespace.

## Conferir os fluxos

1. Abra a porta 3000 pela aba **Ports**. Registre um aluno sintético e teste login, matéria, sessão, resposta, histórico e a evolução de 30 dias.
2. Prepare um professor sintético com matéria e turma vinculadas para testar login, acompanhamento e recados. O callback `dev` cria as questões. O cadastro público cria apenas alunos. O provisionamento do professor na base `dev` é uma etapa da equipe.
3. Com o iniciador rodando, execute o verificador no segundo terminal. Ele usa a base de desenvolvimento, cria uma conta sintética e verifica o `Flyway`, autenticação e catálogo.
4. Registre branch, commit, versão do banco e resultados de cada fluxo em uma sessão limpa. O verificador automático cobre o cadastro e o catálogo. A sessão, a evolução e o fluxo do professor exigem passos adicionais.

```bash
docker compose ps
node scripts/verificar.mjs
```

## Separar as provas acadêmicas

Reserve a base `educria` deste contêiner para a demonstração da aplicação. Use **outra base nova, descartável e marcada** para as provas acadêmicas. Aplique somente V1 e V2 na segunda base, com configuração padrão. Depois execute a carga acadêmica, as verificações e as consultas antes de criar contas pelo site. O seed acadêmico depende de tabelas de usuários, turmas e atividades vazias.

O iniciador fixa a URL de desenvolvimento em `educria`. A segunda base precisa de uma execução independente de Flyway apontada explicitamente para ela. O script de apoio PowerShell foi preparado para Windows com autenticação integrada. A execução equivalente no contêiner Linux requer uma sessão local de `sqlcmd`, confirmação do nome e do marcador e fornecimento do BCrypt pelo procedimento acadêmico. O roteiro Windows é a referência de reprodução disponível para essa parte. [Roteiro da carga acadêmica e verificações](docs/sqlserver/README.md#execução-no-windows)

Para parar a aplicação, use **Ctrl+C** no terminal do iniciador. `docker compose stop db` para o contêiner e preserva os dados do volume `mssql_dados`. As credenciais do `.env` são exclusivas do desenvolvimento.

## Estado da validação

O roteiro, a configuração e os comandos foram conferidos nesta revisão. **A execução ponta a ponta em Codespace limpo está pendente.** Para encerrá-la, a equipe precisa disponibilizar manualmente a branch oficial com estes arquivos, usar um Codespace com Docker operacional e registrar aluno, professor, sessão, evolução e provas acadêmicas. [Matriz de validação](docs/sqlserver/REVISAO_2_DE_ENTREGA.md)
