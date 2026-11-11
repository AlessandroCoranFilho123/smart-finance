# Triagem da auditoria externa estática (SF-001…SF-037)

**Data:** 11/11/2026 · **Entrada:** auditoria estática de terceiros sobre a 3.0.0 (arquivos `00–22`) + revisão independente.
**Diferença fundamental de método:** esta triagem foi feita **com execução** (build, 115 testes unitários, 6 testes GUI, smoke do binário publicado e sondagem do banco 2.x real), corrigindo a limitação central da auditoria externa (nada executado).
**Decisões de produto:** `DECISOES_PRODUTO.md` (D-01…D-09).

| ID | Achado (resumo) | Veredito da revisão externa | Estado nesta triagem | Evidência |
|---|---|---|---|---|
| SF-001 | Tela Transações lança exceção ao abrir | VALID · CRITICAL | **CORRIGIDO** (rodada 2, antes da auditoria externa chegar) — mesmo defeito que o AUD-008 desta base | Suíte GUI navega pela tela; `GuiTests` 6/6 ×2 |
| SF-002 | Combos exibem `ToString()` bruto | VALID · HIGH | **CORRIGIDO** — `DisplayMemberPath="Nome"` nos 4 combos | `TransacaoDialog.xaml`, `TransferenciaDialog.xaml` |
| SF-003 | Tipo Entrada/Saída pode ficar vazio | VALID · HIGH | **CORRIGIDO** — `Unchecked` religa o botão nos 2 diálogos + defesa no serviço (SF-029) | Revisão de fluxo; T-003 |
| SF-004 | Editar meta usa instância obsoleta | VALID · HIGH | **CORRIGIDO** — serviço recarrega a meta do banco antes de aplicar/resgatar | T-004, T-004b, T-004c (meta verificada **no banco**) |
| SF-005 | Exclusão/edição de entrada deixa saldo negativo | PARTIALLY · MEDIUM | **CORRIGIDO** (D-04: bloquear) | T-005a/b/c com rollback verificado |
| SF-006 | Excluir aplicação pós-resgate cria dinheiro | VALID · HIGH | **CORRIGIDO** (D-05: reversão integral ou bloqueio) | T-006 (bloqueio + total preservado), T-006b (caminho legítimo) |
| SF-007 | Excluir meta destrói acumulado | VALID · MEDIUM | **CORRIGIDO** (D-01: bloquear com valor acumulado) | T-007; teste antigo reescrito |
| SF-008 | Totais do mês incluem meta | PARTIALLY · LOW→ | **CORRIGIDO** (D-02: SQL exclui `Adicionar/RetirarMeta`) | T-008, T-008b |
| SF-009 | Exceção no bootstrap → processo zumbi | VALID · MEDIUM | **CORRIGIDO** — `try/catch` + log + mensagem com caminho do banco + `Shutdown(1)` | Build; cenário de processo encerrado: verificação manual NÃO EXECUTADA |
| SF-010 | URIs relativas em XAML de subpasta | INCONCLUSIVE | **CORRIGIDO preventivamente** — `/Assets/...` em diálogos e templates | Compilação; carregamento de Resource falha em silêncio (sem exceção) — defeito era ícone em branco |
| SF-011 | Guid gravado em maiúsculas × Java minúsculas | INCONCLUSIVE | **CONFIRMADO E CORRIGIDO** — sondagem empírica: a lib grava `Guid` em TEXT **maiúsculo**; agora todos os parâmetros usam `Guid.ToString()` (minúsculas, formato Java) | T-011 (banco legado com `aaaaaaaa-…`: editar/aplicar/excluir funcionam) |
| SF-012 | FTS/triggers 2.x divergentes | INCONCLUSIVE · LOW | **REFUTADO com evidência** — sondagem do banco 2.x real: `sqlite_master` idêntico ao DDL atual | Saída da sondagem (docs desta pasta) |
| SF-013 | Parsing ambíguo grava valor errado | PARTIALLY · MEDIUM | **CORRIGIDO** (D-03: parsing estrito + reformatar ao sair em todos os diálogos) | `ValorMonetarioTests` (`1,234.56`, `1.2.3,4`, `1,005`, `0.500` recusados) |
| SF-014 | Overflow → exceção não tratada | VALID · LOW | **CORRIGIDO** — teto verificado em `ParseCentavos` | Teste `99999999999999999999` |
| SF-015a | Parcela: arredondamento e teste com nome errado | PARTIALLY · LOW | **DECISÃO D-06** — regra mantida; teste renomeado (`ValorParcela_UsaArredondamentoPadrao`) | Renomeação no teste |
| SF-015b | "Em aberto" não amortiza | PARTIALLY · LOW | **DECISÃO D-07** — modelagem de parcelas pagas é feature nova | REMAINING_ISSUES |
| SF-016 | `Hoje` estático congela a data | VALID · LOW | **CORRIGIDO** — calculado a cada `Atualizar()` | `CartoesView.xaml.cs` |
| SF-017 | `DatePickerTextBox` sem `PART_ContentHost` | INCONCLUSIVE | **CONFIRMADO VISUALMENTE E CORRIGIDO** — captura mostrou a data invisível; o template customizado quebrava a renderização e foi removido (template padrão do WPF) | captura `06-dialogo-nova-transacao.png`: data `11/11/2026` visível |
| SF-018 | Janela principal sem barra escura | VALID · LOW | **CORRIGIDO** — `MainWindow` aplica e reaplica (liga/desliga) no `TemaAlterado` | `MainWindow.xaml.cs`, `JanelaNativa.cs` |
| SF-019 | DatePicker usa cultura do SO | VALID · LOW | **CORRIGIDO** — `pt-BR` fixado (independente do Windows) | `App.xaml.cs` |
| SF-020 | CSV injection + catch faltando | PARTIALLY · LOW | **CORRIGIDO** (a, d) — prefixo `'` e `UnauthorizedAccessException`; (b, c) mantidos por **D-08** | Teste de injeção |
| SF-021 | `TemaManager.Salvar` catch estreito | VALID · LOW | **CORRIGIDO** — captura `UnauthorizedAccessException` também | `TemaManager.cs` |
| SF-022 | Editar muda `rowid` (ordem) | PARTIALLY · LOW | **DECISÃO** — manter delete+insert (cosmético; FTS permanece sincronizado) | Registrada aqui |
| SF-023 | Meta não pré-selecionada na edição | VALID · LOW | **CORRIGIDO** — pré-seleção por `MetaId`; clamp mantido (**D-09**) | `TransacaoDialog.xaml.cs` |
| SF-024 | Sem `BEGIN IMMEDIATE`; transferência sem edição | PARTIALLY · INFO | **DECISÃO** — documentado (uso pessoal; corrida exige 2 instâncias) | REMAINING_ISSUES |
| SF-025 | `TextBlock` implícito anula cor selecionada | PARTIALLY · LOW | **CORRIGIDO** — estilo `TextoNav` rebinda ao `Foreground` do RadioButton | `Global.xaml`, `MainWindow.xaml` |
| SF-026 | Docs com afirmações incorretas | VALID · INFO | **CORRIGIDO** — contagens reais (68 métodos/84 casos + 6 GUI), `NoWarn CS8602` **removido** (build segue 0 warnings), código morto removido (`TryParseData`, `AbrirDialogTransacao`, `FonteSaldo`, usings), `MetaService` mantido como API de domínio exercitada por testes; `Formatacao` barra de título agora é verdade após SF-018 | Build 0/0; grep |
| SF-027 | Combinação de pacotes; `NoWarn` | PARTIALLY · INFO | **REFUTADO/RESOLVIDO** — combinação `Microsoft.Data.Sqlite 10 + bundle 3.0.5` validada empiricamente (FTS5, WAL, backup, 115 testes); sem pacote vulnerável; `NoWarn` removido | `dotnet list package --vulnerable` vazio; suíte |
| SF-028 | Duplo clique no vazio abre selecionado; sem Enter | VALID · INFO | **CORRIGIDO** — item sob o cursor + Enter em todas as listas | `InteracaoLista.cs` + handlers nas 5 visões |
| SF-029 | Serviço não valida Tipo × Categoria | VALID · LOW | **CORRIGIDO** — `CategoriaExtensoes.CategoriasValidas/EhValidaPara` no domínio + validação em `Registrar/Atualizar` | T-003 (recusa) + testes de coerência (aceita) |
| SF-030 | `0.500` → R$ 500 | (novo) | **CORRIGIDO** — milhar exige grupos estritos sem zero à esquerda | `ValorMonetarioTests` (`0.500` recusado) |
| SF-031 | Tamanho mínimo de janela | (novo) | **CORRIGIDO** — `MinWidth 960 / MinHeight 600` | `MainWindow.xaml` |
| SF-032 | Backup manual ignora WAL | (novo) | **CORRIGIDO (docs)** — SECURITY orientar app embutido (API online) ou copiar `db + -wal + -shm` com app fechado | `docs/SECURITY.md` |
| SF-033 | Interpolação em `ComandoSelecaoTransacoes` | (novo) | **REFUTADO** — interpolados apenas constantes de classe; comentário de segurança adicionado | `RepositoryBase.cs` |
| SF-034 | Uma linha inválida derruba a inicialização | (novo) | **MITIGADO** — falhas de bootstrap (inclui ctor da janela) caem no catch de SF-009: mensagem + `Shutdown(1)` | `App.xaml.cs` |
| SF-035 | Compatibilidade de enums com banco Java | (novo) | **VERIFICADO E CUBERTO** — enums idênticos; teste com banco legado contendo `RetirarMeta`/`AdicionarMeta`/`Alimentacao` | Teste Sf035 |
| SF-036 | Cartão/Empréstimos não afetam carteiras | (novo) | **DECISÃO** — igual à versão Java; feature futura | REMAINING_ISSUES |
| SF-037 | Testes de moeda dependem de ICU | (novo) | **REFUTADO** — testes e app usam cultura explícita `pt-BR` (`Formatacao.CulturaBr`) | Código dos testes |

