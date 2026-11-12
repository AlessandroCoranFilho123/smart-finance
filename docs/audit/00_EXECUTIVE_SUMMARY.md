# 00 — Sumário executivo

**Projeto:** Smart Finance 3.4.0 (C# 14 / WPF / .NET 10 / SQLite)
**Commit auditado:** `4810844` (main) · **Data:** 12/11/2026 · **Tipo:** auditoria **executável** (leitura integral + compilação + suítes completas + sondagens locais)
**Código alterado:** nenhum · **Única alteração:** criação de `docs/audit/`

## Números

| Item | Valor |
|---|---|
| Arquivos versionados | 162 (`git ls-files`) |
| Funcionalidades inventariadas | 10 (transações, carteiras/transferências, metas, cartão+parcelas, empréstimos, dashboard, busca/filtros, exportação, temas, auto-atualização) |
| Testes executados nesta auditoria | **136 unitários + 12 GUI** — **148/148 aprovados** |
| Achados | **13**: 1 HIGH · 2 MEDIUM · 5 LOW · 5 INFO |
| Devedor conhecido | F4 parcial (Win11/suspensão/leitor físico), SF-022 (rowid, decisão), U1 (sem certificado — decisão do usuário) |
| Confiança | Achados com evidência de código/esquema; nenhum reprodutido em GUI manual (automatizado quando possível) |

## Estado geral

A base está madura: 148 testes verdes no commit auditado, WCAG AA verificado programaticamente nos dois temas, migrações compatíveis com 2.x testadas, sem dependências vulneráveis, zero TODOs. O risco concentrado está em **duas frentes novas e pouco battle-tested**: (1) as movimentações de carteira geradas por cartão/empréstimo (D-10) fixam a **data de criação** — edição reposiciona o histórico e a competência mensal não é controlável (SF-040); (2) o **fluxo de auto-atualização** fecha o aplicativo para instalar e, se o instalador falhar (ex.: UAC recusado), o usuário fica sem reabertura nem mensagem específica (SF-041).

## Integridade financeira

Criação/edição/exclusão de transações, metas, cartão e empréstimos são atômicos e reconciliáveis (invariante I1 coberto por teste `TR_*`). Divergência: a **data de competência** das movimentações geradas (SF-040) distorce os totais mensais quando o cadastro ocorre em mês diferente da operação real.

## Segurança

Sem superfície de rede além da checagem de versão/download do instalador (ambos HTTPS + GitHub). SQL 100% parametrizado. Sem segredos. Auto-update: hash do instalador publicado nas notas e conferido — porém publicado no mesmo canal da release (SF-047, INFO).

## Documentação

`README.md`, `docs/ARQUITETURA.md` e `docs/SECURITY.md` **não cobrem os recursos 3.3/3.4** (parcelas pagas, movimentações de cartão/empréstimo, instância única, auto-atualização, acessibilidade) — SF-042.

## Limitações

CI do commit auditado seguia em fila durante a auditoria (baseline local executado como compensação); Windows 11/suspensão/leitor de tela físico não simuláveis. Detalhes em `15_AUDIT_LIMITATIONS.md`.

## Recomendação

Executar `13_REMEDIATION_ROADMAP.md` na ordem: SF-040 (etapa 1) → SF-041 (etapa 0) → SF-042 (etapa 6) → demais LOW/INFO conforme oportunidade.
