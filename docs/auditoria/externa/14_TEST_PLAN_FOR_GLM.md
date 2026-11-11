# 14 — Plano de testes e matriz de rastreabilidade

Convenção dos testes xUnit: usar `TestApp` (banco isolado). Valores em centavos (`100_00` = R$ 100,00). Data fixa `new DateOnly(2026, 11, 10)`.

## A. Smoke manual (executar ANTES e DEPOIS das correções, nos dois temas)

1. Abrir o app → Início carrega. 
2. Clicar em **Transações** → tela abre, combo Categoria mostra "Todas" (SF-001).
3. **+ Nova transação** → ícone do cabeçalho aparece (SF-010); campo Data mostra texto legível (SF-017); combo Carteira mostra "Banco"/"Dinheiro físico" (SF-002).
4. Clicar duas vezes em "Entrada" → o tipo nunca fica sem seleção (SF-003).
5. Categoria "Aplicar em meta" → combo Meta mostra nomes das metas (SF-002).
6. **Transferir** → combos legíveis.
7. Aba **Cartão** com uma compra cadastrada → ícone na linha (SF-010).
8. Item "Início" selecionado na barra lateral → contraste do texto (SF-025); barra de título da janela principal em modo escuro (SF-018).
9. Forçar falha de startup: copiar `financas.db` para `%TEMP%`, corromper a **cópia**, rodar com `SMARTFINANCE_DB_PATH` apontando para ela → mensagem e **processo encerra** (SF-009). Nunca usar o banco real.
10. Em Windows com cultura en-US: DatePicker em `dd/MM/yyyy` (SF-019).

## B. Testes automatizados novos (a escrever pelo GLM)

| ID | Objetivo | Pré-condição | Passos | Esperado | Aprovação |
|---|---|---|---|---|---|
| T-001 | Tela Transações instancia | thread STA, `App.Servicos` configurado | `new TransacoesView()` | sem exceção; categoria selecionada = Todas | passa |
| T-003 | Serviço rejeita Tipo×Categoria incoerente | banco vazio | `Registrar(Entrada, Aluguel, …)` | `ArgumentException` | passa |
| T-004 | Editar aplicação em meta não infla a meta | carteira 1000; meta alvo 500; aplicar 100 | `Atualizar` mesma transação, valor 100, meta obtida **antes** da edição | `meta.atual` no **banco** = 100 | `MetaRepo.BuscarPorId().AtualCentavos == 100` |
| T-004b | Idem com valor maior (100→150) | idem | atualizar para 150 | meta = 150; carteira = 850 | idem |
| T-004c | Resgate editado | meta com 300 resgatada 100 | editar para 50 | meta = 250 | idem |
| T-005a | Excluir entrada usada por saída | Salário 1000; Aluguel 900 | excluir Salário | bloqueado (ArgumentException) e saldo = 100 | rollback íntegro |
| T-005b | Reduzir entrada abaixo do gasto | idem | editar Salário para 500 | bloqueado | idem |
| T-005c | Trocar carteira de entrada | Salário no Banco, gasto no Banco | mover para Dinheiro | bloqueado | idem |
| T-006 | Excluir aplicação após resgate | carteira 100; aplicar 100; resgatar 100 | excluir aplicação | bloqueado (ou regra definida) e soma total constante | total antes == total depois |
| T-007 | Excluir meta com saldo | meta com 200 aplicados | excluir | conforme regra decidida (resgate automático ou bloqueio) | saldo total preservado |
| T-008 | Despesas do mês ignoram meta | saída comum 50 + aplicar 200 | `CalcularDespesasMes` | 50 | igual |
| T-011 | Banco legado com GUID em minúsculas com letras | criar legado com ids `aaaaaaaa-…` | `Initialize`, editar/excluir transação, aplicar em meta | afeta 1 linha; sem erro de FK | passa |
| T-012 | FTS legado com colunas/triggers diferentes | criar `transacao_fts` antigo | `Initialize`; inserir transação | sem erro; busca encontra | passa |
| T-013a | Parsing estrito | — | `1,234.56`, `1.2.3,4`, `1,005` | `ArgumentException` (se esta for a regra decidida) | passa |
| T-013b | Parsing válido | — | `1.234,56`, `10,5`, `10.50` | 123456, 1050, 1050 | passa |
| T-014 | Overflow | — | `ParseCentavos("99999999999999999999")` | `ArgumentException` | passa |
| T-016 | Data do cartão | — | calcular contagem com `Hoje` injetado | usa data atual, não estática | passa |
| T-020 | CSV injeção | comentário `=1+1` | exportar | célula prefixada com `'` | passa |
| T-021 | Salvar tema em pasta somente leitura | — | `Salvar` | sem exceção | passa |
| T-R | Reconciliação | cenário misto (entradas, saídas, metas, transferências, edições, exclusões) | executar sequência | soma dos saldos das carteiras + soma das metas == soma algébrica das entradas/saídas externas | igual em centavos |

