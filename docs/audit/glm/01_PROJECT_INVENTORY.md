# 01 — Inventário do projeto (auditoria GLM)

**Commit:** `722ceeb` · 162 arquivos versionados · WPF `net10.0-windows` · SQLite · xUnit · FlaUI · Inno Setup · GitHub Actions

## Projetos

| Projeto | Conteúdo |
|---|---|
| `src/SmartFinance` | App WPF (Data 6, Models 6, Services 11, Views 15+6 diálogos, Themes 4, Assets) |
| `tests/SmartFinance.Tests` | 136 execuções / 9 classes |
| `tests/SmartFinance.GuiTests` | 12 execuções / 4 classes (interação 6, captura 2, acessibilidade 2, datas 2) |
| `setup/` | Inno Setup (reabre o app também em instalação silenciosa) |
| `.github/workflows/ci.yml` | CI Windows (build Release + unit + GUI) |
| `docs/audit/` | Auditoria executável (rodada anterior, SF-040..051) |

## Funcionalidades (10)

Transações CRUD+filtros+busca FTS · Carteiras+transferências · Metas (3 faixas) · Cartão parcelado com pagas (F1) · Empréstimos com movimentação (F2) · Dashboard · Exportação CSV/TXT · Temas WCAG AA · Instância única (F3) · Auto-atualização (F6).

## Componentes principais (leitura confirmada)

- `Data/Database.cs` — conexão única, migrations idempotentes (incl. `AdicionarColunaSeFaltar`), FTS5 + triggers.
- `Services/TransacaoService.cs` — núcleo financeiro (delete+insert com reversão de meta, `EmTransacao` reentrante).
- `Services/CartaoCreditoService.cs` / `EmprestimoService.cs` — movimentações vinculadas (D-10) com estorno.
- `Services/BackupService.cs` — snapshot diário online (retenção 7).
- `Services/AtualizacaoService.cs` — checagem diária + download + verificação SHA-256 + instalação silenciosa (F6).
- `Views/MainWindow.xaml.cs` — shell + tema + fluxo de atualização (~280 linhas).

## Arquivos suspeitos/órfãos (verificados)

- Nenhum órfão versionado (todos os recursos/XAML referenciados; grep de usos).
- Locais não versionados: `financas.db`, `identifier.sqlite` (resquício Java), `dist/`, `installer/`, `target/` — candidatos a exclusão local, cobertos por `.gitignore`.
