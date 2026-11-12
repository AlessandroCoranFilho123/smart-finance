# 13 — Catálogo de achados (auditoria GLM — SF-052 … SF-056)

**Numeração:** SF-001…029 (auditoria externa), SF-030…037 (revisão), SF-038…039 (rodada 5), SF-040…051 (auditoria executável) já utilizados — esta auditoria continua de **SF-052**. Achados anteriores: ver `18_PREVIOUS_AUDIT_COMPARISON.md`.

| ID | Sev. | Confiança | Categoria | Título |
|---|---|---|---|---|
| SF-052 | MEDIUM | **CONFIRMED** (log CI) | Testes | `DataTransacoesTests` falha no CI: datas digitadas não são commitadas no runner |
| SF-053 | MEDIUM | CONFIRMED (fluxo) | Distribuição | Auto-atualização migra usuários portáteis para a instalação e abandona a cópia original |
| SF-054 | MEDIUM | CONFIRMED (herdado SF-041) | Funcional | Falha na instalação de atualização fecha o app sem reabertura |
| SF-055 | LOW | CONFIRMED (herdado SF-046) | WPF | Janela "Baixando atualização" sem Owner, não cancelável |
| SF-056 | LOW | CONFIRMED (código) | Testes/limpeza | `%TEMP%\SmartFinance-Update` e `docs/auditoria/imagens` acumulam artefatos de teste/atualização |

## SF-052 — Teste GUI de datas falha no CI (main vermelho)

**Severidade:** MEDIUM (bloqueia confiança no pipeline; não é defeito de produto confirmado) · **Confiança:** CONFIRMED (log do runner)
**Localização:** `tests/SmartFinance.GuiTests/DataTransacoesTests.cs` (`PreencherData`/`Limpar`).
**Descrição:** no runner GitHub (cultura en-US, sessão sem interação humana), `ValuePattern.SetValue` + ENTER não **commita** a data no `DatePickerTextBox` — o `SelectedDateChanged` não dispara, o filtro não aplica e a lista mantém 3 itens (esperado 2). Local (pt-BR, sessão interativa) passa.
**Evidência:** log do job `131296154751` (run do commit `722ceeb`): `Assert.Equal() Failure: Expected: 2 / Actual: 3` — 11/12 GUI.
**Como reproduzir:** CI do repositório; localmente opcional forçar cultura en-US do runner.
**Comportamento esperado:** datas commitam independentemente de foco/cultura OU o teste entra pela via determinística.
**Impacto:** main vermelho → confiança no pipeline reduzida.
**Causa provável:** ENTER global não alcança o campo (foco do runner) e/ou parse de data com cultura do runner.
**Correção recomendada (uma linha/algumas):** após `SetValue`, forçar commit por **LostFocus** (clicar/focar outro elemento) em vez de ENTER, **e/ou** digitar via `Keyboard.Type` no formato da cultura do DatePicker (Language pt-BR fixo — dd/MM/yyyy); alternativa determinística: disparar o commit apertando TAB. Validar no CI antes de fechar.
**Teste de validação:** o próprio `DataTransacoesTests` verde no CI.
**Dependências:** nenhuma.

## SF-053 — Auto-atualização migra usuários portáteis para instalação

**Severidade:** MEDIUM · **Categoria:** Distribuição · **Confiança:** CONFIRMED (fluxo)
**Descrição:** o F6 baixa sempre o **instalador** (asset `SmartFinance-Setup-*`), que instala em `{autopf}`. Quem executa o **exe portátil** (ex.: pasta Downloads) recebe uma segunda cópia instalada; a portátil fica obsoleta.
**Impacto:** duplicidade de cópias e confusão sobre qual versão está usando.
**Correção recomendada:** decisão de produto — (a) detectar modo portátil (exe em pasta gravável sem `unins000.exe`) e atualizar **in-place** o próprio exe (substituição agendada com batch de reinício); ou (b) documentar que o portátil é "modo avaliação" e a atualização converte para instalado.
**Teste de validação:** atualização simulada do cenário portátil.
**Dependências:** decisão do usuário.

## SF-054 — Falha na instalação de atualização encerra sem reabertura
**Severidade:** MEDIUM · **Confiança:** CONFIRMED (fluxo) · Herdado (SF-041 da auditoria executável). Correção: aguardar o processo do instalador; se falhar, reabrir e logar.

## SF-055 — Janela de download sem Owner/não cancelável
**Severidade:** LOW · **Confiança:** CONFIRMED · Herdado (SF-046). Correção: `Owner = MainWindow` + `CancellationToken` no download.

## SF-056 — Acúmulo de artefatos (temp do F6 e imagens de captura)
**Severidade:** LOW · **Confiança:** CONFIRMED (código) · `%TEMP%\SmartFinance-Update` sem limpeza de versões antigas; `CapturaTests` grava em `docs/auditoria/imagens` (árvore do repositório) durante o teste. Correção: limpar pasta antes de baixar; capturas com flag/destino configurável.

## SF-057 — SeletorData não exposto na árvore UIA no runner de CI

**Severidade:** MEDIUM · **Confiança:** CONFIRMED (log do runner)
**Localização:** `Views/SeletorData.xaml.cs` (UserControl sem peer de automação próprio)
**Descrição:** WPF `UserControl` **não gera peer de automação que expõe o controle/nome** por padrão — no runner de CI, `ByAutomationId("DataInicio")` não encontra o SeletorData e o marcador da tela não aparece (a tela até carrega: o log do runner mostra apenas "startup ok", sem exceções).
**Evidência:** CI `fe19ea0` — erro "campo 'De' não encontrado" + log do app sem exceções + "startup ok".
**Correção recomendada:** override `OnCreateAutomationPeer` em `SeletorData` retornando `FrameworkElementAutomationPeer` (expõe o controle com seu AutomationId), ou remover o UserControl e compor o botão+popup diretamente nas visões.
**Validação:** `DataTransacoesTests` verde no CI.
