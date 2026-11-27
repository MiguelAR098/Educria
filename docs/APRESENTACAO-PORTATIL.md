# Apresentação portátil do EDUCRIA

## Preparar e transportar

Use um Windows x64 autorizado, com Docker Desktop no modo de contêineres Linux. Abra o Docker Desktop e aguarde o engine. O pacote usa o plug-in `docker compose` com suporte a `up --wait-timeout`, disponível em versões compatíveis do Compose v2 e no Compose v5. O Engine 28.0 ou mais recente garante o isolamento de portas publicadas no loopback. Confirme que o Docker Desktop inicia com a conta Windows que fará a apresentação. [Versões do Docker Compose](https://docs.docker.com/compose/intro/history/) · [Publicação de portas Docker](https://docs.docker.com/engine/network/port-publishing/)

### Máquina de preparação com internet

Use uma pasta local em disco NTFS, fora de locais sincronizados ou compartilhados. Na raiz do pacote, execute os comandos abaixo. Crie a pasta `D:\transporte` ou indique outra pasta existente para receber as imagens.

```powershell
.\preparar-apresentacao.cmd -Modo Construir
.\apresentar.cmd -NaoAbrir
.\verificar-apresentacao.cmd -Reiniciar
.\parar-apresentacao.cmd
.\preparar-apresentacao.cmd -Modo Exportar -Arquivo 'D:\transporte\educria-imagens.tar'
```

A preparação usa Java 21 e Maven dentro da imagem de compilação da API, Node 22 com `npm ci` e build de produção para o Next, e SQL Server 2022 Developer para o contêiner de dados. A exportação mostra os tamanhos das imagens e do TAR e gera `educria-imagens.tar.json` com hashes e IDs. O total depende das imagens construídas. Reserve espaço tanto no disco usado pelo Docker Desktop quanto no destino do TAR. Se o disco virtual foi movido, indique a pasta do Docker Desktop em `-PastaDiscoDocker` na preparação. Use um meio de transporte que aceite arquivos acima de 4 GiB, como NTFS ou exFAT. [Imagem SQL Server 2022](https://mcr.microsoft.com/artifact/mar/mssql/server/tags) · [Exportação de imagens Docker](https://docs.docker.com/reference/cli/docker/image/save/)

Leve o código desta mesma versão, o TAR e o manifesto JSON gerado ao lado dele. O ZIP de código e o TAR de imagens viajam separados. Mantenha fora do transporte `.env`, `.educria-apresentacao/`, tokens, senhas, volumes, backups, `node_modules`, `.next`, `target` e materiais confidenciais. O TAR é gerado a partir das imagens, sem os segredos locais ou o conteúdo do volume.

### Segundo computador, inclusive sem internet

Extraia o código para a pasta local definitiva. Mantenha o TAR e o manifesto juntos. Com Docker Desktop aberto, importe e faça o ensaio.

```powershell
.\preparar-apresentacao.cmd -Modo Importar -Arquivo 'D:\transporte\educria-imagens.tar'
.\apresentar.cmd
.\verificar-apresentacao.cmd -Reiniciar
```

O importador verifica o TAR, o manifesto e as três imagens Linux x64. O segundo PC cria segredos próprios para SQL Server, JWT e contas fictícias e guarda os arquivos com acesso restrito à conta Windows da preparação e ao sistema. Para consultar a senha fictícia local, abra `.educria-apresentacao\demo-senha` em um editor local com o compartilhamento de tela desligado. Use-a com `aluno@demo.educria.test`, `professor@demo.educria.test` ou `admin@demo.educria.test`. Preserve a pasta de configuração junto ao projeto enquanto o volume existir.

## Apresentar

Com Docker Desktop aberto e o computador já preparado, execute na pasta do pacote:

```powershell
.\apresentar.cmd
```

O comando inicia as imagens locais com `--no-build --pull never` e aguarda as verificações do banco, da API e do frontend. Ele abre `http://localhost:3000` somente após confirmar a tela de login e a resposta de uma rota protegida. Java, Maven, Node e ferramentas de teste são dispensáveis no host da apresentação.

O projeto `educria-apresentacao` publica apenas o Next em `127.0.0.1:3000`. A API em `api:8080` e o SQL Server em `db:1433` atendem pela rede interna do projeto. Se a porta 3000 estiver ocupada, identifique o programa responsável e use outro computador autorizado se o conflito continuar. Os fluxos existentes do Codespaces e do Windows manual continuam usando seus próprios processos e configuração.

No ensaio presencial, conecte a TV ao computador que executa o pacote e projete a janela do navegador. Confirme resolução e ampliação legíveis, login nos três perfis, turma do professor e do ADM e sessão de Matemática do aluno. Com o PC já preparado, repita a inicialização com a rede desligada. Se Docker Desktop não estiver disponível ou não iniciar, use um vídeo ou capturas previamente gravadas com os dados fictícios, omitindo a digitação de credenciais.

## Dados e diagnóstico

Na primeira inicialização, o contêiner cria `educria_demo_apresentacao` dentro do volume exclusivo e aplica o marcador `EducriaAmbiente=descartavel`. Se a base já existir, o marcador tem de estar presente. Em seguida, Flyway aplica as migrations SQL Server V1–V5 e o callback carrega as 160 questões. A carga sintética usa a autenticação da aplicação e cria uma turma, dois alunos, um professor, um ADM, histórico em Matemática e uma sessão que o aluno pode retomar.

A carga usa uma transação e um marcador de conclusão. As execuções posteriores preservam credenciais das contas, edições e atividade do ensaio. Para verificar a instalação, `verificar-apresentacao.cmd` consulta contagens, testa os três logins e os bloqueios por perfil, e lê o estado de uma sessão. A opção `-Reiniciar` para e sobe apenas este projeto, compara as assinaturas dos registros antes e depois e repete os testes HTTP. Faça essa comparação sem alunos ou apresentadores fazendo alterações em paralelo.

```powershell
.\apresentar.cmd -Diagnostico
.\parar-apresentacao.cmd
```

O diagnóstico exibe o estado dos serviços e as últimas linhas de log com filtragem de credenciais. Se o banco estiver sem a marca ou a carga acusar colisão, guarde o diagnóstico e verifique a origem do volume com a equipe. Se alguma senha local tiver sido perdida, recupere a configuração original antes de reiniciar. O comando de parada mantém contêineres, volume e configuração local. Evite comandos de remoção de volumes ou limpeza geral.
