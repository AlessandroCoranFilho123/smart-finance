# Relatório de regressões — 10/11/2026

| Problema original | Correção | Teste de proteção | Resultado pós-correção |
|---|---|---|---|
| Crash de startup 100% (AUD pré-auditoria, commit 1a8bf16) | Guarda no handler de navegação durante parsing do XAML | Smoke com log `startup ok` + execução com cópia do banco real (T10) | PASSOU — 0 exceções em 12+ execuções |
| AUD-001 crash na busca de símbolos | Ignorar filtro textual quando consulta FTS vazia | `AuditoriaTests.Busca_SomenteSimbolos_NaoDeveQuebrar` | PASSOU |
| AUD-004 estado parcial no diálogo de carteiras | Gravações compostas em `Database.EmTransacao` | `AuditoriaTests.AtualizacaoComposta_FalhaNaSegundaEtapa_RollbackTotal` | PASSOU |
| AUD-003 DatePicker en-US | `OverrideMetadata` de Language para cultura atual | Suíte completa + correção estrutural; visual NÃO EXECUTADO | PASSOU (suíte) |
| AUD-005 Escape não fechava diálogos | `IsCancel="True"` nos 6 diálogos | Compilação + revisão; interação real NÃO EXECUTADA | PASSOU (build) |
| Asserção frágil de CSV (CRLF) no próprio teste | Asserção reescrita | `ExporterTests` + `AuditoriaTests.Csv_ComentarioComQuebraDeLinha_Escapado` | PASSOU |

Regressão completa executada após cada correção: **79/79** no estado final. Nenhuma funcionalidade existente regrediu (comportamentos validados nos 65 testes originais permanecem verdes).
