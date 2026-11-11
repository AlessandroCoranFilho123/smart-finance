# 18 — Revalidação dos 29 achados

Base: releitura estática. **Nenhuma reprodução executada** ⇒ nenhum CONFIRMED. IDs de teste (`T-…`) estão em [`21_TEST_PLAN_REVIEW.md`](21_TEST_PLAN_REVIEW.md). Decisões (`D-…`) em [`22_FINAL_GLM_HANDOFF.md`](22_FINAL_GLM_HANDOFF.md) §3. Catálogo original: [`12_ISSUES_CATALOG.md`](12_ISSUES_CATALOG.md).

## Resumo

| ID | Veredito | Sev. orig → rev. | Confiança | Depende de decisão |
|---|---|---|---|---|
| SF-001 | VALID | CRITICAL → CRITICAL | HIGH | — |
| SF-002 | VALID | HIGH → HIGH | HIGH | — |
| SF-003 | VALID | HIGH → HIGH | HIGH | — |
| SF-004 | VALID | HIGH → HIGH | HIGH | — |
| SF-005 | PARTIALLY VALID | HIGH → **MEDIUM** | HIGH (mecanismo) | D-04 |
| SF-006 | VALID | HIGH → HIGH | HIGH | D-05 (forma da correção) |
| SF-007 | VALID | MEDIUM → MEDIUM | HIGH | D-01 |
| SF-008 | PARTIALLY VALID | MEDIUM → **LOW** | HIGH (fato) | D-02 |
| SF-009 | VALID | MEDIUM → MEDIUM | HIGH | — |
| SF-010 | INCONCLUSIVE | MEDIUM → **condicional** (CRITICAL se exceção; LOW se ícone em branco) | MEDIUM | — |
| SF-011 | INCONCLUSIVE | MEDIUM → condicional (HIGH se IDs legados têm letras) | MEDIUM (mecanismo) / UNVERIFIED (dados) | — |
| SF-012 | INCONCLUSIVE | MEDIUM → MEDIUM | LOW | — |
| SF-013 | PARTIALLY VALID | MEDIUM → MEDIUM | HIGH | D-03 |
| SF-014 | VALID | LOW → LOW | HIGH | — |
| SF-015 | PARTIALLY VALID | LOW → LOW | HIGH (a) | D-06 (b), D-07 |
| SF-016 | VALID | LOW → LOW | HIGH | — |
| SF-017 | INCONCLUSIVE | LOW → **condicional** (MEDIUM/HIGH se data invisível) | MEDIUM | — |
| SF-018 | VALID | LOW → LOW | HIGH | — |
| SF-019 | VALID | LOW → LOW | HIGH | — |
| SF-020 | PARTIALLY VALID | LOW → LOW | HIGH | D-08 (itens b, c) |
| SF-021 | VALID | LOW → LOW | HIGH | — |
| SF-022 | PARTIALLY VALID | LOW → LOW | HIGH (ordem) / LOW (VACUUM) | — |
| SF-023 | VALID | LOW → LOW | HIGH | D-09 (clamp) |
| SF-024 | PARTIALLY VALID | LOW → **INFO** | **MEDIUM** (reclassificada) | — |
| SF-025 | PARTIALLY VALID | LOW → LOW | MEDIUM | — |
| SF-026 | VALID | INFO → INFO | HIGH | — |
| SF-027 | PARTIALLY VALID | INFO → INFO | MEDIUM | — |
| SF-028 | VALID | INFO → INFO | HIGH | — |
| SF-029 | VALID | LOW → LOW | HIGH | — |

Totais: VALID 16 · PARTIALLY VALID 9 · INCONCLUSIVE 4 · INVALID 0.

---

## SF-001
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · CRITICAL · HIGH |
| Causa técnica | `ComboBoxItem` estático em `Items` + `ItemsSource` no construtor ⇒ `InvalidOperationException` |
| Evidência | `TransacoesView.xaml` (`ComboCategoria`), `TransacoesView.xaml.cs` ctor; sem `Items.Clear()` |
| Problema na original | Correto. Faltou: radio "Transações" fica marcado com conteúdo do Dashboard após a exceção |
| Impacto real | Busca, filtros, histórico completo e exportação inacessíveis; handler global mostra "erro inesperado" |
| Correção | Modelo único: lista completa em `ItemsSource` com item "Todas" (valor nulo) na posição 0, ou só itens estáticos; ajustar `CategoriaSelecionada` e `LimparFiltros` |
| Teste | Manual A.2 (obrigatório). Automático só com STA (ver `21` T-001) |
| Pendências | Executar o app uma vez |

