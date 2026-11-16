# Guardas de regressão — cada classe de bug e o teste que a prende

Este documento mapeia **cada classe de bug** que já chegou ao usuário (ou quase)
nas rodadas de auditoria (2026-11) ao **teste automatizado que a impede de
voltar**, com o modo de reprodução manual. Se um bug desta lista regressar, é
porque o teste correspondente foi removido, enfraquecido ou ignorado.

Executar as suítes:

```bash
dotnet test tests/SmartFinance.Tests        # 225 testes unitários (~35 s)
dotnet test tests/SmartFinance.GuiTests     # 16 testes GUI (sessão interativa, ~3 min)
```

Requisitos da suíte GUI: Windows com sessão interativa (o runner do CI atende),
`.NET 10`; o aplicativo é lançado com banco isolado em `%TEMP%` (`SMARTFINANCE_DB_PATH`),
exceto `DataTransacoesTests` (ver limitação WAL abaixo).

---

## 1. Codificação de fontes (mojibake no binário)

- **Sintoma histórico**: MessageBox de instância única exibia "O Smart Finance jÃ¡
  estÃ¡ em execuÃ§Ã£o."; strings de log garimpadas ("backup diÃ¡rio").
- **Causa raiz**: um **único byte cp1252 inválido** (0xE3) dentro de um comentário
  de `App.xaml.cs`. O Roslyn valida o arquivo como UTF-8, falha, e recodifica o
  **arquivo inteiro** no codepage ANSI — todas as strings acentuadas do arquivo
  viram mojibake no binário. Arquivos UTF-8 válido **sem BOM** compilam
  corretamente (o BOM é opcional; a validade estrita não é).
- **Guarda**: `ConsistenciaProjetoTests.Fontes_SaoUtf8Estrito` — varre **todos**
  os `.cs` e `.xaml` de `src/` decodificando com `UTF8Encoding(throwOnInvalidBytes: true)`;
  cada arquivo é um caso nomeado da theory.
- **Reprodução manual**: gravar qualquer byte inválido (ex.: `ç` em cp1252 = 0xE7
  após uma sequência quebrada) em um .cs e reconstruir — o texto acento do
  binário sai "Ã©"/"Ã§".
- **Detecção em produção**: mojibake em `%APPDATA%\SmartFinance\erro.log` ou em
  qualquer diálogo = arquivo corrompido na árvore.

## 2. Bump de versão corrompendo o manifest (exe que não inicia)

- **Sintoma histórico**: release 3.4.5 com instalador cujo exe morria no arranque
  com "side-by-side configuration is incorrect"; CI 100% vermelho nos testes GUI.
- **Causa raiz**: o bump de versão do `Publicar.ps1` usava `-replace` (que troca
  **todas** as ocorrências) de `version="[0-9.]+\.0"` no `app.manifest` — atingiu a
  declaração XML (`<?xml version="1.0"?>`) e o `manifestVersion="1.0"` além do
  `assemblyIdentity`, gerando um manifest inválido.
- **Guardas**:
  - `ConsistenciaProjetoTests.Manifest_TemEstruturaValidaEVersaoSincronizada` —
    valida o manifest como XML: declaração `1.0` intacta, `manifestVersion="1.0"`,
    e `assemblyIdentity/@version` **igual à `<Version>` do csproj + ".0"**;
  - `ConsistenciaProjetoTests.Versao_DoCsproj_CasaComOInstalador` — csproj ×
    `SmartFinance.iss` (o `-replace` de bump também é global no iss).
- **Reprodução manual**: aplicar o replace antigo no manifest, reconstruir e
  iniciar o exe — erro SxS imediato. O regex correto (em `Publicar.ps1`) só toca
  `(<assemblyIdentity[^>]*version=")[0-9.]+(")`.

## 3. Parsing monetário (D-03/D-16)

- **Sintoma histórico**: valores ≥ 1000 com vírgula decimal e sem milhar
  ("1234,56", "2500,0") eram **recusados** com "Valor inválido"; entradas de 28
  dígitos vazavam `OverflowException` (não-`ArgumentException`) por cima do
  guard de `long`.
- **Guarda**: `ValorMonetarioTests` — casos válidos incluem `1234,56`, `2500,0`,
  `123456,78`, `1.005`, `1.234.567`; recusados incluem os ambíguos (`1,005`,
  `10,123`, `0.500`, `1,234.56`) e os de estouro (`99999999999999999999`,
  `9999999999999999999999999999,99` — este último quebrava antes do guard).
