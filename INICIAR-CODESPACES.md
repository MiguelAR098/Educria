# Iniciar o EDUCRIA no GitHub Codespaces

Você precisa de uma conta no GitHub com acesso a Codespaces e do arquivo `educria-codespaces.zip`. Todo o processo pode ser feito no navegador.

## 1. Colocar o projeto no GitHub · uma vez

1. No GitHub, clique em **New repository**. Dê um nome como `educria`, marque **Private**, ative **Add a README file** e clique em **Create repository**.
2. No repositório, clique em **Code → Codespaces → Create codespace on main**. Aguarde o editor abrir.
3. No painel **Explorer** à esquerda, arraste o arquivo **educria-codespaces.zip** do seu computador para a pasta principal, ao lado do README. Aguarde o envio terminar.
4. Abra **Terminal → New Terminal**. Cole o bloco abaixo. Ele extrai o projeto e envia os arquivos ao seu repositório. O próprio ZIP fica fora do commit.

```bash
cd "$(git rev-parse --show-toplevel)"
unzip -o educria-codespaces.zip
git add .
git commit -m "Adicionar EDUCRIA consolidado para Codespaces"
git push
```

5. Pressione **F1**, procure **Codespaces: Rebuild Container** e confirme. Aguarde a reconstrução terminar. Isso instala no Codespace as versões de Java, Maven, Node e Docker usadas pelo projeto.

Se o repositório já mostra `.devcontainer/`, `backend/`, `frontend/` e `compose.yaml` na página principal, basta abrir um Codespace novo nesse repositório e seguir o passo 2.

## Atualizar um Codespace já existente

1. No terminal que executa o projeto, pressione **Ctrl+C**. Guarde suas alterações locais caso tenha editado o código.
2. Envie o ZIP completo atualizado para a raiz do mesmo Codespace, com o nome `educria-codespaces.zip`. Substitua o ZIP antigo.
3. Na raiz, extraia o backend e o frontend juntos e reconstrua a aplicação com as questões de demonstração.

```bash
unzip -o educria-codespaces.zip
SPRING_PROFILES_ACTIVE=dev bash scripts/iniciar.sh
```

Quando aparecer **Ready**, abra a porta **3000** e recarregue a página com **Ctrl+Shift+R**. A atualização preserva o volume do PostgreSQL e o arquivo `.env`.

No topo do painel de aluno, abra **Histórico de sessões**. Para testar uma resposta, clique em **Fazer atividade**, escolha um assunto, inicie a sessão, marque A–D e clique em **Enviar resposta**. A correção aparece na mesma questão. **Próxima questão** avança depois da correção. Ao encerrar, abra **Suas respostas** no resultado para conferir cada questão e a alternativa enviada. Sessões de versões que apenas apresentavam questões podem ter itens sem resposta registrada.

## 2. Iniciar o EDUCRIA

Abra um terminal na pasta principal e execute:

```bash
bash scripts/iniciar.sh
```

O comando prepara as credenciais no primeiro uso, inicia o PostgreSQL 16, compila e testa o backend, instala as dependências do frontend e compila o Next. Em seguida, o Spring aplica as migrations e o Next começa a atender. A primeira execução demora mais por causa dos downloads. **Deixe esse terminal aberto e aguarde aparecer `Ready`.**

## 3. Abrir o site e testar

1. Na parte inferior do editor, abra **Portas** ou **Ports**, ao lado de **Terminal**. Na linha **3000 · EDUCRIA**, clique no ícone de globo para abrir o site em outra aba. Mantenha a visibilidade **Private**. A aplicação usa internamente as portas 8080 e 5432.
2. No site, abra **Cadastre-se gratuitamente**. Informe nome, e-mail e uma senha de teste com pelo menos seis caracteres. Deixe turma e matrícula em branco. Envie o cadastro.
3. Você deve chegar ao painel do aluno com nível **1/10**, zero acertos e zero erros. Clique em **Sair**, faça login com o mesmo e-mail e senha e confira se o painel volta a abrir.

**Verificação automática opcional.** Com o site em execução, abra um **segundo terminal** na pasta principal e rode:

```bash
node scripts/verificar.mjs
```

O teste cria um aluno de teste, confere as migrations e os oito assuntos, testa o cadastro com o cabeçalho do frontend e uma origem HTTPS diferente da configurada no servidor, verifica o JWT e testa a recusa de acesso sem token e com perfil inadequado. Cada etapa aprovada imprime `OK`. Para conferir no próprio navegador, abra **F12 → Rede**, faça um novo cadastro e procure `/api/auth/register` com status **201** no mesmo endereço HTTPS do site.

## 4. Parar e voltar depois

1. No terminal do iniciador, pressione **Ctrl+C** para encerrar o site e a API.
2. Para parar também o banco, execute `docker compose stop db`. Os dados ficam no volume `dados`.
3. Em **github.com/codespaces**, abra o menu **…** do Codespace e escolha **Stop codespace**.
4. Para voltar, reabra **o mesmo Codespace** e execute novamente `bash scripts/iniciar.sh`. O banco volta com os cadastros e a chave JWT anterior.

Guarde o arquivo `.env` gerado dentro desse Codespace. Ele é ignorado pelo Git. Excluir o Codespace ou apagar o volume de dados remove a instalação local do banco.

## Se algo travar

| O que aparece | O que fazer |
|---|---|
| Comando `mvn`, `docker` ou `node` ausente | Confirme que há uma pasta `.devcontainer` na raiz do repositório. Faça **F1 → Codespaces: Rebuild Container** e abra um terminal novo. |
| A porta 3000 ainda não aparece | Confira se o terminal chegou a `Ready`. Depois abra **Portas → Forward a Port** e digite `3000`. |
| O cadastro falha | Abra a porta **3000** em outra aba pelo globo, confira a primeira mensagem de erro no terminal do iniciador e rode a verificação automática. Use um e-mail novo para cada cadastro. |

Ajuda do GitHub sobre [como abrir uma porta do Codespace](https://docs.github.com/pt/codespaces/developing-in-a-codespace/forwarding-ports-in-your-codespace) e [como reconstruir o contêiner](https://docs.github.com/pt/codespaces/developing-in-a-codespace/rebuilding-the-container-in-a-codespace).