## SF-002
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · HIGH · HIGH |
| Causa técnica | Sem `DisplayMemberPath`/`ItemTemplate`; `ToString()` de record/classe |
| Evidência | `TransacaoDialog.xaml` (`ComboCarteira`, `ComboMeta`), `TransferenciaDialog.xaml`; `Templates.xaml` só tem `Linha*` |
| Problema na original | Correto, mas misturou estético (carteira) e operacional (meta) |
| Impacto real | Meta: inoperável (todos os itens iguais). Carteira: texto verboso com o nome dentro |
| Correção | `DisplayMemberPath="Nome"` nos 4 combos; não alterar `ToString` do domínio |
| Teste | Manual A.3/A.5/A.6; opcional: verificação de que `ComboMeta` mostra N nomes distintos |
| Pendências | Confirmar visualmente |

## SF-003
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · HIGH · HIGH |
| Causa técnica | `ToggleButton` alterna; só `Checked` tratado; ramo "nenhum marcado" inalcançável |
| Evidência | `TransacaoDialog.xaml.cs` `AoMudarTipo`/`TipoAtual`; `EmprestimoDialog.xaml.cs` idem; `Global.xaml` `BotaoAlternar` |
| Problema na original | Correto |
| Impacto real | Persiste `tipo='Saida'` com categoria de Entrada (ex.: Salário); lista de categorias não atualiza. Empréstimo: "Emprestei"→"Pedi emprestado" |
| Correção | `Unchecked` religa o botão ou `RadioButton` com o mesmo visual; **mais** defesa no serviço (SF-029) |
| Teste | Manual A.4 (cliques repetidos); unitário: SF-029 |
| Pendências | Confirmar; verificar também que "Saída" repetido não gera efeito |

## SF-004
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · HIGH · HIGH |
| Causa técnica | Instância `Meta` obsoleta da UI sobrescreve a reversão feita no banco |
| Evidência | `TransacaoService.Atualizar` → `ExcluirTransacaoInterna` (instância nova) → `RegistrarAplicacaoMeta(meta,…)` (instância antiga) → `metaRepository.Atualizar(meta)` |
| Problema na original | Mecanismo correto; **exemplo numérico errado** (E1) — corrigido. Só ocorre com a **mesma** meta; vale também para resgate |
| Impacto real | Ex.: alvo 1000, atual 100 (aplicação de 100); editar para 100 ⇒ banco 0 → grava 200 (esperado 100). Resgate: atual 200, resgate 100 editado para 50 ⇒ grava 150 (esperado 250). Viola I2 |
| Correção | Recarregar a meta por `Id` **após** a reversão e antes de reaplicar; ignorar o estado em memória da UI. Manter tudo em `EmTransacao`. Garantir que o objeto `Meta` do chamador não seja a fonte da verdade |
| Teste | `T-004`, `T-004b`, `T-004c` + I2 após cada operação |
| Pendências | Nenhuma de análise; falta execução |

## SF-005
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | PARTIALLY VALID · **MEDIUM** (era HIGH) · HIGH (mecanismo) |
| Causa técnica | `ExcluirTransacao`/`Atualizar` só validam saldo para `Saida` |
| Evidência | `TransacaoService.ValidarSaldoSuficienteParaSaida` (retorna se tipo ≠ Saida); `ExcluirTransacao` sem validação |
| Problema na original | Tratou como defeito; é **decisão**: bloquear pode impedir o usuário de corrigir um lançamento errado de entrada já gasto. Saldo negativo pode ser a realidade contábil |
| Impacto real | Saldo exibido negativo sem aviso; contradiz `AtualizarSaldoInicial` e `Registrar` |
| Correção | Depende de D-04. Qualquer alternativa deve considerar também resgate de meta (é Entrada) e troca de carteira |
| Teste | `T-005a/b/c` conforme a decisão (bloquear vs avisar vs permitir) |
| Pendências | D-04 |

