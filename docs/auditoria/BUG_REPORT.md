# Relatório de bugs — auditoria 10/11/2026

## AUD-001 — Busca com apenas símbolos quebra a aplicação — **ALTO — CORRIGIDO**

- **Categoria:** Funcional/persistência (FTS5)
- **Reprodutibilidade:** 100%
- **Passos:** abrir Transações → digitar `!!!` na busca.
- **Causa raiz:** `TransacaoRepository.CriarConsultaFts("!!!")` retorna string vazia (nenhum termo alfanumérico) e o código enviava `transacao_fts MATCH ''` → `SqliteException: fts5: syntax error near ""`. Bug herdado da versão Java.
- **Evidência:** `tests/SmartFinance.Tests/AuditoriaTests.Busca_SomenteSimbolos_NaoDeveQuebrar` falhava com stack completo (TransacaoRepository.cs:126).
- **Correção:** filtro textual ignorado quando a consulta FTS gerada é vazia (`TransacaoRepository.ListarPorFiltro`).
- **Validação:** teste passa na suíte; regressão completa 79/79.

## AUD-002 — Busca FTS não encontra infixo em texto CJK — **BAIXO — DOCUMENTADO**

- **Categoria:** Localização/FTS
- **Descrição:** o tokenizer `unicode61` trata runs CJK contíguos como um token único; `中文` não encontra `说中文` (prefixo pela esquerda encontra: `说中`).
- **Classificação:** limitação conhecida do SQLite (idêntica na versão Java). Corrigir exigiria tokenizer `trigram`. Registrado em REMAINING_ISSUES.
- **Teste:** `AuditoriaTests.Transacao_ComUnicodeEEmoji_SobreviveAoRoundtrip` documenta o comportamento.

## AUD-003 — DatePicker em formato en-US — **MÉDIO — CORRIGIDO**

- **Categoria:** Localização/GUI
- **Descrição:** WPF usa `en-US` como cultura padrão de elementos; datas do `DatePicker` aparecem/parseiam como `M/d/yyyy` em vez de `dd/MM/yyyy`, inconsistente com o resto da interface (pt-BR).
- **Causa raiz:** padrão documentado do WPF (`FrameworkElement.Language` → en-US).
- **Correção:** `FrameworkElement.LanguageProperty.OverrideMetadata` para a cultura atual em `App.OnStartup` (corrige todos os elementos de uma vez).
- **Validação:** compila, suíte verde. **Verificação visual: NÃO EXECUTADA** (sem automação de UI).

## AUD-004 — Diálogo de carteiras podia aplicar saldo inicial pela metade — **MÉDIO — CORRIGIDO**

- **Categoria:** Integridade de dados
- **Descrição:** `CarteirasDialog` gravava Banco e depois Dinheiro físico em transações separadas; se a segunda validação falhasse, a primeira permanecia aplicada (estado parcial).
- **Correção:** as duas gravações agora rodam dentro de `Database.EmTransacao` (rollback total na falha).
- **Validação:** `AuditoriaTests.AtualizacaoComposta_FalhaNaSegundaEtapa_RollbackTotal`.

## AUD-005 — Escape não fechava diálogos — **BAIXO — CORRIGIDO**

- **Categoria:** Interação/usabilidade
- **Correção:** `IsCancel="True"` nos botões Cancelar dos 6 diálogos (padrão Windows).

## AUD-006 — Colar texto burla a máscara do campo de valor — **BAIXO — COMPORTAMENTO DOCUMENTADO**

- **Descrição:** `PreviewTextInput` não intercepta colar; texto inválido colado permanece no campo até `Salvar`, que exibe "Valor inválido" e bloqueia a gravação (não há corrupção). Comportamento aceitável; melhoria possível via validação em `TextChanged`.

## AUD-007 — Caminho de `settings.json` não isolável para testes — **INFORMATIVO — TESTABILIDADE**

- `TemaManager` resolve o caminho via `Environment.GetFolderPath` (registro do Windows); a variável de ambiente `APPDATA` é ignorada, impedindo isolar preferências em testes de runtime (o banco é isolável via `SMARTFINANCE_DB_PATH`).

## Pré-auditoria — crash de startup em toda execução — **CRÍTICO — CORRIGIDO (commit 1a8bf16)**

- `IsChecked="True"` no XAML disparava navegação antes de `AreaConteudo` existir → `NullReferenceException` em 100% das inicializações. Corrigido com guarda no handler; detectado pelo usuário na Release 3.0.0, reproduzido com stack via `erro.log` e corrigido antes desta auditoria.
