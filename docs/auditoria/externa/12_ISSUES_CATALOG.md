# 12 — Catálogo de achados (SF-001 … SF-029)

**Base analisada:** conteúdo do repositório entregue na sessão (código, XAML, testes, docs), versão 3.0.0.
**Método:** leitura estática integral. **Nada foi compilado nem executado** (ver `15_AUDIT_LIMITATIONS.md`).
**Linhas:** os arquivos foram recebidos sem numeração; a localização é por arquivo + membro. O GLM deve localizar pelo membro citado.
**Confiança:** "Alto" = comprovado por leitura do código + regra conhecida do framework, sem execução. Nenhum achado está "Confirmado" (reproduzido), por honestidade.

## Índice

| ID | Sev. | Categoria | Título | Confiança |
|---|---|---|---|---|
| SF-001 | CRITICAL | Funcional/UI | Tela Transações lança exceção ao abrir (`ItemsSource` com itens no XAML) | Alto |
| SF-002 | HIGH | Interface | ComboBoxes de Carteira e Meta exibem `ToString()` bruto | Alto |
| SF-003 | HIGH | Funcional | Botões Entrada/Saída podem ficar ambos desmarcados → tipo trocado em silêncio | Alto |
| SF-004 | HIGH | Financeiro | Editar transação de meta usa `Meta` obsoleta → saldo da meta inflado | Alto |
| SF-005 | HIGH | Financeiro | Excluir/editar Entrada deixa carteira com saldo negativo | Alto |
| SF-006 | HIGH | Financeiro | Excluir aplicação em meta após resgate cria dinheiro (clamp de `Retirar`) | Alto |
| SF-007 | MEDIUM | Financeiro | Excluir meta destrói o valor acumulado | Alto |
| SF-008 | MEDIUM | Financeiro | Receitas/Despesas do mês incluem movimentos de meta e empréstimo | Alto |
| SF-009 | MEDIUM | Funcional | Exceção em `OnStartup` deixa processo invisível vivo | Alto |
| SF-010 | MEDIUM | Interface | URIs relativas de imagem em XAML de subpastas | Médio |
| SF-011 | MEDIUM | Banco | GUID gravado em MAIÚSCULAS vs IDs minúsculos do banco Java | Médio |
| SF-012 | MEDIUM | Banco | FTS/triggers preexistentes da versão 2.x não são reconciliados | Baixo |
| SF-013 | MEDIUM | Financeiro | `ParseCentavos` aceita entradas ambíguas e devolve valor errado | Alto |
| SF-014 | LOW | Funcional | `OverflowException` com valores enormes; diálogos só tratam `ArgumentException` | Alto |
| SF-015 | LOW | Financeiro | Cartão: parcela com resto, `double`, total em aberto nunca amortiza | Alto |
| SF-016 | LOW | Funcional | `CartoesView.Hoje` estático congela a data | Alto |
| SF-017 | LOW | Interface | Template de `DatePickerTextBox` sem `PART_ContentHost` | Médio |
| SF-018 | LOW | Interface | Janela principal nunca recebe barra de título escura | Alto |
| SF-019 | LOW | Interface | `DatePicker` usa cultura do SO, não pt-BR | Alto |
| SF-020 | LOW | Funcional | Exportação: injeção de fórmula, inconsistências CSV×TXT, exceção não tratada | Alto |
| SF-021 | LOW | Funcional | `TemaManager.Salvar` só captura `IOException` | Alto |
| SF-022 | LOW | Banco | Editar = delete+insert muda `rowid` (ordem e FTS externo) | Alto |
| SF-023 | LOW | Interface | Edição de transação de meta não pré-seleciona a meta; clamp silencioso | Alto |
| SF-024 | LOW | Banco | `Transferir`/`Registrar` sem transação; transferência sem editar/excluir | Alto |
| SF-025 | LOW | Interface | `TextBlock` implícito com `Foreground` fixo anula cor de itens selecionados | Médio |
| SF-026 | INFO | Documentação | Auditoria anterior e docs com afirmações incorretas | Alto |
| SF-027 | INFO | Dependências | Combinação `Microsoft.Data.Sqlite 10.0.0` + `SQLitePCLRaw 3.0.5`; `NoWarn CS8602` | Médio |
| SF-028 | INFO | Interface | Duplo clique abre item previamente selecionado; sem acesso por teclado | Alto |
| SF-029 | LOW | Financeiro | Serviço não valida coerência Tipo × Categoria | Alto |

