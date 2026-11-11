# Relatório de regressões — 10/11/2026

| Problema original | Correção | Teste de proteção | Resultado pós-correção |
|---|---|---|---|
| Crash de startup 100% (AUD pré-auditoria, commit 1a8bf16) | Guarda no handler de navegação durante parsing do XAML | Smoke com log `startup ok` + execução com cópia do banco real (T10) | PASSOU — 0 exceções em 12+ execuções |
| AUD-001 crash na busca de símbolos | Ignorar filtro textual quando consulta FTS vazia | `AuditoriaTests.Busca_SomenteSimbolos_NaoDeveQuebrar` | PASSOU |
| AUD-004 estado parcial no diálogo de carteiras | Gravações compostas em `Database.EmTransacao` | `AuditoriaTests.AtualizacaoComposta_FalhaNaSegundaEtapa_RollbackTotal` | PASSOU |
| AUD-003 DatePicker en-US | `OverrideMetadata` de Language para cultura atual | Suíte completa + correção estrutural; visual NÃO EXECUTADO | PASSOU (suíte) |
| AUD-005 Escape não fechava diálogos | `IsCancel="True"` nos 6 diálogos | Compilação + revisão; interação real NÃO EXECUTADA | PASSOU (build) |
| Asserção frágil de CSV (CRLF) no próprio teste | Asserção reescrita | `ExporterTests` + `AuditoriaTests.Csv_ComentarioComQuebraDeLinha_Escapado` | PASSOU |
| AUD-008 tela de Transações quebrada (ItemsSource + item XAML) | Itens removidos do XAML; combo populado via código com "Todas" pré-selecionado | Suíte GUI `GuiTests` (6/6 ×2) — navegação + criação de transação na tela corrigida | PASSOU |
| Publish quebrando exe de build comum (PublishSingleFile no csproj) | Propriedades de publish movidas para a linha de comando (README) | Build Release + execução do binário de teste | PASSOU |

Regressão completa executada após cada correção: **79/79** no estado final. Nenhuma funcionalidade existente regrediu (comportamentos validados nos 65 testes originais permanecem verdes).

## Rodada 3 — auditoria externa estática (triagem com execução)

| Problema original (SF) | Correção | Teste de proteção | Resultado |
|---|---|---|---|
| SF-001 tela Transações quebrada (idêntico ao AUD-008) | Já corrigido na rodada 2 | `GuiTests` navega pela tela | PASSOU |
| SF-002 combos ilegíveis (`ToString()`) | `DisplayMemberPath="Nome"` nos 4 combos | Captura `06`/`07` mostram "Banco" e nomes legíveis | PASSOU |
| SF-003 tipo Entrada/Saída pode ficar vazio | `Unchecked` religa o botão (2 diálogos) | Revisão de fluxo + defesa no serviço (SF-029) | PASSOU (build) |
| SF-004 editar meta usa instância obsoleta | Serviço recarrega a meta do banco | T-004/b/c (meta conferida **no banco**) | PASSOU |
| SF-005 excluir/reduzir entrada deixa negativo | Bloqueio com rollback (D-04) | T-005a/b/c | PASSOU |
| SF-006 excluir aplicação pós-resgate cria dinheiro | Reversão integral ou bloqueio (D-05) | T-006/b + total preservado | PASSOU |
| SF-007 excluir meta destrói acumulado | Bloqueio com valor acumulado (D-01) | T-007 + teste antigo reescrito | PASSOU |
| SF-008 totais do mês incluem meta | SQL exclui categorias de meta (D-02) | T-008/T-008b | PASSOU |
| SF-011 Guid maiúsculo × legado minúsculo | `Guid.ToString()` em todos os parâmetros | T-011 (banco legado `aaaaaaaa-…`) | PASSOU |
| SF-013/030 parsing ambíguo grava errado | Parsing estrito + reformat ao sair (D-03) | `ValorMonetarioTests` atualizados | PASSOU |
| SF-014 overflow | Teto verificado em `ParseCentavos` | Teste `99999…` | PASSOU |
| SF-029 Tipo × Categoria incoerente | Validação no domínio + serviço | T-003 (recusa) + coerentes (aceita) | PASSOU |
| SF-016 data estática do cartão | Calculada a cada `Atualizar()` | Revisão | PASSOU |
| SF-018/019/021/023/025/028/031 | Barra escura na principal, cultura fixa, catch, pré-seleção de meta, TextoNav, item sob cursor + Enter, janela mínima | Build + revisão; visual nas capturas | PASSOU |
| **SF-038 (novo, crítico)**: app não abria com metas cadastradas | `Mode=OneWay` no binding do progresso | `CapturaTests` com banco populado (reproduziu antes da correção) | PASSOU |
| **SF-017 confirmado**: data invisível | Template customizado removido | Captura `06` mostra `11/11/2026` | PASSOU |