## C. Regressão obrigatória
`dotnet test tests/SmartFinance.Tests` — as 79 execuções existentes devem continuar verdes, **exceto** casos cuja regra foi alterada por decisão explícita (`ValorMonetarioTests` linha `1,005`; teste `ValorParcela_ArredondaParaCima`, renomear). Registrar cada exceção.

## D. Matriz de rastreabilidade

| Achado | Arquivos | Correção | Teste | Critério de aprovação |
|---|---|---|---|---|
| SF-001 | `TransacoesView.xaml(.cs)` | modelo único de itens do combo | T-001, A.2 | tela abre; filtro por categoria funciona |
| SF-002 | `TransacaoDialog.xaml`, `TransferenciaDialog.xaml` | `DisplayMemberPath="Nome"` | A.3, A.5, A.6 | nomes legíveis |
| SF-003 | `TransacaoDialog`, `EmprestimoDialog` | tratar `Unchecked`/`RadioButton` | A.4, T-003 | tipo nunca vazio |
| SF-004 | `TransacaoService.cs` | recarregar meta após reversão | T-004/b/c | meta no banco correta |
| SF-005 | `TransacaoService.cs` | validar saldo após remover/editar entrada | T-005a/b/c | bloqueio + rollback |
| SF-006 | `TransacaoService.cs`, `Meta.cs` | reversão integral ou bloqueio | T-006, T-R | soma total constante |
| SF-007 | `MetaDialog.xaml.cs`, `MetaService.cs` | regra decidida | T-007 | sem perda de dinheiro |
| SF-008 | `TransacaoRepository.cs` | excluir categorias de meta | T-008 | 50 |
| SF-009 | `App.xaml.cs` | try/catch + `Shutdown` | A.9 | processo encerra |
| SF-010 | `Views/Dialogs/*.xaml`, `Templates.xaml` | URI com `/` | A.3, A.7 | ícones visíveis |
| SF-011 | repositórios, `Database.cs` | normalizar GUID | T-011 | passa com ids com letras |
| SF-012 | `Database.cs` | reconciliar FTS | T-012 | passa |
| SF-013 | `ValorMonetario.cs`, diálogos | parsing estrito + reformatar ao sair | T-013a/b | entradas ambíguas recusadas |
| SF-014 | `ValorMonetario.cs`, diálogos | capturar overflow | T-014 | `ArgumentException` |
| SF-015 | `CompraCartaoCredito.cs`, `CartaoCreditoService.cs` | divisão inteira com resto; rótulos | teste de soma das parcelas == total | 3 parcelas somam 100,00 |
| SF-016 | `CartoesView.xaml.cs` | calcular em `Atualizar` | T-016 | usa data atual |
| SF-017 | `Global.xaml` | `PART_ContentHost` | A.3 | data legível |
| SF-018 | `MainWindow.xaml.cs`, `JanelaNativa.cs` | aplicar/desfazer ao trocar tema | A.8 | barra acompanha tema |
| SF-019 | `App.xaml.cs` | cultura pt-BR fixa | A.10 | `dd/MM/yyyy` |
| SF-020 | exportadores, `TransacoesView.xaml.cs` | sanitizar; padronizar; catch | T-020 | célula segura |
| SF-021 | `TemaManager.cs` | catch adicional | T-021 | sem exceção |
| SF-022 | `TransacaoService.cs` | `UPDATE` quando possível | teste de ordenação após edição | posição preservada |
| SF-023 | `TransacaoDialog.xaml.cs` | pré-selecionar meta | A (edição de aplicação) | meta selecionada |
| SF-024 | `CarteiraService.cs`, `TransacaoService.cs` | transação + `IMMEDIATE` | teste de duas conexões | sem saldo negativo |
| SF-025 | `Global.xaml` | remover Foreground implícito | A.8 | contraste adequado |
| SF-026 | docs, csproj | corrigir afirmações; avaliar `NoWarn` | revisão | docs coerentes |
| SF-027 | `SmartFinance.csproj` | validar combinação de pacotes | `dotnet test` + smoke + publish | FTS5/WAL funcionam |
| SF-028 | views | validar item sob cursor; Enter | manual | comportamento correto |
| SF-029 | `TransacaoService.cs` | validar Tipo×Categoria | T-003 | exceção |