---

## SF-001 — Tela Transações lança exceção ao abrir
**Severidade:** CRITICAL · **Categoria:** Funcional/UI · **Confiança:** Alto (estático)
**Localização:** `Views/TransacoesView.xaml` (`ComboCategoria` contém `<ComboBoxItem Content="Todas" />`) + `Views/TransacoesView.xaml.cs` construtor (`ComboCategoria.ItemsSource = ...`).
**Descrição/Evidência:** o XAML coloca um `ComboBoxItem` direto em `Items`. O construtor então atribui `ItemsSource`. O WPF lança `InvalidOperationException: Items collection must be empty before using ItemsSource`. O código posterior (`CategoriaSelecionada`, `LimparFiltros` com `SelectedIndex = 0`) mostra que a intenção era "Todas" como primeiro item da lista.
**Reproduzir (não executado):** abrir o app → clicar em "Transações" (ou "Ver todas →" no dashboard).
**Atual:** exceção em `AoNavegar` → `DispatcherUnhandledException` mostra "erro inesperado"; a navegação não ocorre.
**Esperado:** tela abre com "Todas" selecionado.
**Impacto:** busca, filtros, exportação CSV/TXT e a lista completa ficam inacessíveis. Nenhum teste cobre UI, então a suíte 79/79 não detecta.
**Causa provável:** mistura de itens estáticos com `ItemsSource`.
**Correção:** escolher um modelo único: ou lista inteira em `ItemsSource` com item "Todas" (`ItemCombo` com `Valor` nulo) inserido na posição 0, ou itens estáticos e sem `ItemsSource`. Ajustar `CategoriaSelecionada` ao modelo escolhido.
**Arquivos:** `TransacoesView.xaml(.cs)`, `DashboardView.xaml.cs` (`IrParaTransacoes`), `MainWindow.xaml.cs`.
**Regressão:** filtro por categoria, `LimparFiltros`, exportação.
**Validação:** teste manual/UI: abrir Transações; filtrar por categoria; limpar. Idealmente teste STA instanciando `TransacoesView`.

## SF-002 — ComboBoxes de Carteira e Meta exibem `ToString()` bruto
**Severidade:** HIGH · **Categoria:** Interface · **Confiança:** Alto (estático)
**Localização:** `TransacaoDialog.xaml` (`ComboCarteira`, `ComboMeta`), `TransferenciaDialog.xaml` (`ComboOrigem`, `ComboDestino`); `Models/Meta.cs`, `Models/Carteira.cs`; `Themes/Templates.xaml` (sem DataTemplate para `Meta`/`Carteira`).
**Descrição/Evidência:** os combos recebem `List<Carteira>`/`List<Meta>` sem `DisplayMemberPath`/`ItemTemplate`. `Carteira` é `record` (ToString gera `Carteira { Id = ..., Nome = Banco, SaldoInicialCentavos = 0 }`); `Meta` é classe sem `ToString` (mostra `SmartFinance.Models.Meta`). `ItemCombo` tem `ToString` — mostra que o padrão era conhecido.
**Impacto:** o usuário não distingue metas entre si ao aplicar/resgatar dinheiro → lançamento na meta errada.
**Correção:** `DisplayMemberPath="Nome"` nos quatro combos (mais simples) ou DataTemplates. Não sobrescrever `ToString` do record de domínio.
**Arquivos:** os dois XAMLs de diálogo (+ Templates.xaml se optar por template).
**Regressão:** `ComboBox` implícito (placeholder por `Tag`, `SelectionBoxItem`).
**Validação:** abrir "Nova transação" com categoria "Aplicar em meta" e "Transferir": nomes legíveis.

