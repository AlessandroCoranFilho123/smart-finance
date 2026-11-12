# 11 — Auditoria de distribuição e ciclo de vida (GLM)

## Cadeia atual

`dotnet publish` (flags no README) → exe único self-contained R2R (143MB) + instalador Inno (45MB) → GitHub Releases (com SHA-256 nas notas) → F6 baixa/verifica/instala silenciosamente e **reabre o aplicativo** (entrada `[Run]` incondicional no .iss).

## Verificado (executado nesta auditoria e em rodadas anteriores)

| Cenário | Resultado |
|---|---|
| Instalação silenciosa em diretório custom | OK |
| Execução pós-instalação | `startup ok` |
| Upgrade sobre instalação existente (arquivos substituídos) | OK |
| **Reabertura automática após instalação silenciosa** | **OK — PID capturado** |
| Desinstalação preservando `%APPDATA%\SmartFinance` | OK |
| Portátil (exe único, sem instalador) | OK |

## Constatações

| ID | Constatação | Severidade |
|---|---|---|
| SF-053 | **Auto-atualização usa sempre o instalador**: usuários do exe portátil migram para a instalação em `{autopf}` e a cópia portátil original fica obsoleta. Comportamento coerente com "atualize na minha máquina", mas é **mudança de modelo de distribuição** para quem usava o portátil — requer decisão/documentação | MEDIUM |
| SF-054 | Falha na instalação (UAC recusado) → app encerrado sem reabertura (SF-041 herdado — referência) | MEDIUM (herdado) |
| — | Desinstalação preserva dados do usuário (decisão correta, documentada no .iss) | (positivo) |
| — | SHA-256 dos dois artefatos publicados nas notas | (positivo) |

## Ciclo de vida

- Atualização: F6 cobre o caso instalado. Backup diário protege os dados antes de qualquer atualização.
- Rollback de atualização: não existe mecanismo — versão anterior permanece disponível para download manual (RELEASES). Aceitável para o porte; registrado.
