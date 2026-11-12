# Inventário de testes — Smart Finance

Atualizado: 12/11/2026 (rodada 6) · Suítes: `tests/SmartFinance.Tests` (**136 execuções**) e `tests/SmartFinance.GuiTests` (**12 execuções**).

| Classe | Categoria | Métodos | Objetivo | Qualidade | Observações |
|---|---|---|---|---|---|
| `ValorMonetarioTests` | Unitário | 20 | Parsing/formatação monetária pt-BR (casos válidos, vazios, inválidos, inversão) | Boa | Cobertura de limites (`1,005`, `1.005`, `R$`) |
| `TransacaoServiceTests` | Integração/negócio | 15 | Registro, edição, exclusão, metas, saldo por carteira, rollback | Boa | Valida rollback real com banco (não mock) |
| `CarteiraServiceTests` | Integração/negócio | 7 | Saldos, transferências, validações de saldo inicial | Boa | — |
| `DatabaseTests` | Integração/persistência | 7 | Bootstrap, migration de banco legado 2.x, FTS5, filtros, transação aninhada | Boa | Migration testa schema Java real simulado |
| `PlanejamentoFinanceiroTests` | Unitário/integração | 15 | Cartão, empréstimos, metas (validações, totais, status de vencimento) | Boa | — |
| `ExporterTests` | Unitário | 4 | Formatos CSV (BOM, escapamento) e TXT (resumo) | Boa | — |
| `AuditoriaTests` | Auditoria (fases 3-6) | 14 | Busca degradada, Unicode, volumes (10k), concorrência, corrupção, idempotência | Boa | Criados nesta auditoria; incluem regressões de AUD-001/004 |
| `ManutencaoTests` | Auditoria (P6/P8) | 5 | Backup diário (criação, idempotência, poda) e rotação de log | Boa | Segunda rodada |
| `TestApp` | Suporte | — | Banco SQLite isolado por teste em `%TEMP%` | Adequada | Sem dados reais do usuário |
| `GuiTests` (projeto `SmartFinance.GuiTests`) | GUI/Interação (P1) | 6 | Automação UIA real: dashboard, navegação nas 5 visões, criação de transação persistida, Escape, diálogo de carteiras, tema com persistência | Boa | FlaUI 5; requer sessão interativa; roda no CI e com `--filter Category=GUI` |

## Testes que não existem

- Testes de carga além de 10k registros (suíte atual).
- Testes de acessibilidade automatizados (leitor de tela) — verificação via UIA parcialmente coberta pelos nomes acessíveis usados pela suíte GUI.
- Instalador automatizado no CI (script .iss versionado; ciclo instal/desinstal validado manualmente).

## Qualidade geral

Nenhum teste superficial detectado: todos usam assertions com valores concretos, banco real isolado (não mocks) e cobrem caminhos de erro. Um teste frágil foi corrigido durante a auditoria (asserção de CSV sensível a CRLF).