## SF-003 — Botões Entrada/Saída podem ficar ambos desmarcados
**Severidade:** HIGH · **Categoria:** Funcional · **Confiança:** Alto (estático)
**Localização:** `TransacaoDialog.xaml(.cs)` (`AoMudarTipo`, `TipoAtual`), `EmprestimoDialog.xaml(.cs)` (idem).
**Descrição/Evidência:** apenas `Checked` está ligado. Clicar em um `ToggleButton` já marcado o desmarca sem disparar `Checked`; o ramo "nenhum marcado" de `AoMudarTipo` só roda dentro de um `Checked`, logo é inalcançável. `TipoAtual` usa `BotaoEntrada.IsChecked == true ? Entrada : Saida`.
**Reproduzir (não executado):** Nova transação → Entrada já marcado → clicar em "Entrada" de novo → escolher "Salário" → Salvar.
**Atual:** visual sem seleção; grava `Saida` com categoria Salário (se houver saldo).
**Esperado:** um tipo sempre selecionado, ou erro explícito.
**Impacto:** receita lançada como despesa; saldo e totais incorretos. No empréstimo, "Emprestei" vira "Pedi emprestado".
**Correção:** tratar `Unchecked` (religar o botão) ou trocar por `RadioButton` com mesmo estilo. Remover o ramo morto.
**Regressão:** carregamento em modo edição (`_carregando`), lista de categorias por tipo.
**Validação:** UI: clicar repetidamente nos botões; tipo nunca fica vazio. Ver SF-029 para defesa no serviço.

## SF-004 — Editar transação de meta usa `Meta` obsoleta
**Severidade:** HIGH · **Categoria:** Financeiro · **Confiança:** Alto (estático)
**Localização:** `Services/TransacaoService.cs` — `Atualizar`, `RegistrarAplicacaoMeta`, `RegistrarResgateMeta`, `ExcluirTransacaoInterna`.
**Descrição/Evidência:** `Atualizar` recebe o objeto `meta` carregado pela UI **antes** da edição. Dentro da transação, `ExcluirTransacaoInterna` recarrega outra instância da meta do banco, reverte e grava. Em seguida `RegistrarAplicacaoMeta(meta, …)` usa a instância antiga (ainda com o valor original), faz `meta.Adicionar` e grava `AtualCentavos` obsoleto + novo, **sobrescrevendo a reversão**.
**Exemplo numérico:** meta=500 (inclui aplicação de 100). Editar aplicação de 100 para 100: o banco reverte para 400; a instância antiga (500) soma 100 → grava 600. Esperado 500. A meta ganha 100 indevidos a cada edição.
**Reproduzir (não executado):** aplicar 100 em meta → editar a transação (reselecionar a meta, valor 100) → conferir meta.
**Cobertura:** `Atualizar_DeComumParaMeta` não passa por reversão; nenhum teste edita uma transação que já era de meta.
**Correção:** recarregar a meta do repositório (por `meta.Id`) **depois** de `ExcluirTransacaoInterna` e antes de aplicar/resgatar; ignorar o estado em memória vindo da UI. Vale também para `Registrar` (usar sempre o estado do banco).
**Arquivos:** `TransacaoService.cs`, `TransacaoDialog.xaml.cs`, `TransacaoServiceTests.cs`.
**Regressão:** clamp ao restante, rollback, exclusão com reversão.
**Validação:** teste novo (ver `14_TEST_PLAN_FOR_GLM.md` T-004) com bancos reais; comparar `meta.atual` do banco, não da instância.

