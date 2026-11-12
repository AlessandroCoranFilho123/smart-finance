# 02 — Auditoria de arquitetura (GLM)

## Arquitetura real (confirmada por leitura)

`Views → Services → Data` com composition root em `App.OnStartup`; conexão SQLite única; `Database.EmTransacao` reentrante para operações compostas; movimentações de cartão/empréstimo vinculadas por `transacao_id` (D-10); paletas WCAG AA (D-12); `TemaManager` + `JanelaNativa`; `BackupService` (diário) e `AtualizacaoService` (F6) na inicialização.

## Avaliação

| Aspecto | Avaliação |
|---|---|
| Camadas | Coerentes — nenhuma consulta SQL em Views; nenhum MessageBox em Services |
| Acoplamento | Views consomem `Servicos` (agregador) — pragmático; substituível por DI se o porte crescer (não recomendado agora) |
| Coesão | Cada serviço uma responsabilidade; `MainWindow` acumula shell+tema+update (~280 linhas) — aceitável, monitorar |
| Ciclos | Nenhum (Views→Services→Data) |
| Testabilidade | Services 100% testáveis com banco real isolado; GUI via UIA |

## Problemas arquiteturais (referência)

- SF-040 (herdado, HIGH): competência das movimentações — a correção deve manter `EmTransacao` e os testes T-004*/F2_*.
- SF-044 (herdado, LOW): `transacao_id` sem FK — vínculo órfão na edição duplica movimentação (ver `12_ISSUES_CATALOG.md` da auditoria executável).
- Nenhum problema arquitetural **novo** nesta auditoria: os fluxos F1/F2/F6 seguem o padrão estabelecido (serviço + EmTransacao + vínculo).