- **Reprodução manual**: digitar `2500,00` em qualquer campo de valor e salvar —
  na 3.4.4 aparecia "Valor inválido".

## 4. Competência das movimentações de cartão/empréstimo (D-13)

- **Sintoma histórico**: editar uma compra/empréstimo re-datava a movimentação
  vinculada para **hoje**, movendo o lançamento de mês no dashboard ("Pedi
  emprestado com pagamento futuro inflando receita do mês atual"). Relatórios
  de auditoria afirmavam "RESOLVIDO com teste novo" — falso (nenhum teste
  existia); os relatórios receberam correção datada.
- **Guarda**: `RecursosNovosTests.F2_EditarCompra_PreservaDataDaMovimentacao` e
  `F2_EditarEmprestimo_PreservaDataDaMovimentacao` — re-datam a movimentação
  para `Hoje-30` via `Transacoes.Atualizar`, editam a origem e afirmam que a
  data não saltou.
- **Reprodução manual**: cadastrar compra, editar a movimentação para uma data
  antiga, editar a compra — na 3.4.6 a movimentação saltava para hoje.

## 5. Estorno de cartão/empréstimo (criação de dinheiro)

- **Sintoma histórico**: `EstornarMovimentacao` fazia DELETE direto no
  repositório. Se a movimentação vinculada tivesse sido reclassificada para
  "Aplicar em meta" (o TransacaoDialog permite), excluir a compra apagava a
  transação **sem reverter** `meta.AtualCentavos` — carteira de volta + meta
  mantida = patrimônio criado do nada (viola I1).
- **Guardas**: `F2_EstornoReverteMeta_QuandoMovimentacaoFoiReclassificada`
  (meta volta a zero, carteira volta ao inicial) e
  `F2_EstornoBloqueado_QuandoMetaParcialmenteResgatada` (SF-006: bloqueia com
  ArgumentException e o estado permanece íntegro).
- **Reprodução manual**: compra → editar a movimentação para "Aplicar em meta"
  → excluir a compra no Cartão — na 3.4.6 o acumulado da meta sobrava órfão.

## 6. Cores de texto dentro de templates (SF-025 estendido)

- **Sintoma histórico**: o estilo implícito de `TextBlock` (`Foreground` fixado)
  **vence** qualquer `Foreground` herdado/anexado dentro de templates — texto do
  botão primário saía azul-marinho sobre azul (2,89:1 no claro), "Excluir" nunca
  ficava vermelho, texto do toggle selecionado nunca ficava verde, estados
  IsToday/IsSelected/IsInactive do calendário ignorados; e a primeira tentativa
  de correção via `TemplateBinding Foreground` deixou o toggle **preto** (o
  TemplateBinding não resolve a cadeia `Binding(Tag) → DynamicResource`).
- **Guardas**:
  - `CapturaTests.VerificarCoresToggles` — o texto do toggle SELECIONADO
    ("Entrada") contém pixels na cor do estado (verde) e o NÃO selecionado não
    herda cor alheia, nos dois temas;
  - `CapturaTests.VerificarCoresBotoesEdicao` — "Excluir" vermelho e
    "Salvar/Atualizar" branco sobre o accent;
  - `CapturaTests.VerificarCoresDoCalendario` — célula de hoje na cor accent-
    soft, texto azul-acento, cartão do popup na cor do card.
- **Mecanismo (documentação viva)**: estilos implícitos de elementos **não-Controle**
  (TextBlock) atravessam fronteiras de template; para vencê-los o elemento dentro
  do template precisa de valor **local** (`TemplateBinding`/`Binding` explícitos no
  TextBlock) — `TextElement.Foreground` no ContentPresenter é apenas HERANÇA e
  perde. Cadeias de duas indireções (`Foreground=Binding(Tag)` →
  `Tag=DynamicResource`) também falham — por isso as variantes
  `BotaoAlternarVerde/Vermelho/Laranja` usam `DynamicResource` **direto** no
  trigger do estilo do botão.
- **Reprodução manual**: tema escuro → "Nova transação" → o toggle "Entrada"
  selecionado exibia texto preto na 3.4.6; "Excluir" no diálogo de edição nunca
  ficava vermelho antes da 3.4.6.

## 7. Alturas e alinhamento de campos (TextBox com padding duplo)

- **Sintoma histórico**: o campo Buscar (e todos os TextBox) com ~48px de altura
  contra ~30px dos ComboBox/SeletorData na mesma linha — o template aplicava o
  `Padding` como `Margin` no `PART_ContentHost`, mas o `TextBoxView` interno
  **já aplica** o `Padding` do controle.
- **Guarda**: `AlinhamentoTests.CamposDoFiltro_MesmaAlturaEAlinhamento` (retângulos
  UIA, tolerância ±2px) e `BotoesDoDialogoEdicao_NaMesmaLinha` (Excluir na mesma
  linha dos demais — o botão chegou a renderizar sem borda, fora da linha).
- **Reprodução manual**: comparar visualmente o campo Buscar com o campo Tipo na
  tela de transações (na 3.4.4 a diferença era óbvia).

## 8. Calendário do SeletorData (tema escuro ilegível / visões)

- **Sintoma histórico**: template padrão do WPF com texto preto sobre fundo
  transparente (ilegível no escuro); resquício do template padrão desenhando o
  dia de hoje; visão de meses/anos não alternava (o trigger errado —
  `DisplayMode` é do `Calendar`, não do `CalendarItem`).
- **Guardas**: `CapturaTests.CapturarCalendarioNosDoisTemas` — captura mês e
  meses nos dois temas + asserta cores por pixel (cartão, hoje, campo de data);
  `DataTransacoesTests` afirma que o popup abre (`AguardarCalendario`, padrão
  `WhileNull` — o helper antigo com `WhileTrue` invertido era código morto).
- **Convenções vivas** (ARQUITETURA.md > UI e temas): day/month buttons entram
  por `CalendarDayButtonStyle`/`CalendarButtonStyle` (estilos implícitos não
  atravessam a árvore do template do `CalendarItem`); troca de visão por
  `DataTrigger` no `DisplayMode` do `Calendar` ancestral.

## 9. SF-009 — banco corrompido

- **Sintoma histórico**: o teste validava o **diálogo errado** — o app da fixture
  segurava o mutex de instância única, então o segundo processo mostrava "já
  está em execução" antes do bootstrap e o teste passava vazio (e a captura
  arquivada mostrava o diálogo errado, com mojibake).