## SF-005 — Excluir/editar Entrada deixa carteira negativa
**Severidade:** HIGH · **Categoria:** Financeiro · **Confiança:** Alto (estático)
**Localização:** `TransacaoService.ExcluirTransacao`, `Atualizar` (`ValidarSaldoSuficienteParaSaida` só valida `Saida`).
**Evidência/Exemplo:** saldo 0; Salário +1000 (Banco); Aluguel −900; excluir Salário → saldo −900. Também: reduzir o valor do Salário ou trocá-lo de carteira.
**Contraste:** `AtualizarSaldoInicial` impede saldo negativo e `Registrar` impede saída sem saldo — o invariante existe, mas não é aplicado aqui.
**Impacto:** saldo negativo que o sistema declara impossível; telas de saldo mostram valor negativo sem aviso.
**Correção:** após remover/alterar, validar que o saldo da(s) carteira(s) afetada(s) não fique negativo (dentro da transação, com rollback). Definir a regra para resgate de meta (também é entrada).
**Validação:** testes T-005a/b (excluir entrada, reduzir entrada, trocar carteira de entrada).

## SF-006 — Excluir aplicação em meta após resgate cria dinheiro
**Severidade:** HIGH · **Categoria:** Financeiro · **Confiança:** Alto (estático)
**Localização:** `TransacaoService.ExcluirTransacaoInterna`; `Meta.Retirar` (clamp ao `AtualCentavos`).
**Exemplo:** carteira 100 → aplicar 100 (carteira 0, meta 100) → resgatar 100 (carteira 100, meta 0) → excluir a aplicação original: a saída some (carteira 200) e `meta.Retirar(100)` retira 0 (clamp). Total passou de 100 para 200.
**Correção:** decidir regra: bloquear exclusão/edição quando a reversão não puder ser integral (`retirado != valor`), com mensagem clara; ou exigir excluir na ordem inversa.
**Validação:** T-006. Simétrico: excluir um resgate cujo valor já foi reaplicado.

## SF-007 — Excluir meta destrói o valor acumulado
**Severidade:** MEDIUM · **Categoria:** Financeiro · **Confiança:** Alto
**Localização:** `MetaDialog.xaml.cs` (`Excluir`), `MetaService.DeletarMeta`, FK `transacao.meta_id ON DELETE SET NULL`; teste `Metas_Excluir_RevinculaTransacoesParaNulo` documenta o comportamento.
**Descrição:** o dinheiro aplicado saiu da carteira; ao excluir a meta, nada retorna à carteira e as transações ficam sem vínculo (e sem possibilidade de resgate).
**Impacto:** dinheiro "some" do saldo total. A mensagem do diálogo só fala que as transações são mantidas.
**Correção:** ao excluir meta com `AtualCentavos > 0`, gerar resgate automático (Entrada) na carteira escolhida, ou avisar explicitamente e pedir confirmação do valor perdido. Decisão de produto — registrar.

## SF-008 — Receitas/Despesas do mês incluem metas e empréstimos
**Severidade:** MEDIUM · **Categoria:** Financeiro · **Confiança:** Alto
**Localização:** `TransacaoRepository.CalcularTotalPorTipoEMes` (soma por `tipo` apenas); `DashboardView.Atualizar`.
**Impacto:** guardar 500 em meta aparece como despesa de 500; resgatar aparece como receita. Indicadores do dashboard não representam consumo/renda.
**Correção:** excluir `AdicionarMeta`/`RetirarMeta` (decidir também para `Emprestimo`) do cálculo; documentar a regra. Teste existente `ReceitasEDespesasDoMes…` não cobre.