## SF-006
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · HIGH · HIGH |
| Causa técnica | `Meta.Retirar` limita ao acumulado; a exclusão remove a saída da carteira mesmo quando a meta não devolveu o valor integral |
| Evidência | `ExcluirTransacaoInterna`; `Meta.Retirar`. Sequência: carteira 100 → aplicar 100 → resgatar 100 → excluir aplicação ⇒ carteira 200, meta 0 |
| Problema na original | Correto. Faltou frisar que "impedir saldo negativo" **não** corrige este caso |
| Impacto real | Dinheiro criado silenciosamente (viola I1). Simétrico: excluir resgate cuja soma foi reaplicada |
| Correção | Reverter somente se `retirado == valor`; senão bloquear com mensagem. Forma final em D-05 |
| Teste | `T-006` (+ simétrico `T-006b`) com I1 antes/depois |
| Pendências | D-05 |

## SF-007
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · MEDIUM · HIGH |
| Causa técnica | FK `meta_id ON DELETE SET NULL`; sem fluxo financeiro ao excluir |
| Evidência | `MetaDialog.Excluir`, `MetaService.DeletarMeta`, `Database.CriarTabelas`; teste `Metas_Excluir_RevinculaTransacoesParaNulo` documenta |
| Problema na original | Correto; é decisão de produto |
| Impacto real | Valor acumulado some do total (viola I1); transações sem vínculo não podem mais ser "resgatadas" |
| Correção | Alternativas em D-01 |
| Teste | `T-007` conforme D-01 |
| Pendências | D-01 |

## SF-008
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | PARTIALLY VALID · **LOW** (era MEDIUM) · HIGH (fato) |
| Causa técnica | `CalcularTotalPorTipoEMes` soma por `tipo` apenas |
| Evidência | `TransacaoRepository.CalcularTotalPorTipoEMes`; `DashboardView.Atualizar` |
| Problema na original | O fato é correto; a "correção" (excluir categorias de meta) foi apresentada como técnica e o `T-008` já assumia o resultado |
| Impacto real | "Despesas do mês" inclui dinheiro guardado; "Receitas" inclui resgates; empréstimos (categoria `Emprestimo`) também |
| Correção | Depende de D-02 |
| Teste | `T-008` conforme D-02 |
| Pendências | D-02 |

## SF-009
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · MEDIUM · HIGH |
| Causa técnica | Exceção em `OnStartup` → `DispatcherUnhandledException` com `Handled=true`; sem `MainWindow`; `ShutdownMode=OnMainWindowClose` |
| Evidência | `App.xaml.cs`, `App.xaml`. Gatilhos: `db.Initialize`, `TemaManager` (SF-021), `DashboardView` no ctor de `MainWindow` (SF-034) |
| Problema na original | Correto |
| Impacto real | Processo invisível vivo, pode segurar o banco; usuário só vê "erro inesperado" |
| Correção | `try/catch` no bootstrap: log, mensagem com caminho do banco, `Shutdown(1)` |
| Teste | Manual A.9 com **cópia** corrompida; automático inviável sem refatorar o bootstrap |
| Pendências | Execução |

## SF-010
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | INCONCLUSIVE · condicional · MEDIUM |
| Causa técnica | URI relativa de `Image.Source` em XAML de subpasta resolve relativa ao XAML |
| Evidência | `Views/Dialogs/*.xaml`, `Themes/Templates.xaml` usam `Assets/icons/...`; `MainWindow.xaml` (raiz) e `Icones.cs` (pack URI) estão corretos |
| Problema na original | Severidade fixa MEDIUM; se o carregamento síncrono falhar, o diálogo **não abre** (CRITICAL); se falhar silenciosamente, só ícone em branco (LOW). Também não verificou existência dos PNGs |
| Impacto real | Indeterminado até executar |
| Correção | `/Assets/icons/...` ou pack URI completo |
| Teste | Manual A.3, A.7 |
| Pendências | Executar; listar `Assets/icons/**` |

## SF-011
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | INCONCLUSIVE · condicional · MEDIUM / UNVERIFIED |
| Causa técnica | Guid → TEXT maiúsculo (a confirmar na versão da lib) × IDs legados possivelmente minúsculos; `=` em TEXT é case-sensitive |
| Evidência | Repositórios (`AddWithValue("$id", Guid)`); `DatabaseTests` migra só IDs numéricos |
| Problema na original | Não registrou consequências concretas: `ExcluirTransacao` vira no-op silencioso e o diálogo fecha como sucesso; `Atualizar` de meta legada não altera linha; FK falha ao inserir transação com `meta_id` maiúsculo |
| Impacto real | Se confirmado: **HIGH** (operações por ID quebradas em dados legados) |
| Correção | Normalizar formato (parâmetro e/ou migração) com teste de GUID com letras |
| Teste | `T-011` (banco sintético com `aaaaaaaa-…`) + verificação em **cópia** do banco real |
| Pendências | Cópia do banco 2.x; versão exata da lib |

