# Painel de leitura do EDUCRIA

Base: main em `9267905` e a entrega `educria-texto-global.zip`. Aplique este ajuste sobre a versão que já exibe o seletor global.

O ícone de ajustes no topo abre **Configurações de leitura**. O painel usa o seletor existente com Normal, Grande e Muito grande. A escolha é aplicada imediatamente. O botão X ou Esc fecha o diálogo e devolve o foco ao botão de ajustes.

O patch altera o componente TamanhoTexto e os testes de interface que acessam o seletor. A lógica de tamanho e persistência é a mesma da entrega anterior.

## Aplicar no VS Code

1. Abra a pasta do EDUCRIA que contém frontend e backend.
2. Coloque o ZIP nessa pasta e abra **Terminal > Novo Terminal** com PowerShell.
3. Execute:

```powershell
Expand-Archive -LiteralPath .\educria-painel-leitura.zip -DestinationPath . -Force
```

Com o frontend em modo de desenvolvimento, atualize a página. Para conferir, abra o painel, escolha Grande, feche com Esc e navegue para outra página. Ao reabrir o painel, Grande deve continuar selecionado.

Validação: build de produção e TypeScript aprovados. Onze testes de interface passaram no Chromium com API simulada, incluindo abertura pelo teclado, Esc, retorno do foco, fechamento pelo botão e painel em 320 e 1280 px. Dois testes que exigem backend real foram omitidos.
