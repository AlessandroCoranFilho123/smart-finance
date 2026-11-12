# 09 — Dependências e configurações (GLM)

| Pacote | Versão | Uso confirmado | Vulneráveis |
|---|---|---|---|
| Microsoft.Data.Sqlite | 10.0.0 | Persistência (FTS5/WAL/backup online) | 0 (verificado nesta auditoria) |
| SQLitePCLRaw.bundle_e_sqlite3 | 3.0.5 (explícito) | Nativo — corrige CVE do 2.1.11 | 0 |
| xunit + runner + Test.Sdk | 2.9.3/3.1.5/18.0.0 | Testes | 0 |
| FlaUI.UIA3 | 5.0.0 | Automação UIA | 0 |

## Configurações

- Build Debug e Release: **0 warnings, 0 erros** (executados nesta auditoria).
- Publish por linha de comando (props fora do csproj — decisão da rodada 3 que corrigiu o runtimeconfig).
- CI: build Release + unit + GUI serializado; estado no commit auditado: unit **verde**, GUI **vermelho** (SF-052) — ver catálogo.
- `app.manifest`: PerMonitorV2 + asInvoker + Win10/11.
- `xunit.runner.json` serializa coleções GUI (3 fixtures não podem rodar em paralelo).
- Sem configurações duplicadas/contraditórias encontradas; sem pacotes sem uso.
