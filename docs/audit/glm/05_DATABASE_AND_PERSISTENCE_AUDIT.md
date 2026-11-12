# 05 — Auditoria de banco e persistência (GLM)

## Schema (7 tabelas + FTS5, migrations idempotentes)

Confirmado por leitura de `Database.cs`: FKs em `transacao`/`transferencia_carteira`; **sem FK** em `compra_cartao_credito`/`emprestimo` (SF-043, herdado); migrations `AdicionarColunaSeFaltar` para 2.x e para as colunas de F1/F2.

## CRUD e integridade (executados — 136 unitários verdes)

- Órfãos: exclusão de meta com acumulado bloqueada (D-01); FK SET NULL testada com meta vazia.
- Atualizações parciais: impossíveis — operações compostas em `EmTransacao` (rollback testado).
- Legado 2.x: migração de `transacao` testada com GUIDs minúsculos; **migração de compra/emprestimo (colunas novas) sem teste dedicado** — lacuna NT-2.
- Duplicação: `Inserir` com `Guid.NewGuid()` — sem risco de duplicação por PK.
- Nulos: COALESCE em leituras (categoria/carteira) e parâmetros com DBNull — consistente.

## Concorrência e recuperação (executados em rodadas anteriores, mesma arquitetura)

WAL + busy_timeout; duas instâncias — hoje **impedido pelo mutex F3**; kill -9 ×5 + WAL recupera; backup diário online com retenção 7.

## Corrupção e compatibilidade

Corrompido → `SqliteException` identificável no bootstrap (mensagem + encerramento — SF-009); banco vazio → bootstrap; GUIDs minúsculos legados operáveis (T-011); enums 2.x carregam (Sf035).

## Consultas

Parametrizadas; índices cobrem filtros; FTS5 sem varredura. N+1: `CalcularSaldoCarteiraCentavos` chamado por carteira no dashboard (3 consultas) — volume pessoal, sem gargalo medido.
