# Inventário de testes — Smart Finance

Data: 10/11/2026 · Suíte: `tests/SmartFinance.Tests` (xUnit, .NET 10) · **79 execuções** (49 métodos, `[Theory]` expande casos).

| Classe | Categoria | Métodos | Objetivo | Qualidade | Observações |
|---|---|---|---|---|---|
| `ValorMonetarioTests` | Unitário | 20 | Parsing/formatação monetária pt-BR (casos válidos, vazios, inválidos, inversão) | Boa | Cobertura de limites (`1,005`, `1.005`, `R$`) |
| `TransacaoServiceTests` | Integração/negócio | 15 | Registro, edição, exclusão, metas, saldo por carteira, rollback | Boa | Valida rollback real com banco (não mock) |
| `CarteiraServiceTests` | Integração/negócio | 7 | Saldos, transferências, validações de saldo inicial | Boa | — |
| `DatabaseTests` | Integração/persistência | 7 | Bootstrap, migration de banco legado 2.x, FTS5, filtros, transação aninhada | Boa | Migration testa schema Java real simulado |
| `PlanejamentoFinanceiroTests` | Unitário/integração | 15 | Cartão, empréstimos, metas (validações, totais, status de vencimento) | Boa | — |
| `ExporterTests` | Unitário | 4 | Formatos CSV (BOM, escapamento) e TXT (resumo) | Boa | — |
| `AuditoriaTests` | Auditoria (fases 3-6) | 14 | Busca degradada, Unicode, volumes (10k), concorrência, corrupção, idempotência | Boa | Criados nesta auditoria; incluem regressões de AUD-001/004 |
| `TestApp` | Suporte | — | Banco SQLite isolado por teste em `%TEMP%` | Adequada | Sem dados reais do usuário |

## Testes que não existem

- Testes de UI automatizados (WPF): não há. Cobertura de UI é manual.
- Testes de instalação/distribuição automatizados: execução manual documentada em TEST_RESULTS.
- Testes de carga além de 10k registros (suíte atual).
- Pipeline de CI (GitHub Actions): inexistente — recomendação em REMAINING_ISSUES.

## Qualidade geral

Nenhum teste superficial detectado: todos usam assertions com valores concretos, banco real isolado (não mocks) e cobrem caminhos de erro. Um teste frágil foi corrigido durante a auditoria (asserção de CSV sensível a CRLF).