## SF-012
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | INCONCLUSIVE · MEDIUM · LOW |
| Causa técnica | `CREATE … IF NOT EXISTS` preserva FTS/triggers antigos |
| Evidência | `Database.CriarBuscaTextual`; comentários indicam que a versão Java tinha FTS |
| Problema na original | Hipótese bem rotulada |
| Impacto real | Se esquemas divergem: falha em todo INSERT ou indexação duplicada |
| Correção | Detectar divergência e recriar com `rebuild` dentro de transação |
| Teste | `T-012` |
| Pendências | `sqlite_master` de uma cópia 2.x |

## SF-013
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | PARTIALLY VALID · MEDIUM · HIGH |
| Causa técnica | Ramo "contém vírgula" remove todos os `.`; sem validação de estrutura |
| Evidência | `ValorMonetario.ParseCentavos`. `1,234.56`→123; `1.2.3,4`→12340. Só `TransacaoDialog` tem `FormatarValorAoSair` |
| Problema na original | Misturou comportamento **intencional e testado** (`1,005`→101, `10.50`→1050) com defeitos. Política de parsing é decisão |
| Impacto real | Valor errado gravado sem erro em 5 diálogos |
| Correção | Política explícita (D-03); recusar o resto; reformatar ao sair em todos os diálogos |
| Teste | `T-013a/b` conforme D-03 |
| Pendências | D-03 |

## SF-014
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · LOW · HIGH |
| Causa técnica | `(long)Math.Round(valor*100m)` estoura para > ~9,2×10¹⁶ reais; diálogos (exceto Transação) só capturam `ArgumentException` |
| Evidência | `ValorMonetario`, `MetaDialog`, `CartaoDialog`, `EmprestimoDialog`, `CarteirasDialog`, `TransferenciaDialog` |
| Problema na original | Correto |
| Impacto real | Exceção cai no handler global; diálogo permanece aberto |
| Correção | Capturar overflow em `ParseCentavos` → `ArgumentException("Valor inválido")`; teto definido |
| Teste | `T-014` |
| Pendências | — |

## SF-015
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | PARTIALLY VALID · LOW · HIGH (a) |
| Causa técnica | (a) divisão em `double`, sem tratamento do resto: 100,00/3 ⇒ 33,33 (3×33,33=99,99); teste `ValorParcela_ArredondaParaCima` tem nome oposto ao afirmado. (b) não existe "parcela paga" |
| Evidência | `CompraCartaoCredito.ValorParcelaCentavos`; `CartaoCreditoService` |
| Problema na original | (b) é modelagem de produto, não defeito |
| Impacto real | (a) centavo perdido na soma; (b) "Total em aberto"/"Parcelas por mês" não diminuem |
| Correção | (a) divisão inteira + resto (primeira ou última parcela: D-06); (b) D-07 |
| Teste | Σ parcelas == total para 3, 7, 12 parcelas |
| Pendências | D-06, D-07 |

## SF-016
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · LOW · HIGH |
| Causa técnica | `static readonly DateOnly Hoje` em `CartoesView` |
| Evidência | `CartoesView.xaml.cs`; `LinhaCompra` usa `DateTime.Today` a cada carga |
| Problema na original | Correto |
| Impacto real | Após meia-noite com app aberto, contador × cores divergem |
| Correção | Calcular em `Atualizar()` |
| Teste | Manual (alterar data do SO) — teste unitário não alcança (o serviço já recebe `hoje`) |
| Pendências | — |

## SF-017
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | INCONCLUSIVE · condicional · MEDIUM |
| Causa técnica | Template de `DatePickerTextBox` = `Border + ContentPresenter`, sem `PART_ContentHost` |
| Evidência | `Global.xaml` (Style `DatePickerTextBox`); o `DatePickerTextBox` interno do template do `DatePicker` pega esse estilo implícito |
| Problema na original | Severidade LOW subestima: se o texto não renderiza, **nenhuma data aparece** em 6 telas/diálogos e a digitação é impossível |
| Impacto real | Indeterminado até executar |
| Correção | `ScrollViewer x:Name="PART_ContentHost"` no template |
| Teste | Manual A.3 |
| Pendências | Executar |