## SF-009 — Exceção em `OnStartup` deixa processo invisível vivo
**Severidade:** MEDIUM · **Categoria:** Funcional · **Confiança:** Alto
**Localização:** `App.xaml.cs` `OnStartup` (handler `DispatcherUnhandledException` com `Handled = true`) + `App.xaml` `ShutdownMode="OnMainWindowClose"`.
**Descrição:** se `db.Initialize()` falhar (banco corrompido, travado, sem permissão) ou `TemaManager` lançar, o handler exibe mensagem e marca como tratado; nenhuma janela principal existe e o modo de encerramento nunca dispara. O processo fica vivo e invisível (e pode segurar o banco).
**Evidência:** teste `ArquivoBanco_Corrompido…` valida só o tipo da exceção, não o comportamento do app. É o mesmo cenário do crash de startup pré-auditoria.
**Correção:** envolver o bootstrap em `try/catch` que registra no log, mostra mensagem com o caminho do banco e chama `Shutdown(1)`.

## SF-010 — URIs relativas de imagem em XAML de subpastas
**Severidade:** MEDIUM · **Categoria:** Interface · **Confiança:** Médio (depende de verificação visual)
**Localização:** `Views/Dialogs/*.xaml` (`Source="Assets/icons/…"`), `Themes/Templates.xaml` (`Assets/icons/credito.png`, `emprestimo.png`).
**Descrição:** em WPF, URI relativa em XAML compilado é resolvida relativamente ao arquivo XAML. Em `Views/Dialogs/` e `Themes/` ela aponta para `/Views/Dialogs/Assets/...` e `/Themes/Assets/...`, inexistentes. `MainWindow.xaml` (raiz) funciona; `Icones.cs` usa pack URI completo e funciona.
**Impacto provável:** ícone ausente ou `XamlParseException` ao abrir diálogo/linhas de cartão e empréstimo.
**Verificação rápida:** abrir "+ Nova transação" e a aba Cartão com uma compra.
**Correção:** usar `/Assets/icons/...` (barra inicial) ou pack URI completo.

## SF-011 — GUID em maiúsculas vs IDs minúsculos do banco Java
**Severidade:** MEDIUM · **Categoria:** Banco · **Confiança:** Médio
**Localização:** `Data/*Repository.cs` (`AddWithValue("$id", Guid)`), `RepositoryBase.LerTransacao`; `DatabaseTests.Initialize_MigraBancoLegado…`.
**Descrição:** `Microsoft.Data.Sqlite` grava `Guid` como TEXT em maiúsculas (comportamento a confirmar na versão usada). O Java 2.x grava `UUID.toString()` em minúsculas. Comparações `WHERE id = $id` e FKs são case-sensitive em TEXT. Para registros legados com letras a–f, editar/excluir pode não afetar linha alguma, e inserir transação com `meta_id` maiúsculo contra `meta.id` minúsculo violaria a FK.
**Por que passou despercebido:** o teste de migração usa IDs só com dígitos (`1111…`).
**Verificação:** inspecionar IDs reais de uma **cópia** do banco do usuário (`SELECT id FROM meta LIMIT 5`) e testar editar/excluir uma transação legada.
**Correção (se confirmado):** padronizar o formato gravado/consultado (ex.: `COLLATE NOCASE` ou normalização em migração/parâmetro) com teste usando GUID com letras.

## SF-012 — FTS e triggers da versão 2.x não são reconciliados
**Severidade:** MEDIUM · **Categoria:** Banco · **Confiança:** Baixo (hipótese)
**Localização:** `Database.CriarBuscaTextual` (`CREATE … IF NOT EXISTS`).
**Descrição:** o código assume que a versão Java criou FTS com a mesma semântica. Se um banco 2.x já tiver `transacao_fts` com colunas/triggers diferentes, o `IF NOT EXISTS` preserva o esquema antigo e os novos triggers (5 colunas) podem falhar a cada INSERT, ou duplicar indexação se os nomes diferirem. `TEST_COVERAGE.md` lista o cenário como não coberto.
**Verificação:** comparar `sqlite_master` de uma cópia de banco real da 2.x com o esquema esperado.
**Correção:** detectar divergência (colunas do FTS, nomes de triggers) e recriar o índice com `rebuild`, em transação.

