# 16 — Handoff para o GLM

**O GLM deverá consultar os documentos desta auditoria (`docs/audit/`) antes de alterar qualquer código. Deverá investigar o contexto de cada achado, corrigir os problemas individualmente ou em grupos tecnicamente relacionados, executar os testes correspondentes e verificar regressões. Não deverá assumir que a recomendação do auditor é necessariamente a única implementação possível.**

O GLM deverá preservar funcionalidades corretas e evitar refatorações não relacionadas aos achados.

## 1. O que é

Smart Finance 3.4.0: app desktop Windows de finanças pessoais (C# 14 / WPF / .NET 10 / SQLite em `%APPDATA%\SmartFinance\financas.db`), reescrito da versão JavaFX 2.x com schema compatível. Distribuição: instalador Inno + exe portátil; CI no GitHub Actions; auto-atualização embutida (F6).

## 2. Estrutura

`Views (WPF) → Services (regras) → Data (SQLite)`; composição em `App.OnStartup` (classe `Servicos`); uma conexão SQLite; `Database.EmTransacao` reentrante; listas com view-models pré-formatados (`Views/Linhas.cs`); testes xUnit (unit `tests/SmartFinance.Tests`, UIA `tests/SmartFinance.GuiTests` com `xunit.runner.json` **serializado** — nunca paralelizar as fixtures de GUI).

## 3. Estado atual (commit `4810844`)

- **148 testes verdes** executados nesta auditoria (136 unit + 12 GUI) — baseline confiável.
- Contraste WCAG AA verificado programaticamente nos dois temas (`ContrasteTests`).
- Auditorias anteriores: rodadas 1-2 (internas, `docs/auditoria/`), rodada 3 (externa estática, `docs/auditoria/externa/` + `TRIAGEM_EXTERNA.md`), rodadas 4-5 (features F1-F6 + verificação visual).

## 4. Achados desta auditoria (13 — `12_ISSUES_CATALOG.md`)

Prioridade: **SF-040** (competência das movimentações) → **SF-041** (falha de instalação) → **SF-042** (docs) → LOW/INFO conforme `13_REMEDIATION_ROADMAP.md`.

## 5. Sequência de execução

1. Ler `00`, `12`, `13`, `14`, `15` desta pasta e `DECISOES_PRODUTO.md` (em `docs/auditoria/`).
2. Baseline: `dotnet build SmartFinance.slnx` + `dotnet test` completo (expectativa: 136 + 12 verdes).
3. Corrigir por etapa do roadmap, com teste que falha → correção → verde → suíte completa.
4. Decisões de produto novas: registrar em `docs/auditoria/DECISOES_PRODUTO.md` antes de codificar.
5. Documentação por último (`11_DOCUMENTATION_AUDIT.md`).

## 6. Cuidados especiais

- **Nunca** usar o `financas.db` real em testes — cópia em `%TEMP%` ou `SMARTFINANCE_DB_PATH`.
- `EmTransacao` reentrante: manter atomicidade nas mudanças de F2 (estorno + inserção).
- Instâncias de `Meta`/`CompraCartaoCredito`/`Emprestimo` vindas da UI são **potencialmente obsoletas** — o banco é a fonte da verdade (padrão SF-004).
- Valores sempre `long` centavos; nada de `double`.
- Mudar `Themes/Templates.xaml` exige revalidar as capturas (`CapturaTests`) — partes `PART_*` do WPF são contratuais.
- GUI tests são **serializados** (`xunit.runner.json`) e precisam de sessão interativa.
- Requisições de rede existem (F6) — não adicionar outras sem decisão explícita.

## 7. Critérios de "concluído" por correção

(1) teste/roteiro que falhava agora passa; (2) suítes completas verdes; (3) linha correspondente preenchida em `17_MATRIZ_DE_RASTREABILIDADE.md`; (4) nenhuma alteração fora do escopo do achado.