- **Guarda**: `CorrompidoTests.SF009_BancoCorrompido_MostraErroEEncerra` — classe
  PRÓPRIA sem app prévio, **asserta o texto** ("Falha ao iniciar"), fotografa e
  verifica o encerramento.
- **Reprodução manual**: gravar bytes aleatórios em um .db e apontar
  `SMARTFINANCE_DB_PATH` para ele — mensagem clara + encerramento limpo.

## 10. Auto-atualização (F5/F6)

- **Sintoma histórico**: instalador podia ser executado sem verificação quando
  a release não publicava hash nas notas; teste F5 mutava o
  `%APPDATA%\SmartFinance\atualizacao.json` real do usuário.
- **Guardas**: `MainWindow.OferecerAtualizacao` recusa instalação silenciosa sem
  hash (abre a página da release) — [INCONFIRMADO em teste automatizado: requer
  rede/Release; coberto indiretamente por `ExtrairHashSetup`/`VerificarIntegridade`
  (unitários) e pela verificação ponta a ponta do `Publicar.ps1`];
  `F5_DeveVerificarHoje_MarcaUmaVezPorDia` roda com arquivo de controle em temp
  (restaura o original em `finally`).
- **Pilha de segurança**: HTTPS + repositório fixo + SHA-256 publicado nas notas
  (formato parseado por `ExtrairHashSetup`) + recusa sem hash.

---

## Exclusões do CI (e por quê)

| Exclusão (ci.yml) | Motivo |
|---|---|
| `FullyQualifiedName!~Navegacao_Alcancar` | sensível ao runner (sessão/foco) |
| `FullyQualifiedName!~DataTransacoes` | seed via segunda conexão SQLite invisível para a conexão WAL aberta do app — roda localmente |
| (categoria `Captura` roda no CI) | capturas + assertions de pixel; flake ocasional de navegação no runner — se falhar, reexecutar antes de investigar |

## Como adicionar uma nova guarda

1. Identifique a **classe** do bug (não o sintoma isolado).
2. Prefira guarda no nível mais baixo possível que a pegue: unitário (regra),
   `ConsistenciaProjetoTests` (integridade de arquivos/config), GUI com UIA
   (estrutura visual) ou pixels (cor renderizada).
3. Registre a classe nesta página: sintoma → causa → teste → reprodução manual.
4. Um bug só é considerado fechado com o teste verde **no CI** e a reprodução
   manual documentada aqui.