## SF-013 — `ParseCentavos` devolve valor errado em entradas ambíguas
**Severidade:** MEDIUM · **Categoria:** Financeiro · **Confiança:** Alto
**Localização:** `Services/ValorMonetario.cs`; diálogos Meta/Cartão/Empréstimo/Transferência/Carteiras (não reformatam ao sair do campo; só `TransacaoDialog` tem `FormatarValorAoSair`).
**Exemplos (por leitura):**
- `1,234.56` → contém vírgula → remove `.` → `1,23456` → `1.23456` → **123 centavos** (esperado 123456).
- `1.2.3,4` → `123.4` → 12.340 centavos.
- `1,005` → 101 centavos (arredonda sub-centavo em silêncio).
**Impacto:** valor financeiro gravado errado sem erro. O filtro `^[0-9.,]*$` permite todas essas formas.
**Correção:** validar padrão estrito (um separador decimal, no máximo 2 casas; milhar só em grupos de 3) e recusar o resto com "Valor inválido"; reformatar ao sair do campo em todos os diálogos. Atualizar `ValorMonetarioTests` (`1,005` hoje é caso "esperado" 101 — decidir).

## SF-014 — `OverflowException` com valores enormes
**Severidade:** LOW · **Confiança:** Alto · **Localização:** `ValorMonetario.ParseCentavos` (`valor * 100m`, cast `(long)`); catch só de `ArgumentException` em `MetaDialog`, `CartaoDialog`, `EmprestimoDialog`, `CarteirasDialog`, `TransferenciaDialog`.
**Impacto:** digitar ~`1000000000000000000` gera exceção não tratada (mensagem genérica via handler global). **Correção:** capturar overflow em `ParseCentavos` e lançar `ArgumentException("Valor inválido")`; definir teto.

## SF-015 — Cartão: parcela com resto e total em aberto que nunca amortiza
**Severidade:** LOW · **Confiança:** Alto · **Localização:** `CompraCartaoCredito.ValorParcelaCentavos`, `CartaoCreditoService.CalcularTotalAbertoCentavos/CalcularTotalParcelasMesCentavos`.
**Descrição:** (a) divisão em `double`; 100,00/3 → 33,33 e 3×33,33 = 99,99 (resto de 1 centavo perdido); o teste `ValorParcela_ArredondaParaCima` tem nome oposto ao que afirma (arredonda para baixo neste caso). (b) não existe conceito de parcela paga: "Total em aberto" é a soma dos totais para sempre; "Parcelas por mês" soma compras já vencidas/encerradas. **Correção:** divisão inteira com resto na última (ou primeira) parcela; decidir modelo de parcelas pagas (produto) ou rotular os números como "valor total cadastrado".

## SF-016 — `CartoesView.Hoje` estático congela a data
**Severidade:** LOW · **Confiança:** Alto · **Localização:** `Views/CartoesView.xaml.cs` (`static readonly DateOnly Hoje`).
**Descrição:** calculado uma vez por processo; `LinhaCompra` usa `DateTime.Today` a cada carga. Após meia-noite com o app aberto, o contador "Vencendo em até 7 dias" diverge das cores das linhas. **Correção:** calcular em `Atualizar()`.

## SF-017 — Template de `DatePickerTextBox` sem `PART_ContentHost`
**Severidade:** LOW (pode ser maior se confirmado) · **Confiança:** Médio · **Localização:** `Themes/Global.xaml` (Style `DatePickerTextBox`).
**Descrição:** o template substitui o padrão por `Border + ContentPresenter`. `TextBox` renderiza texto via `PART_ContentHost`; sem ele o texto da data pode não aparecer. **Verificação:** abrir qualquer diálogo com data. **Correção:** template com `ScrollViewer x:Name="PART_ContentHost"`.

