# 03 — Auditoria funcional

Inventário de 10 funcionalidades. **Executado:** suítes completas (136+12) no commit auditado + testes UIA que exercitam as telas reais.

| Funcionalidade | Implementação | Validções | Persistência | Testes | Resultado |
|---|---|---|---|---|---|
| Transações CRUD | `TransacaoService` + `TransacoesView`/Dialog | valor>0, saldo por carteira, Tipo×Categoria (SF-029), meta obrigatória | transacao (FTS5 via triggers) | 15 unit + T-003/004/005 + GUI criação | **OK** |
| Metas | `MetaService` + progresso 3 faixas | alvo>0, exclusão bloqueada com acumulado (D-01) | meta (FK SET NULL) | 6 unit + T-007 + capturas | **OK** |
| Carteiras/transferência | `CarteiraService` | saldo ≥0 em toda operação (D-04), carteiras distintas | carteira + transferencia | 6 unit + T-005 | **OK** |
| Cartão parcelado (F1/F2) | `CartaoCreditoService` | valor>0, parcelas>0, pagas∈[0,parcelas] | compra_cartao (+parcelas_pagas/carteira_id/transacao_id migrados) | unit + capturas | **OK** com SF-040 (data) |
| Empréstimos (F2) | `EmprestimoService` | nome/valor | emprestimo (+carteira_id/transacao_id) | unit + capturas | **OK** com SF-040 |
| Dashboard | `DashboardView` | — | leitura | capturas ambos temas | **OK** |
| Busca/filtros | `TransacaoRepository` FTS5 + De/Até | MATCH vazio ignorado | — | DatabaseTests + `DataTransacoesTests` (filtra 3→2→0→3) | **OK** |
| Exportação CSV/TXT | exporters + SaveFileDialog | escapamento, BOM, injeção sanitizada | — | ExporterTests | **OK** |
| Temas claro/escuro | TemaManager + paletas WCAG AA | persistência settings.json | settings.json | capturas ×2 temas + ContrasteTests | **OK** |
| Auto-atualização (F6) | AtualizacaoService + MainWindow | hash publicado, diária, mutex instância única | atualizacao.json | unit (hash/compare/dia) + instalação silenciosa local | **OK** com SF-041 |

## Fluxos alternativos verificados (executados)

- Banco corrompido/vazio/legado → erro identificável ou migration completa (DatabaseTests).
- Segunda instância → mutex + foreground (F3).
- Atualização silenciosa sobre instalação existente → arquivos substituídos + reabertura (testado localmente, rodada 4).
- Campos de data digitados manualmente → filtro real (DataTransacoesTests).

## Fluxos com risco residual

- **F2 na edição**: a movimentação é recriada com data de hoje (SF-040) — histórico e competência mensal distorcem.
- **F6 falha de instalação**: UAC recusado/instalador abortado → app fechado sem reabertura nem explicação (SF-041).
