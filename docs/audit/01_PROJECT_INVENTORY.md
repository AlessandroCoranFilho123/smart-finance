# 01 — Inventário do projeto

**Commit:** `4810844` · 162 arquivos versionados · .NET 10 (WPF) · SQLite (Microsoft.Data.Sqlite 10.0.0 + SQLitePCLRaw bundle 3.0.5) · xUnit · Inno Setup (instalador) · GitHub Actions (CI)

## Estrutura

| Caminho | Finalidade |
|---|---|
| `src/SmartFinance/SmartFinance.csproj` | App WPF `net10.0-windows`; publish-only props fora do csproj (linha de comando) |
| `src/SmartFinance/Data/` | `Database` (conexão única, migrations, FTS5) + 5 repositórios |
| `src/SmartFinance/Services/` | Regras: Transacao, Carteira, Meta, CartaoCredito, Emprestimo, Backup, Atualizacao, ValorMonetario, Formatacao, exportadores |
| `src/SmartFinance/Views/` | MainWindow, 5 visões, 6 diálogos, `Linhas.cs`, `InteracaoLista.cs`, `MascaraValor.cs`, `Icones.cs`, `JanelaNativa.cs` |
| `src/SmartFinance/Themes/` | Paletas clara/escura, estilos, templates |
| `tests/SmartFinance.Tests/` | 136 execuções (9 classes) |
| `tests/SmartFinance.GuiTests/` | 12 execuções UIA (interação, captura, acessibilidade, campos de data) |
| `setup/SmartFinance.iss` | Instalador Inno (reabre o app também em instalação silenciosa) |
| `.github/workflows/ci.yml` | CI: build Release + suítes no `windows-latest` |
| `docs/auditoria/` | Auditorias anteriores (internas + externa) e `imagens/` |

## Artefatos locais NÃO versionados (presentes no disco, cobertos por `.gitignore`)

`financas.db`, `identifier.sqlite` (resquício Java — **candidato a exclusão local**), `dist/`, `installer/`, `target/` (resquício Maven — **candidato a exclusão local**), `app.iml` removido na rodada 1.

## Dependências (todas em uso)

`Microsoft.Data.Sqlite` + `SQLitePCLRaw.bundle_e_sqlite3` (3.0.5 — CVE do 2.1.11 evitado), `xunit`/`xunit.runner.visualstudio`, `FlaUI.UIA3` (GUI). Sem vulneráveis (`dotnet list package --vulnerable` vazio).

## Órfãos/duplicados

- **Nenhum arquivo versionado órfão** — varredura confirmou que todos os `.cs` compõem o app/testes e todos os `.png` de `Assets/icons/**` são referenciados por `Icones.cs` ou XAML.
- `docs/audit/` (esta) vs `docs/auditoria/` (histórico): pastas distintas por propósito — manter.

## Observações

- Zero `TODO`/`FIXME` no código (grep).
- `.gitignore` cobre DBs, `bin/obj/dist`, segredos — sem binários versionados.
