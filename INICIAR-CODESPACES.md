# Apresentar o EDUCRIA no GitHub Codespaces

## Abrir a branch oficial de validação

Quando a equipe tiver publicado manualmente a branch oficial de validação, abra essa branch no repositório EDUCRIA e use **Code → Codespaces → Create codespace**. Confirme o nome da branch e o commit no terminal. Um Codespace recebe o conteúdo publicado na branch. [Criação de um Codespace no GitHub](https://docs.github.com/en/codespaces/developing-in-a-codespace/creating-a-codespace-for-a-repository)

```bash
git status --short --branch
git log -1 --oneline
docker compose version
```

O Dev Container configura Java 21, Maven, Node 22 e Docker-in-Docker. O Compose usa a imagem **SQL Server 2022 Developer**, do ramo 16.x. Para a apresentação atual focada no backend, use o roteiro com uma base descartável e chamadas diretas à API. [Roteiro do backend](docs/sqlserver/README.md) · [Roteiro Windows](docs/sqlserver/APRESENTAR_WINDOWS.md)

## Subir a demonstração em um Codespace limpo

Para a apresentação, inicie o backend no perfil `dev` com uma base `educria_demo_` descartável e marcada. O Flyway aplica as migrations de V1 a V5 e o callback carrega as 160 questões de Matemática. [Configuração da base e do backend](docs/sqlserver/README.md#codespaces)

Para o desenvolvimento local com a interface, o iniciador abaixo usa o banco `educria`. Uma base nova no perfil padrão recebe schema e catálogo, com zero questões antes de qualquer carga ou cadastro.

```bash
bash scripts/iniciar.sh
```

O iniciador prepara o arquivo privado `.env`, espera o banco ficar saudável, empacota o backend e inicia o Next.js. Mantenha esse terminal aberto e aguarde `Ready`. O Codespace só precisa encaminhar a porta **3000**. Mantenha a visibilidade **Private**. A API usa internamente 8080 e o SQL Server usa 1433 no loopback do Codespace.

## Carregar as contas de demonstração

Com a aplicação rodando e o catálogo de Matemática disponível, execute na raiz do projeto, em outro terminal. A carga inicial usa oito questões ativas de Matemática com dificuldade cadastrada para as revisões de Carla e três questões ativas de frações com uma alternativa incorreta preenchida para os erros de Ana. O Flyway preenche esse catálogo ao iniciar uma base nova com o perfil `dev`. As contas ficam disponíveis para login assim que a carga termina, sem reiniciar o backend.

```bash
bash scripts/sqlserver/carregar-demo.sh --desenvolvimento --banco educria_demo_apresentacao
```

A carga exige o nome explícito da base `educria_demo_<sufixo>` do serviço `db` no Compose local e a propriedade estendida `EducriaAmbiente` com valor `descartavel`. Os dois professores e os quatro alunos ficam na turma `8º A` de 2026, reservada pelo código `DEMO-8A`. Repetir o comando preserva o progresso já registrado. Se algum e-mail, matrícula ou vínculo conflitar com os dados esperados, a transação é revertida e o comando informa o conflito.

| E-mail | Perfil ou cenário inicial |
|---|---|
| `professor@educria.com` | Professora de Matemática |
| `portugues@educria.com` | Professor de Português, com acesso ao cadastro de assuntos |
| `ana.demo@educria.com` | Em risco, com dificuldades em frações |
| `bruno.demo@educria.com` | Bom desempenho |
| `carla.demo@educria.com` | Em atenção, com oito revisões vencidas |
| `diego.demo@educria.com` | Sem atividade |

Use as senhas configuradas para essas contas na sua instância descartável ao executar o roteiro HTTP.

O lançador Bash usa o socket Unix do Docker e o SQL Server Developer do Compose. O lançador PowerShell nativo usa os mesmos scripts SQL e autenticação integrada na instância local. [Roteiro Windows](docs/sqlserver/APRESENTAR_WINDOWS.md)

## Conferir os fluxos

1. Confirme a conexão do backend com a base descartável de demonstração.
2. Execute as chamadas diretas de login, sessão, resposta, histórico, ranking e grupos. [Roteiro HTTP do backend](backend/README.md#apresentação-pelo-backend)
3. Registre branch, commit, versão do banco e resultados de cada fluxo em uma sessão limpa.

## Separar as provas acadêmicas

Reserve a base `educria` deste contêiner para o uso normal de desenvolvimento. Use **bases novas, descartáveis e marcadas** para a demonstração e as provas acadêmicas. Aplique V1 a V5 no perfil padrão na base acadêmica. Depois execute a carga acadêmica, as verificações e as consultas antes de criar contas pela API. O seed acadêmico depende de tabelas de usuários, turmas e atividades vazias.

O iniciador fixa a URL de desenvolvimento em `educria`. Inicie apenas o backend com a conexão explícita para rodar o Flyway nas bases descartáveis. Use os lançadores acadêmicos de Bash ou PowerShell para as provas. [Roteiro da carga acadêmica e verificações](docs/sqlserver/README.md#provas-acadêmicas)

Para parar a aplicação, use **Ctrl+C** no terminal do iniciador. `docker compose stop db` para o contêiner e preserva os dados do volume `mssql_dados`. As credenciais do `.env` são exclusivas do desenvolvimento.

## Estado da validação

No Codespaces com SQL Server 2022 Developer, foram verificadas a instalação limpa, a atualização de V4 para V5 com dados, a carga Bash repetida e a persistência de sessões e grupos após reiniciar a API. As provas acadêmicas confirmaram 22 tabelas do domínio, 126 atributos e pelo menos três registros em cada tabela. A instalação nativa no Windows requer validação na máquina da equipe. [Validação do backend](docs/sqlserver/README.md)