## Não executado nesta rodada

- Confirmação **visual** de SF-010/017/025 (renderização de ícones, texto do DatePicker, contraste) — automação UIA valida existência/comportamento, não estética.
- Encerramento do processo no cenário SF-009 (corromper banco e rodar o app) — o caminho de código (`Shutdown(1)`) está coberto por revisão; executar manualmente com **cópia** corrompida.
- Windows 11, leitores de tela, suspensão/hibernação (já em REMAINING_ISSUES).

## Novo achado da verificação visual (SF-038) — CRITICAL, corrigido

**App não abria para quem tivesse meta cadastrada.** `ProgressBar.Value` é TwoWay por padrão no WPF; o template das metas fazia `{Binding Progresso}` contra uma propriedade somente leitura de `LinhaMeta` → `InvalidOperationException` no bootstrap. Invisível até então porque os testes GUI rodavam com banco vazio (template nunca instanciado) — **as capturas de tela com dados populados o revelaram** (a primeira imagem capturada era a própria caixa de erro).

Correção: `Value="{Binding Progresso, Mode=OneWay}"` em `Themes/Templates.xaml`. Verificação: capturas `01-dashboard-claro.png` e `08-dashboard-escuro.png` mostram o dashboard com metas, progresso e percentuais renderizando nos dois temas.

## Verificação visual executada (capturas em `docs/auditoria/imagens/`)

| Captura | Verifica | Resultado |
|---|---|---|
| `01`/`08` dashboard claro/escuro | SF-025 contraste do item ativo, layout geral, metas com progresso | **OK** — texto branco legível sobre accent nos dois temas |
| `02` transações | SF-001 (tela abre), ícones de categoria (SF-010) | **OK** — ícones renderizam |
| `04` cartão | ícone na linha de compra | **OK** |
| `06` diálogo nova transação | SF-010 ícone, SF-017 data visível, SF-002 carteira legível, SF-003 tipo marcado | **OK** após correções |
| `07` diálogo transferência | SF-002 combos legíveis | **OK** |
| `10` banco corrompido | SF-009 mensagem + processo encerra | **OK** — assertado (`WaitForExit`) |
