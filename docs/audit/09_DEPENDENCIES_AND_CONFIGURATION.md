# 09 — Auditoria de dependências e configurações

## Dependências (todas usadas; nenhuma vulnerável — verificado nesta auditoria)

| Pacote | Versão | Uso | Risco |
|---|---|---|---|
| Microsoft.Data.Sqlite | 10.0.0 | SQLite + FTS5 | Baixo |
| SQLitePCLRaw.bundle_e_sqlite3 | 3.0.5 (explícito) | Nativo — corrige CVE do 2.1.11 transitivo; combinação validada empiricamente (FTS5/WAL/backup/148 testes) | Baixo |
| xunit / runner.visualstudio / Microsoft.NET.Test.Sdk | 2.9.3 / 3.1.5 / 18.0.0 | Testes | Baixo |
| FlaUI.UIA3 | 5.0.0 | Automação UIA | Baixo |

## Configurações de build/publicação

- `net10.0-windows`; props de publish **fora do csproj** (linha de comando, documentada no README) — corrige o bug de runtimeconfig de builds comuns (SF rodada 3).
- `app.manifest`: PerMonitorV2 + asInvoker + supportedOS Win10/11.
- CI (`ci.yml`): build Release + unit + GUI no runner Windows interativo — **verde** em `dc4e9a0`; commit auditado `4810844` seguia **em fila** durante a auditoria (limitação registrada).
- `.gitignore`: cobre DBs/bin/dist/segredos; sem binários versionados (162 arquivos conferidos).

## Constatações

| ID | Constatação | Severidade |
|---|---|---|
| — | `SatelliteResourceLanguages=en` reduz satélites WPF | (positivo) |
| — | SDK 11 RC compilando alvo LTS 10 — validado por todas as rodadas | (positivo) |
| SF-049 | `ContrasteTests` localiza paletas por caminho relativo ao repositório (frágil se a árvore de build mudar) | INFO |
| SF-050 | `settings.json`/`atualizacao.json` sem versionamento de schema (campos opcionais tolerados pelo parser) | INFO |
