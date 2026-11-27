# EDUCRIA Frontend

Frontend do sistema **EDUCRIA** (aprendizagem adaptativa e gamificada para Ensino Fundamental II), construído com **Next.js 14 (App Router)**, **TypeScript**, **Tailwind CSS**, **Shadcn/ui**, **Zustand**, **Axios** e **TanStack Query**.

## Como executar

O frontend usa caminhos relativos em `/api/`. A variável `EDUCRIA_BACKEND_URL` define o destino no servidor Next e aceita HTTP em `127.0.0.1` ou `localhost` com porta explícita. Reinicie o Next após alterar a variável.

No Codespaces, execute o iniciador na raiz do repositório. Ele define o destino do proxy em `http://127.0.0.1:8080`, com precedência sobre valores herdados. [Guia de inicialização no Codespaces](../INICIAR-CODESPACES.md)

```bash
bash scripts/iniciar.sh
```

No Windows da faculdade, inicie o backend com `$env:SERVER_PORT = "8085"`. Com o backend rodando, abra outro PowerShell na raiz do repositório para iniciar o Next.js. [Roteiro do backend no Windows](../docs/sqlserver/APRESENTAR_WINDOWS.md)

```powershell
Set-Location frontend
$env:EDUCRIA_BACKEND_URL = "http://127.0.0.1:8085"
npm.cmd run dev
```

## Experimentar uma sessão de estudo

1. Na raiz do projeto, execute `SPRING_PROFILES_ACTIVE=dev bash scripts/iniciar.sh` para carregar as questões de demonstração. Abra a porta 3000 e entre como aluno.
2. No painel, clique em **Fazer atividade**. Escolha um assunto ou mantenha **Recomendação automática** e clique em **Iniciar sessão**.
3. Marque uma das alternativas A–D. Clique em **Enviar resposta** para receber a correção e a explicação. Use **Próxima questão** para avançar. Se recarregar a página, o feedback da questão respondida continua disponível.
4. Use **Pausar e sair**. Para voltar à mesma questão, abra **Fazer atividade → Abrir sessão → Retomar sessão**.
5. Ao chegar à décima questão ou esgotar o assunto, use **Encerrar sessão** para abrir o resultado.

No painel do aluno, abra **Minhas revisões** para consultar as questões pendentes e futuras. Ao iniciar uma atividade no assunto, a sessão prioriza questões com revisão vencida.

No topo do painel, abra **Histórico de sessões** e escolha uma sessão para consultar o resultado completo ou parcial, comparar com a anterior e ver as datas de revisão na agenda atual. Em **Suas respostas**, confira o enunciado, a letra enviada e a indicação de acerto ou erro de cada questão.