## SF-018 — Janela principal nunca recebe barra de título escura
**Severidade:** LOW · **Confiança:** Alto · **Localização:** `MainWindow.xaml.cs` (sem chamada a `JanelaNativa`); `JanelaNativa.AplicarBarraEscura` (só aplica, nunca desfaz, e só em `SourceInitialized`).
**Descrição:** `ARQUITETURA.md` afirma que a barra acompanha o modo escuro; só os diálogos o fazem. Trocar o tema em execução não atualiza janelas abertas. **Correção:** chamar na `MainWindow`, reaplicar em `TemaAlterado` (inclusive desligar o modo escuro).

## SF-019 — `DatePicker` usa a cultura do SO, não pt-BR
**Severidade:** LOW · **Confiança:** Alto · **Localização:** `App.xaml.cs` (`OverrideMetadata` com `CultureInfo.CurrentCulture`), comentário diz "pt-BR".
**Impacto:** em Windows en-US, DatePicker mostra `M/d/yyyy` enquanto o resto usa `Formatacao.CulturaBr`. A auditoria anterior (AUD-003) declarou resolvido sem esse caveat. **Correção:** usar `Formatacao.CulturaBr.IetfLanguageTag`.

## SF-020 — Exportação
**Severidade:** LOW · **Confiança:** Alto · **Localização:** `TransacaoCsvExporter`, `TransacaoTxtExporter`, `TransacoesView.xaml.cs`.
**Itens:** (a) `Descricao`/`Comentario` iniciando com `=`, `+`, `-`, `@` viram fórmula no Excel (CSV injection); (b) categoria exportada como nome do enum (`Alimentacao`) em vez do rótulo da UI; (c) CSV em ordem crescente, TXT decrescente; (d) só `IOException` é capturada (`UnauthorizedAccessException` cai no handler global). **Correção:** prefixar `'` em células perigosas; padronizar rótulos/ordem; capturar `UnauthorizedAccessException`.

## SF-021 — `TemaManager.Salvar` só captura `IOException`
**Severidade:** LOW · **Confiança:** Alto · **Localização:** `Themes/TemaManager.cs`.
`UnauthorizedAccessException` (pasta somente leitura) não é `IOException` e propaga dentro de `OnStartup` → ver SF-009. **Correção:** capturar também `UnauthorizedAccessException`; não gravar em `Inicializar` se nada mudou.

## SF-022 — Editar = delete+insert muda `rowid`
**Severidade:** LOW · **Confiança:** Alto · **Localização:** `TransacaoService.Atualizar`; `ListarRecentes/ListarTodas` (`ORDER BY data DESC, rowid DESC`); FTS `content_rowid='rowid'`.
**Descrição:** transação editada "pula" para o topo entre as de mesma data. Além disso, tabela com `id TEXT PRIMARY KEY` não tem rowid estável sob `VACUUM`; ferramenta externa que rode `VACUUM` pode desalinhar o FTS de conteúdo externo. **Correção:** `UPDATE` em vez de delete/insert quando a categoria não é de meta; documentar/neutralizar `VACUUM` (ou `rebuild` do FTS).

## SF-023 — Edição de transação de meta não pré-seleciona a meta; clamp silencioso
**Severidade:** LOW · **Confiança:** Alto · **Localização:** `TransacaoDialog.xaml.cs` (`AtualizarVisibilidadeMeta` zera `ComboMeta.SelectedItem`); `RegistrarAplicacaoMeta` (clamp).
O usuário precisa reselecionar a meta ao editar (e é isso que aciona SF-004). Se o valor excede o restante, grava menos sem avisar. **Correção:** preselecionar por `transacao.MetaId`; informar o valor efetivamente aplicado.

## SF-024 — Operações sem transação; transferência sem edição/exclusão
**Severidade:** LOW · **Confiança:** Alto · **Localização:** `CarteiraService.Transferir`, `TransacaoService.Registrar` (caminho comum).
Verificar saldo e inserir são passos separados; com duas instâncias abertas (T7 permite) há corrida. A transação padrão é `DEFERRED`. Transferências não têm tela para corrigir erro. **Correção:** `EmTransacao` + `BEGIN IMMEDIATE` ou instância única; avaliar edição/exclusão de transferência.

