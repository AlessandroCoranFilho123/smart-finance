# 13 — Roadmap de correção

Base: `12_ISSUES_CATALOG.md`. Cada etapa fecha com suíte completa verde + testes citados.

## Etapa 0 — UX do ciclo de atualização

- **IDs:** SF-041, SF-046, SF-048.
- **Objetivo:** o fluxo F6 nunca deixar o usuário sem aplicativo nem sem explicação.
- **Ordem:** SF-041 → SF-046 → SF-048.
- **Arquivos:** `MainWindow.xaml.cs` (OferecerAtualizacao), `AtualizacaoService.cs`, `CriarJanelaBaixando`.
- **Riscos:** aguardar o instalador pode demorar (instalação ~10s); usar timeout + checagem de processo do app.
- **Critério:** recusa de UAC simulada → app reaberto + log; janela de download com Owner e cancelamento.
- **Regressão:** fluxo feliz de atualização (instalação silenciosa local).

## Etapa 1 — Competência das movimentações (SF-040)

- **Objetivo:** movimentações geradas por cartão/empréstimo preservam a data de competência; edição não reposiciona.
- **Decisão de produto a registrar antes:** competência = data do cadastro original preservada na edição, ou campo de data no diálogo (recomendado: preservar original + campo opcional).
- **Arquivos:** `CartaoCreditoService.cs`, `EmprestimoService.cs`, 2 diálogos, `Linhas.cs`.
- **Riscos:** T-R e F2_*; ordem do histórico.
- **Critério:** NT-1/NT-3 verdes + reconciliação.

## Etapa 2 — Banco (SF-043/044) e docs (SF-042)

- **IDs:** SF-043, SF-044, SF-042.
- **Objetivo:** documentar decisão sobre FKs retroativas (migration com rebuild de tabela vs. aplicação) e atualizar as 3 docs funcionais.
- **Critério:** docs coerentes com 3.4; teste sintético de vínculo órfão (SF-044).

## Etapa 3 — Acabamento (SF-045/046 rótulo/051)

- IDs SF-045 (documentar categoria), SF-051 (rótulo "Previsão de retorno"), SF-048/049/050 (limpeza de temp, caminho de paletas, versionamento de JSON).
- **Critério:** testes existentes verdes; docs atualizadas.

## Etapa 4 — Cobertura (NT-4)

- Abstrair cliente HTTP de `AtualizacaoService` e testar `VerificarNovaVersaoAsync` (nova/igual/erro).

## Regressão obrigatória por etapa

`dotnet test tests/SmartFinance.Tests` (136) + `tests/SmartFinance.GuiTests` (12) + capturas de tela nos dois temas (`CapturaTests`).
