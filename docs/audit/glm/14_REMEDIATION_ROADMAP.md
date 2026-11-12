# 14 — Roadmap de correção (GLM)

Base: `13_ISSUES_CATALOG.md` (SF-052…056) + achados herdados pendentes (SF-040, SF-041..051 parcialmente).

## Etapa 0 — CI verde (SF-052)

- **Objetivo:** main verde no CI.
- **Arquivos:** `tests/SmartFinance.GuiTests/DataTransacoesTests.cs`.
- **Ordem:** tornar o commit de data determinístico (LostFocus em vez de ENTER; digitação via teclado no formato pt-BR já garantido pela Language fixa) + retry de asserção com espera.
- **Riscos:** sensibilidade residual a foco — validar no CI antes de fechar.
- **Critério:** CI verde no commit da correção (2 execuções consecutivas recomendado).
- **Regressão:** nada fora do teste.

## Etapa 1 — Integridade financeira (SF-040)

- **IDs:** SF-040 (+NT-1/NT-3), dependente da decisão de competência (preservar original vs. campo no diálogo).
- **Arquivos:** `CartaoCreditoService.cs`, `EmprestimoService.cs`, diálogos.
- **Critério:** NT-1/NT-3 + T-R + F2_* verdes.

## Etapa 2 — Ciclo de atualização (SF-041/053/055/056)

- **IDs:** SF-041 (herdado), SF-053 (decisão de distribuição), SF-055, SF-056.
- **Ordem:** SF-041 (aguardar instalador + reabrir em falha) → SF-053 (decisão: portátil vs. instalado) → SF-055 (Owner+cancelamento) → SF-056 (limpeza).
- **Critério:** recusa de UAC simulada → app reaberto + log; portátil definido como modelo.

## Etapa 3 — Documentação (SF-042) e acabamento (SF-049/050/051)

- Atualizar README/ARQUITETURA/SECURITY para 3.3/3.4; rótulo "Previsão de retorno"; convenção de JSON.

## Etapa 4 — Banco (SF-043/044)

- Decidir FK retroativa (rebuild de tabela) vs. documentação; tratamento de vínculo órfão na edição (NT-5).

## Regressão obrigatória

Suíte completa (136+12) + `CapturaTests` (evidência visual) + instalação silenciosa local a cada etapa que toque distribuição.