## SF-018
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · LOW · HIGH |
| Causa técnica | `MainWindow` não chama `JanelaNativa`; `AplicarBarraEscura` não desfaz e só roda em `SourceInitialized` |
| Evidência | `MainWindow.xaml.cs`, `JanelaNativa.cs`, `ARQUITETURA.md` ("acompanha o modo escuro") |
| Problema na original | Correto |
| Impacto real | Barra clara sobre UI escura |
| Correção | Aplicar na `MainWindow` e reaplicar (inclusive desligar) em `TemaAlterado` |
| Teste | Manual A.8 |
| Pendências | — |

## SF-019
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · LOW · HIGH |
| Causa técnica | `OverrideMetadata` usa `CurrentCulture`, comentário diz pt-BR |
| Evidência | `App.xaml.cs` |
| Problema na original | Correto; também invalida a conclusão "AUD-003 corrigido" da auditoria anterior para SOs não pt-BR |
| Impacto real | Datas `M/d/yyyy` em Windows en-US |
| Correção | Fixar `pt-BR` |
| Teste | Manual A.10 |
| Pendências | — |

## SF-020
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | PARTIALLY VALID · LOW · HIGH |
| Causa técnica | `Escapar` só trata aspas/quebras; categoria exportada como nome do enum; ordens distintas; só `IOException` capturada |
| Evidência | `TransacaoCsvExporter`, `TransacaoTxtExporter`, `TransacoesView.xaml.cs`; testes `ExporterTests` **afirmam** `Alimentacao` e as ordens |
| Problema na original | (b) rótulo do enum e (c) ordem são comportamento testado/intencional (INFO, D-08); (a) injeção de fórmula e (d) `UnauthorizedAccessException` são defeitos |
| Impacto real | `=`/`+`/`-`/`@` iniciais executam como fórmula no Excel; exceção genérica em pasta protegida |
| Correção | Prefixar `'`; capturar `UnauthorizedAccessException` |
| Teste | `T-020` |
| Pendências | D-08 |

## SF-021
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · LOW · HIGH |
| Causa técnica | `Salvar` captura só `IOException`; `UnauthorizedAccessException` não deriva dela |
| Evidência | `TemaManager.Salvar`, chamado por `Aplicar` em `Inicializar` (dentro de `OnStartup`) |
| Problema na original | Correto; faltou ligar a SF-009 |
| Impacto real | Pasta `%APPDATA%\SmartFinance` sem escrita ⇒ falha de inicialização (zumbi, SF-009) |
| Correção | Capturar também; não gravar em `Inicializar` |
| Teste | `T-021` |
| Pendências | — |

## SF-022
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | PARTIALLY VALID · LOW · HIGH (ordem) / LOW (VACUUM) |
| Causa técnica | Edição = DELETE+INSERT ⇒ novo `rowid`; ordenação usa `rowid` |
| Evidência | `TransacaoService.Atualizar`; `TransacaoRepository.ListarRecentes` |
| Problema na original | Efeito de ordenação correto. `VACUUM` renumerando rowid sem PK inteira é documentado do SQLite, mas o app não executa `VACUUM`; hipotético |
| Impacto real | Transação editada "salta" ao topo do dia |
| Correção | `UPDATE` quando não envolve meta; ou ordenar por chave estável |
| Teste | Ordem antes/depois da edição |
| Pendências | — |

## SF-023
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · LOW · HIGH |
| Causa técnica | `AtualizarVisibilidadeMeta` zera `ComboMeta.SelectedItem` em toda mudança de categoria, inclusive na carga de edição |
| Evidência | `TransacaoDialog.xaml.cs` |
| Problema na original | Correto; faltou dizer que **é o caminho que aciona SF-004** |
| Impacto real | Reseleção obrigatória; clamp silencioso grava menos que o digitado |
| Correção | Pré-selecionar por `MetaId`; informar o valor efetivamente aplicado (D-09) |
| Teste | Manual: editar aplicação e conferir meta/valor |
| Pendências | D-09; corrigir **depois** de SF-004 |

