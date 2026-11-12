# 17 — Handoff final (GLM → etapa de implementação)

## Contexto

Smart Finance 3.4.0 (`722ceeb`): app desktop Windows de finanças pessoais (C# 14 / WPF / .NET 10 / SQLite em `%APPDATA%\SmartFinance\financas.db`), compatível com bancos 2.x, com 148 testes (136 unit + 12 GUI) verdes localmente, CI no GitHub Actions, instalador Inno + portátil, auto-atualização (F6) e backup diário.

## Estado no commit auditado

- Build Debug/Release: 0 erros/0 warnings.
- Testes: **136 unit + 12 GUI verdes localmente**; CI: unit verde, **GUI vermelho** (SF-052).
- Segurança: sem vulneráveis, sem segredos, SQL parametrizado.
- Achados abertos: SF-040 (HIGH, herdado), SF-052/053/054 (MEDIUM, novos/herdados), SF-055/056 e SF-043..051 (LOW/INFO).

## Problemas prioritários

1. **SF-052** — CI vermelho em main (teste GUI sensível a foco/cultura no runner). Correção no teste; validar no CI.
2. **SF-040** — competência das movimentações de cartão/empréstimo (decisão de produto antes de codificar).
3. **SF-054/SF-041** — robustez do ciclo de atualização.
4. **SF-053** — decisão de distribuição (portátil vs. instalado).

## Restrições

- Não usar o `financas.db` real em testes (cópia/`SMARTFINANCE_DB_PATH`).
- Não introduzir `double` em valores; nada de `long` centavos quebrado.
- Não quebrar compatibilidade de schema 2.x.
- `EmTransacao` obrigatório em operações compostas; manter rollback total.
- GUI tests serializados e com sessão interativa.
- Mudanças em `Themes/*` exigem re-execução de `CapturaTests`.

## Ordem de execução e estratégia de testes

Ver `14_REMEDIATION_ROADMAP.md` e `15_TEST_PLAN_FOR_IMPLEMENTATION.md`.

## Critérios de aceite

(1) teste/roteiro que falhava passa; (2) suítes completas verdes (136+12) no CI; (3) matriz `docs/audit/17_MATRIZ_DE_RASTREABILIDADE.md` atualizada com resultado real; (4) nenhuma alteração fora do escopo do achado; (5) decisões novas registradas em `DECISOES_PRODUTO.md`.
