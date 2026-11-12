# 05 — Auditoria de banco de dados e persistência

## Esquema (7 tabelas + FTS5)

`meta`, `carteira` (2 seeds), `transacao` (FKs meta/carteira, índices), `compra_cartao_credito`, `emprestimo`, `transferencia_carteira` (FKs CASCADE), `transacao_fts` (conteúdo externo + 3 triggers).

## Migrações (executadas em Initialize, dentro de transação)

- Java 2.0 → colunas `comentario/categoria/carteira_id` (testado com banco legado sintético).
- Rodada 4 → `compra_cartao_credito.parcelas_pagas/carteira_id/transacao_id` e `emprestimo.carteira_id/transacao_id` (`AdicionarColunaSeFaltar`, default que preserva legados). Testado implicitamente pela suíte (TestApp cria esquema novo) — **migração de banco 2.x com essas tabelas não tem teste dedicado** (lacuna, ver 10).

## Constatações

| ID | Constatação | Severidade |
|---|---|---|
| SF-043 | `compra_cartao_credito.carteira_id`, `emprestimo.carteira_id` e os dois `transacao_id` **sem FOREIGN KEY** — inconsistente com `transacao.carteira_id` (que tem FK). Integridade hoje garantida apenas pela aplicação | LOW |
| SF-044 | `transacao_id` sem FK: um vínculo órfão (ex.: transação apagada manualmente) faria `EstornarMovimentacao` virar no-op e, na edição, geraria movimentação **adicional** — exige manipulação manual do banco | LOW |
| — | WAL + `busy_timeout 5000` + `secure_delete` + backup diário online (F1) — confirmados no schema/pragmas | (positivo) |
| — | `EmTransacao` reentrante com rollback — operações compostas atômicas | (positivo) |

## Persistência e recuperação (executados)

- Banco corrompido → `SqliteException` identificável; vazio → bootstrap completo; inicialização idempotente; FTS sincronizado por triggers (purge no delete testado); kill -9 ×5 + reabertura → WAL recupera (rodada 3).
- Backup diário com retenção 7 (testado) — snapshot consistente com conexão aberta.

## Concorrência

Duas instâncias simultâneas: WAL + busy_timeout sustentam (testado rodada 3). Corrida verificar-à-gravar entre duas instâncias segue INFO (SF-024, decisão documentada — agora mitigada pelo mutex F3, que impede duas instâncias).