## SF-024
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | PARTIALLY VALID · **INFO** (era LOW) · **MEDIUM** (era HIGH) |
| Causa técnica | Verificar-e-inserir sem `BEGIN IMMEDIATE`; transação padrão é deferida |
| Evidência | `CarteiraService.Transferir`, `TransacaoService.Registrar` |
| Problema na original | Corrida exige duas instâncias (permitidas, T7). Em WAL, upgrade de leitura→escrita após commit alheio pode falhar com `SQLITE_BUSY_SNAPSHOT` sem respeitar `busy_timeout` — mais provável que saldo negativo |
| Impacto real | Baixo (uso pessoal); erro "database is locked" ocasional com duas instâncias |
| Correção | Instância única (mutex) ou `BEGIN IMMEDIATE`; transferência sem edição/exclusão é lacuna de produto |
| Teste | Duas conexões com sequência controlada |
| Pendências | — |

## SF-025
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | PARTIALLY VALID · LOW · MEDIUM |
| Causa técnica | Setter local de estilo implícito (`TextBlock.Foreground`) vence a herança do pai |
| Evidência | `Global.xaml` (Style `TextBlock`), `BotaoNav`, `MainWindow.xaml` (TextBlocks explícitos dentro do `RadioButton`) |
| Problema na original | Mecanismo correto; impacto pequeno: no tema claro o contraste do item ativo cai a ~4:1 (estimativa), no escuro é adequado. Item inativo também usa cor primária em vez de secundária |
| Impacto real | Cosmético |
| Correção | Remover `Foreground` do estilo implícito |
| Teste | Manual A.8 |
| Pendências | Visual |

## SF-026
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · INFO · HIGH |
| Causa técnica | n/a (documentação) |
| Evidência | Contagem real: 63 métodos/79 casos (ver `17` §6). `NoWarn CS8602` no csproj. Itens sem uso: `Formatacao.TryParseData`, `ValorMonetario.FormatarCentavos` (só testes), `MainWindow.AbrirDialogTransacao`, `FonteSaldo`, `using System.ComponentModel` (Dashboard e Transacoes views), `MetaService.CalcularProgressoPercentual/MetaConcluida/ContarMetasConcluidas/CalcularRestante` (só testes) |
| Problema na original | Correto; a própria tabela de métodos do inventário anterior soma 82 ≠ 49 |
| Impacto real | Docs enganosas |
| Correção | Corrigir docs; decidir sobre código sem uso |
| Teste | Revisão |
| Pendências | — |

## SF-027
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | PARTIALLY VALID · INFO · MEDIUM |
| Causa técnica | `Microsoft.Data.Sqlite 10.0.0` + `SQLitePCLRaw.bundle_e_sqlite3 3.0.5` explícito |
| Evidência | `SmartFinance.csproj`; justificativa em `SECURITY_REPORT.md` |
| Problema na original | `NoWarn` verificado; compatibilidade de pacotes e CVE **não verificados** (UNVERIFIED) |
| Impacto real | Desconhecido; risco de incompatibilidade em runtime nativo/FTS5 |
| Correção | Validar combinação (smoke + FTS5 + WAL + publish) |
| Teste | `dotnet test` + smoke após restore limpo |
| Pendências | Consulta aos feeds/avisos |

## SF-028
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · INFO · HIGH |
| Causa técnica | `MouseDoubleClick` no `ListBox` usa `SelectedItem` |
| Evidência | `*View.xaml.cs` |
| Problema na original | Correto |
| Impacto real | Duplo clique no vazio abre item previamente selecionado; sem Enter |
| Correção | Verificar item sob o cursor; tratar Enter |
| Teste | Manual |
| Pendências | — |

## SF-029
| Campo | Conteúdo |
|---|---|
| Veredito / Sev. / Conf. | VALID · LOW · HIGH |
| Causa técnica | Serviço aceita qualquer par Tipo×Categoria (para metas ignora o `tipo` recebido) |
| Evidência | `TransacaoService.Registrar/Atualizar` |
| Problema na original | Correto; subestima por ser a única defesa contra SF-003 |
| Impacto real | `Registrar(Saida, Salario)` aceito |
| Correção | Validar com as mesmas tabelas da UI, movidas para o domínio |
| Teste | `T-003` |
| Pendências | Definir pares válidos (inclui `Emprestimo` em ambos) |
