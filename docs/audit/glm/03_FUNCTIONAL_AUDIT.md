# 03 — Auditoria funcional (GLM)

Inventário de 10 funcionalidades investigadas por leitura de código + testes executados + capturas. Estado no commit `722ceeb`.

| Funcionalidade | Fluxo rastreado (UI → serviço → banco → UI) | Estado | Evidência |
|---|---|---|---|
| Transações CRUD | Dialog → TransacaoService → transacao → lista/indicadores | **OK** | T-003/004/005 + `DataTransacoesTests` (filtro real) |
| Filtros De/Até + busca | Campos → AplicarFiltros → FTS5/SQL | **OK** | `DataTransacoesTests` local; **falha no CI** (SF-052) |
| Metas + aporte/resgate | MetaDialog → TransacaoService (clamp/reversão) | **OK** | T-004/b/c, T-007 + capturas |
| Carteiras + transferência | CarteirasDialog/TransferenciaDialog → CarteiraService | **OK** | CarteiraServiceTests |
| Cartão parcelado (F1) | CartaoDialog → CartaoCreditoService | **OK** | F1_* (amortização, faixa de pagas) |
| Cartão/Empréstimos → carteiras (F2) | Serviços geram/estornam movimentação vinculada | **OK** | F2_* (criar/excluir/editar/legado) |
| Empréstimos | EmprestimoDialog → EmprestimoService | **OK** | unit + capturas |
| Dashboard | DashboardView (leitura) | **OK** | capturas 2 temas |
| Exportação CSV/TXT | SaveFileDialog → exporters | **OK** | ExporterTests |
| Temas + atualização (F6) | TemaManager / AtualizacaoService + MainWindow | **OK** | GuiTests tema + unit hash; **CI GUI falhou em outro teste** (SF-052) |

## Estados investigados (executados)

Vazio (banco novo), com dados (fixture populada), legado 2.x (migração sintética), corrompido (erro identificável + encerramento — SF-009), datas inválidas (parsing estrito D-03), valores extremos (teto long), repetição (idempotência), segunda instância (mutex F3), reinicialização (WAL).

## Não confirmado (limitação)

Fluxo de falha do instalador de atualização (UAC recusado) — leitura de fluxo apenas (SF-041).
