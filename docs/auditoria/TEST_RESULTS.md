# Resultados de execução — auditoria 10/11/2026

Ambiente: Windows 10 Pro 19045 x64, .NET SDK 11.0.100-rc.2 (alvo net10.0-windows), Git Bash + PowerShell.
Banco do usuário **nunca usado em testes destrutivos** — apenas cópia em `%TEMP%` e bancos isolados.

## Suíte automatizada

| Execução | Escopo | Resultado |
|---|---|---|
| Suíte original (pré-auditoria) | 65 testes | PASSOU (65/65) |
| Testes de auditoria — 1ª rodada (investigação) | 14 testes | FALHOU (3) → **confirmou AUD-001**, AUD-002 classificada, 1 asserção de teste corrigida |
| Suíte completa pós-correções | 79 testes | **PASSOU (79/79)**, ~35s |
| Build de release | `dotnet publish -c Release` | PASSOU (0 erros, 0 warnings) |
| Pacotes vulneráveis | `dotnet list package --vulnerable --include-transitive` | PASSOU (nenhum) |

## Testes de runtime (exe publicado `dist/SmartFinance.exe`)

| # | Teste | Categoria | Resultado | Evidência |
|---|---|---|---|---|
| T1 | Cold start com extração nativa | Desempenho | PASSOU | 1.678ms até janela (`erro.log`: `startup ok`) |
| T2 | Warm start ×5 | Desempenho | PASSOU | 69–114ms |
| T3 | RAM em repouso | Desempenho | PASSOU | 134MB WorkingSet / 84MB Private |
| T4 | Bootstrap em banco novo (instalação limpa) | Distribuição | PASSOU | db+WAL criados, `startup ok` |
| T5 | Persistência de tema em `settings.json` | Distribuição | NÃO EXECUTADO | caminho de settings usa `%APPDATA%` real (não isolável via variável de ambiente — `SHGetKnownFolderPath`); risco de tocar dado do usuário |
| T6 | Caminho com espaços + Unicode (`sf audit éé目录`) | Windows | PASSOU | banco criado, `startup ok` |
| T6b | Caminho longo (~280 caracteres) | Windows | PASSOU | `startup ok`, 0 exceções |
| T7 | 2 instâncias simultâneas no mesmo banco | Concorrência | PASSOU | ambas vivas, 0 exceções (WAL + busy_timeout 5000) |
| T8 | Kill forçado ×5 + reinício no mesmo banco | Recuperação | PASSOU | recuperação por WAL, `startup ok`, 0 exceções |
| T9 | Estabilidade de memória (30s ocioso pós ciclos) | Estabilidade | PASSOU | 132MB → 132MB (sem crescimento) |
| T10 | Exe final com cópia do banco real do usuário | Funcional | PASSOU | `startup ok`, 0 exceções (mesmo banco que causava o crash pré-1a8bf16) |
| T11 | Interação real com UI (cliques, teclado, foco, DPI, temas visuais) | GUI/Interação | NÃO EXECUTADO | sem automação de desktop disponível no ambiente; coberto por revisão estrutural de XAML (ver AUDIT_REPORT §GUI) |
| T12 | Soak > 5 min, suspensão/hibernação, troca de usuário | Estabilidade | NÃO EXECUTADO | tempo/controle do SO indisponível nesta sessão |

## Verificações estáticas

| Verificação | Resultado |
|---|---|
| Superfície de rede / processos (`HttpClient`, `Process.Start`, sockets) | PASSOU — nenhuma ocorrência |
| SQL com entrada do usuário concatenada | PASSOU — toda entrada via `SqliteParameter`; única interpolação usa constante de classe |
| Segredos hardcoded | PASSOU — nenhum |
| Código morto / imports não usados | PASSOU — 0 warnings de build; sem classes órfãs |
