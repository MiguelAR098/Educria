# EDUCRIA Frontend

Frontend do sistema **EDUCRIA** (aprendizagem adaptativa e gamificada para Ensino Fundamental II), construído com **Next.js 14 (App Router)**, **TypeScript**, **Tailwind CSS**, **Shadcn/ui**, **Zustand**, **Axios** e **TanStack Query**.

## Como executar

Siga o [guia de inicialização no Codespaces](../INICIAR-CODESPACES.md). O frontend usa caminhos relativos em `/api/` e o servidor Next encaminha as requisições à API interna.

## Experimentar uma sessão de estudo

1. Na raiz do projeto, execute `SPRING_PROFILES_ACTIVE=dev bash scripts/iniciar.sh` para carregar as questões de demonstração. Abra a porta 3000 e entre como aluno.
2. No painel, clique em **Fazer atividade**. Escolha um assunto ou mantenha **Recomendação automática** e clique em **Iniciar sessão**.
3. Marque uma das alternativas A–D. Clique em **Enviar resposta** para conferir a correção, a explicação e os pontos concedidos. Use **Próxima questão** para avançar. Recarregue a página para conferir que o total da sessão e o feedback permanecem disponíveis.
4. Use **Pausar e sair**. Abra **Fazer atividade → Abrir sessão → Retomar sessão** e confira a questão e a pontuação preservadas.
5. Ao chegar à décima questão ou esgotar o assunto, use **Encerrar sessão** para abrir o resultado.

No painel do aluno, abra **Minhas revisões** para consultar as questões pendentes e futuras. Ao iniciar uma atividade no assunto, a sessão prioriza questões com revisão vencida.

No painel, confira os pontos privados da matéria e expanda os assuntos para ver seus totais. Em **Histórico de sessões**, abra o resultado para conferir os pontos da sessão e os acumulados do assunto e da matéria. Conclua duas sessões do mesmo assunto para conferir a comparação. Sessões antigas sem pontos registrados exibem **Pontuação não registrada**. Em **Suas respostas**, confira o enunciado, a letra enviada e a indicação de acerto ou erro de cada questão.
