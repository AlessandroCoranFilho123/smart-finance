# Resultados de execução — auditoria 10/11/2026

Ambiente: Windows 10 Pro 19045 x64, .NET SDK 11.0.100-rc.2 (alvo net10.0-windows), Git Bash + PowerShell.
Banco do usuário **nunca usado em testes destrutivos** — apenas cópia em `%TEMP%` e bancos isolados.

## Suíte automatizada

| Execução | Escopo | Resultado |
|---|---|---|
| Suíte original (pré-auditoria) | 65 testes | PASSOU (65/65) |
| Testes de auditoria — 1ª rodada (investigação) | 14 testes | FALHOU (3) → **confirmou AUD-001**, AUD-002 classificada, 1 asserção de teste corrigida |
| Suíte completa pós-correções | 79 testes | **PASSOU (79/79)**, ~35s |
| Suíte completa + ManutencaoTests (rodada 2) | 84 testes | **PASSOU (84/84)** |
| Suíte GUI FlaUI (rodada 2) | 6 testes | **PASSOU (6/6) via CI** — runner windows-latest |
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
| T11 | Interação real com UI — suíte FlaUI (6 testes: navegação nas 5 visões, criação de transação persistida, Escape, tema) | GUI/Interação | **PASSOU** — 6/6 duas execuções locais consecutivas + CI | `tests/SmartFinance.GuiTests`; rodada 2; encontrou AUD-008 |
| T12 | Soak 30 min com o exe final (janela minimizada, banco isolado) | Estabilidade | **PASSOU** | 21:28–21:58: processo vivo o tempo todo, WorkingSet/Private estáveis (213/171MB entre o minuto 11 e o 30 — sem crescimento), 0 exceções no log |
| T13 | Instalador: instalar → executar instalado → desinstalar | Distribuição | **PASSOU** | Inno Setup, /VERYSILENT em dir isolado; dados %APPDATA% preservados |
| T14 | Suspensão/hibernação/troca de usuário | Estabilidade | NÃO EXECUTADO | requer controle do SO indisponível nesta sessão |

## Verificações estáticas

| Verificação | Resultado |
|---|---|
| Superfície de rede / processos (`HttpClient`, `Process.Start`, sockets) | PASSOU — nenhuma ocorrência |
| SQL com entrada do usuário concatenada | PASSOU — toda entrada via `SqliteParameter`; única interpolação usa constante de classe |
| Segredos hardcoded | PASSOU — nenhum |
| Código morto / imports não usados | PASSOU — 0 warnings de build; sem classes órfãs |

## Rodada 3 — auditoria externa (11/11/2026)

| Execução | Escopo | Resultado |
|---|---|---|
| Suíte unitária pós-triagem externa | **115 testes** (87 métodos) | **PASSOU (115/115)** |
| Suíte GUI (interação) | 6 testes | **PASSOU (6/6)** |
| Suíte GUI (capturas de tela, 2 testes) | 10 imagens das telas nos dois temas + cenário SF-009 | **PASSOU (2/2)** |
| CI GitHub Actions | build + 115 + 6 no runner Windows | **PASSOU** (`c70268c`) e (`e25dc75`) |
| Verificação visual das capturas | SF-010/017/025 + layout geral | **PASSOU** — revelou e corrigiu o SF-038 |
| Cenário físico SF-009 | banco corrompido → mensagem → `WaitForExit` | **PASSOU** |

## Rodada 4 — recursos F1–F5 (11/11/2026)

| Execução | Escopo | Resultado |
|---|---|---|
| Suíte unitária pós-rodada 4 | **132 testes** (94 métodos) | **PASSOU (132/132)** |
| Suíte GUI completa (serializada) | **10 testes** (6 interação + 2 captura + 2 acessibilidade) | **PASSOU (10/10)** |
| Smoke do binário 3.3.0 | startup ok com banco isolado | **PASSOU** |

## Rodada 5 — atualização automática + campos de data (11/11/2026)

| Execução | Escopo | Resultado |
|---|---|---|
| Suíte unitária pós-F6 | **136 testes** (F6: hash das notas, integridade SHA-256) | **PASSOU (136/136)** |
| Suíte GUI completa (serializada) | **12 testes** | **PASSOU (12/12)** |
| Campos de data da tela de transações | De/Até filtram de fato (3→2→0→3) | **PASSOU** — dúvida do usuário resolvida: nada faltava |
| Upgrade silencioso local | instalar → atualizar sobre existente → reabertura automática | **PASSOU** |
