# 12 — Auditoria de documentação (GLM)

| Documento | Estado | Divergências |
|---|---|---|
| `README.md` | Desatualizado (SF-042 herdado) | Faltam: parcelas pagas (F1), movimentações de cartão/empréstimo (F2), instância única (F3), acessibilidade (F4), auto-atualização completa (F6 — hoje só "abre página de download") |
| `docs/ARQUITETURA.md` | Desatualizado (SF-042 herdado) | Faltam: D-10/D-11 (movimentações vinculadas + pagas), F3 (mutex), F6 (`AtualizacaoService` + relaunch pelo instalador), divisão `BrushAccent`/`BrushAccentTexto` |
| `docs/SECURITY.md` | Desatualizado (SF-042 herdado) | Faltam: rede do F6 (API+download com verificação de hash), mutex |
| `docs/auditoria/*` | Atualizado | Rodadas 1-4 + externa versionada |
| Comandos de build/publish no README | Corretos (flags na linha de comando) | — |
| `docs/audit/glm/*` | Criado nesta auditoria | — |

Nenhuma funcionalidade documentada que não exista; nenhum comando quebrado encontrado (comandos executados nesta auditoria).