## SF-025 — `TextBlock` implícito anula cor de itens selecionados
**Severidade:** LOW · **Confiança:** Médio · **Localização:** `Themes/Global.xaml` (Style `TextBlock` com `Foreground` fixo) + `BotaoNav`/`BotaoAlternar` (trigger muda `Foreground` do pai).
O setter local do `TextBlock` tem precedência sobre herança; o texto do item "Início" (um `TextBlock` explícito) permanece `BrushTextPrimary` sobre fundo de destaque. Contraste ruim no tema claro. **Verificação visual.** **Correção:** remover o `Foreground` do estilo implícito (deixar herdar) e aplicar cor por estilos nomeados.

## SF-026 — Auditoria anterior e docs com afirmações incorretas
**Severidade:** INFO · **Categoria:** Documentação · **Confiança:** Alto
- `docs/auditoria/TEST_INVENTORY.md`: "ValorMonetarioTests — 20 métodos". O arquivo tem 5 métodos (21 execuções).
- `AUDIT_REPORT.md`/`TEST_RESULTS.md`: "0 warnings" — `SmartFinance.csproj` suprime `CS8602` (`NoWarn`), o que mascara avisos de nulabilidade.
- `ARQUITETURA.md`: barra de título "acompanha o modo escuro" (ver SF-018); "sem código morto" — são código sem uso no app: `Formatacao.TryParseData`, `ValorMonetario.FormatarCentavos`, `MainWindow.AbrirDialogTransacao`, `FonteSaldo` em `Global.xaml`, `using System.ComponentModel` em `DashboardView.xaml.cs`, `MetaService.CalcularProgressoPercentual/MetaConcluida/ContarMetasConcluidas/CalcularRestante` (só testes).
- GUI e interação ficaram "NÃO EXECUTADO"; mesmo assim SF-001/002/003/010 são visíveis na leitura do XAML — a "revisão estrutural" não os encontrou.
- Duas pastas de auditoria: `docs/auditoria/` (anterior) e `docs/audit/` (esta).
- `README`: "Windows 10/11" — só Windows 10 testado.
- Nome de teste enganoso: `ValorParcela_ArredondaParaCima` afirma arredondamento para baixo.

## SF-027 — Dependências e build
**Severidade:** INFO · **Confiança:** Médio · **Localização:** `SmartFinance.csproj`.
`Microsoft.Data.Sqlite 10.0.0` é compilado contra a linha 2.1.x do `SQLitePCLRaw`; forçar `bundle_e_sqlite3 3.0.5` é uma combinação que precisa ser validada (inicialização, FTS5, WAL) — a justificativa (CVE do 2.1.11) está em `SECURITY_REPORT.md` e não foi verificada nesta sessão. `NoWarn CS8602` (ver SF-026). `IncludeNativeLibrariesForSelfExtract` é incondicional enquanto o resto é só Release. Alvo `net10.0` com SDK 11 RC. Sem CI.

## SF-028 — Duplo clique e teclado
**Severidade:** INFO/LOW · **Confiança:** Alto · **Localização:** `*View.xaml(.cs)` (`MouseDoubleClick` no `ListBox`).
Duplo clique em área vazia abre o item previamente selecionado; não há ativação por Enter, nem `AutomationProperties` (P10 da auditoria anterior). **Correção:** validar o item sob o cursor; tratar Enter.

## SF-029 — Serviço não valida coerência Tipo × Categoria
**Severidade:** LOW · **Confiança:** Alto · **Localização:** `TransacaoService.Registrar/Atualizar`.
`Registrar(Entrada, Aluguel)` é aceito; para metas o `tipo` do parâmetro é ignorado. A única barreira é a UI (e SF-003 a contorna). **Correção:** validar no serviço usando as mesmas tabelas `CategoriasEntrada/CategoriasSaida` (mover para o domínio).
