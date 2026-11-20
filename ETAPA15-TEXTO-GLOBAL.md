# EDUCRIA · Tamanho do texto em toda a aplicação

Base verificada em 20/11/2026: main no commit `926790505f109357305f9ec92432462d5978bd1f`, com o pacote local `educria-etapa15-parte2.zip` aplicado antes da correção.

Na entrega anterior, a página da questão lia a preferência ao montar e alterava uma variável CSS dentro do cartão da atividade. As outras páginas não liam essa escolha.

Agora há um único seletor nativo **Tamanho do texto**, na faixa superior, antes da navegação. Ele aparece também no login e no cadastro. A opção Normal usa a fonte padrão do navegador. Grande usa 125% e Muito grande usa 150%. O layout raiz aplica a preferência ao HTML antes da hidratação do React. Os textos em rem acompanham a raiz. Os rótulos menores e as notificações também usam unidades relativas.

A chave de armazenamento da parte 2 foi preservada. A escolha continua após F5 e é compartilhada entre as áreas públicas e autenticadas no mesmo navegador e endereço. Botões podem ocupar duas linhas. O catálogo reposiciona o nível e a contagem de assuntos quando precisam de espaço. Em cartões estreitos, a ilustração do nível cede espaço ao texto.

## Verificação

- Build de produção do Next.js, TypeScript e ESLint com as regras de next/core-web-vitals aprovados.
- Dez testes de interface aprovados no Chromium com API simulada. Os dois testes de integração que exigem Spring e PostgreSQL não foram executados nesta correção.
- Páginas verificadas: login, cadastro, painel do aluno com catálogo, preparação da sessão, questão com alternativas e feedback, histórico, resultado com tabela de comparação, agenda de revisões e administração.
- As verificações cobriram aumento a partir do painel, troca de página, recarga, retorno a Normal, preferência da versão anterior, teclado, textos longos e ausência de rolagem horizontal da página nas larguras de 320, 375, 768 e 1280 px.
- Ampliação de 200% testada por emulação no Chromium, incluindo escala visual de 2. O zoom pelo menu do navegador, leitor de tela e aparelho físico não foram testados.
- Verificação adicional em perfil móvel Pixel 7 emulado, com fonte padrão do navegador em 20 px. A opção Muito grande resultou em 30 px mesmo com os scripts de hidratação bloqueados. Com os scripts liberados, o painel abriu sem erro de hidratação.

## Aplicar no Windows

Coloque `educria-texto-global.zip` na pasta que contém `frontend` e `backend`. Abra o PowerShell nessa pasta e execute:

```powershell
Expand-Archive -LiteralPath .\educria-texto-global.zip -DestinationPath . -Force
```

Se o frontend já roda em modo de desenvolvimento, atualize a página. Para iniciá-lo a partir da raiz:

```powershell
cd frontend
npm.cmd run dev
```

Para conferir, escolha **Grande** no painel, abra uma atividade, navegue ao histórico e pressione F5. O seletor deve continuar em **Grande**. Escolha **Normal** para restaurar o tamanho padrão.
