# 17 — Revisão independente (segunda auditoria)

Documentos relacionados: matriz completa em [`18_FINDINGS_REVALIDATION.md`](18_FINDINGS_REVALIDATION.md) · novos achados em [`19_NEW_FINDINGS.md`](19_NEW_FINDINGS.md) · roadmap em [`20_ROADMAP_REVIEW.md`](20_ROADMAP_REVIEW.md) · testes em [`21_TEST_PLAN_REVIEW.md`](21_TEST_PLAN_REVIEW.md) · handoff em [`22_FINAL_GLM_HANDOFF.md`](22_FINAL_GLM_HANDOFF.md).

## 1. Escopo e materiais
- **Tipo:** revisão documental **+ releitura estática do código** (os 84 arquivos do repositório estavam no contexto da conversa) dos 6 documentos `00, 12–16`.
- **Não houve:** acesso a disco/SDK, compilação, execução de testes, abertura de banco, reprodução de bugs, execução do app. Nada nesta revisão foi executado.
- **Viés a declarar:** a primeira auditoria e esta revisão foram feitas pelo mesmo modelo. A revisão reduz erros por releitura independente do código, mas **não substitui** um segundo revisor humano ou a execução do smoke test.
- **Escala usada:** confiança em inglês (CONFIRMED/HIGH/MEDIUM/LOW/UNVERIFIED). Mapeamento do original: Alto→HIGH, Médio→MEDIUM, Baixo→LOW. Nenhum achado é CONFIRMED.

## 2. Metodologia
1. Reli cada achado contra o código citado, refazendo o fluxo (evento → serviço → SQL → UI).
2. Refiz as contas numéricas dos exemplos e dos testes propostos.
3. Contei os testes diretamente nos `.cs` (não nos documentos).
4. Procurei omissões por área (finanças, banco, WPF, arquitetura, segurança, distribuição, testes).
5. Conferi consistência entre os 6 documentos.

## 3. Resultado geral
| Item | Resultado |
|---|---|
| Achados originais revalidados | 29 |
| VALID | 16 |
| PARTIALLY VALID | 9 |
| INVALID | 0 |
| INCONCLUSIVE | 4 (SF-010, 011, 012, 017) |
| Novos achados | 8 (SF-030 … SF-037) |
| Severidade reclassificada | 3 definitivas (SF-005 HIGH→MEDIUM, SF-008 MEDIUM→LOW, SF-024 LOW→INFO) + 2 condicionais (SF-010, SF-017) |
| Confiança reclassificada | 1 (SF-024 HIGH→MEDIUM) |

Nenhum achado foi rejeitado. Vários eram **corretos no mecanismo mas apresentados como defeito quando dependem de decisão de produto** (SF-005, 007, 008, 013, 015b). Isso é o principal problema de classificação da primeira auditoria.

## 4. Qualidade da primeira auditoria — erros encontrados
| # | Erro | Onde | Correção |
|---|---|---|---|
| E1 | Exemplo numérico de SF-004 inconsistente: com meta de atual=alvo=500, a edição lançaria "Meta já alcançada" (clamp usa `RestanteParaAlvo` da instância antiga) em vez de inflar | `12` SF-004 | corrigido no `12` (alvo 1000, atual 100 → grava 200, esperado 100) |
| E2 | "21 Alto, 6 Médio, 2 Baixo" — contagem real do catálogo: **23 / 5 / 1** | `00` | corrigido |
| E3 | "85 arquivos lidos" — eram **84** | `00` | corrigido |
| E4 | SF-005, 007, 008, 013 tratados como correção técnica; os testes T-005/006/008/013a **pressupõem a decisão** | `12`, `14` | marcados como dependentes de decisão (ver `22`) |
| E5 | SF-010 e SF-017 com severidade fixa; o desfecho varia de cosmético a **bloqueante** (exceção de XAML / data invisível) | `12` | severidade condicional; confirmação por smoke antes de qualquer correção |
| E6 | T-001 (instanciar `TransacoesView`) não é executável como descrito: `App.Servicos` tem `private set`, o teste precisa de `Application` com os dicionários de recursos e de thread STA (xUnit puro não oferece) | `14` | ver `21` |
| E7 | T-R (reconciliação) sem fórmula: aplicar em meta é `Saida`, então "soma algébrica" incluiria movimentos internos | `14` | fórmula corrigida em `14` e `21` |
| E8 | Linha confusa em `15` ("Arquivos não recebidos…") | `15` | reescrita |
| E9 | SF-024 superdimensionado (LOW); a corrida exige duas instâncias | `12` | INFO |
| E10 | Faltou a análise do backup em WAL (SECURITY/README mandam copiar só `financas.db`) | — | SF-032 |

**Acertos verificados:** contagem de severidades (1/5/7/13/3) bate com o índice; 29 achados todos presentes no roadmap (5+10+5+4+5); matriz cobre os 29; 79 casos de teste conferem com a releitura (ver §6).

## 5. Investigações prioritárias (resumo; detalhes por achado em `18`)
- **SF-001:** `TransacoesView.xaml` declara `<ComboBoxItem IsSelected="True" Content="Todas"/>` em `ComboCategoria`; o construtor atribui `ItemsSource` após `InitializeComponent`. Regra do WPF: `ItemsSource` com `Items` já preenchido por conteúdo direto lança `InvalidOperationException`. Não há `Items.Clear()`. Ocorre **sempre** que a tela é criada (único caminho: `AoNavegar`/`IrParaTransacoes`). Efeito colateral adicional não citado antes: o `RadioButton` "Transações" fica marcado enquanto o conteúdo continua sendo o Dashboard (estado visual inconsistente). Nada invalida a conclusão; falta só execução.
- **SF-002:** sem `DisplayMemberPath`/`ItemTemplate`; os únicos `DataTemplate` são para `Linha*`. `Carteira` (record) mostra `Carteira { Id = …, Nome = Banco, … }` — legível mas feio; `Meta` mostra o nome do tipo — **inoperável**. Para metas é bloqueio operacional; para carteiras é estético.
- **SF-003:** os controles são `ToggleButton` com `Style=BotaoAlternar` (template preserva o comportamento de alternância). Só `Checked` é tratado. Clique em botão já marcado desmarca sem evento. `TipoAtual` = `BotaoEntrada.IsChecked == true ? Entrada : Saida` ⇒ estado "nenhum" vira `Saida`; lista de categorias permanece a de Entrada. Persistido: `tipo='Saida'` com `categoria='Salario'`. O ramo `else if (nenhum marcado)` é inalcançável porque só roda dentro de `Checked`. Idem `EmprestimoDialog` (nenhum ⇒ `PediEmprestado`).
- **SF-004:** instância `meta` vem da UI, carregada ao trocar categoria; dentro de `Atualizar`, `ExcluirTransacaoInterna` reverte uma **segunda instância** no banco; depois `RegistrarAplicacaoMeta(meta,…)` soma sobre a instância antiga e grava, sobrescrevendo a reversão. Só ocorre quando a meta reaplicada é a **mesma** da original (meta diferente: a original é revertida corretamente e a nova é lida do banco). Vale também para resgate. Clamp usa `RestanteParaAlvo` obsoleto (mais restritivo). Transação SQL não protege estado em memória. Nenhum teste existente edita transação que já era de meta.
- **SF-005/006:** ver §7. SF-006 independe de decisão de produto sobre saldo negativo: ele **cria** dinheiro (sequência aplicar→resgatar→excluir aplicação); a correção "só impedir saldo negativo" **não** resolve SF-006.
- **SF-009:** `Application.Run` dispara `OnStartup` via dispatcher; exceção cai em `DispatcherUnhandledException`, que marca `Handled=true`; sem `MainWindow` e com `ShutdownMode=OnMainWindowClose` o loop nunca termina. Confirmado por raciocínio sobre o framework, não por execução. Amplificado por SF-034.
- **SF-011/012:** não verificáveis sem cópia do banco 2.x. Comportamento a validar: `Microsoft.Data.Sqlite` grava `Guid` em TEXT maiúsculo (HIGH, de memória do código-fonte da lib — **verificar na versão 10.0.0**). Se os IDs legados são minúsculos: `BuscarPorId`/`Excluir`/`Atualizar` por Guid **não encontram a linha**; `ExcluirTransacao` retorna silenciosamente (t nulo) e o diálogo fecha como sucesso; inserir transação com `meta_id` maiúsculo contra `meta.id` minúsculo viola a FK. O teste de migração usa só dígitos. Evidência parcial documental: T10 (startup com cópia do banco real) só prova leitura/soma, não operações por ID. **O que falta:** `SELECT id FROM meta/transacao LIMIT 5` numa cópia; teste de edição/exclusão numa transação legada.
- **SF-013:** `1,234.56` → 123 centavos (reconstituído linha a linha). Só `TransacaoDialog` reformata ao sair do campo (o usuário veria `1,23`). Os outros 5 diálogos gravam sem feedback. `1,005`→101 e `10.50`→1050 são **comportamento intencional testado**; mudar é decisão (ver `22`). Nova falha sem decisão: `0.500` → R$ 500 (SF-030).

## 6. Contagem de testes (conferida nos arquivos)
| Classe | Métodos (real) | Casos | Inventário anterior |
|---|---|---|---|
| ValorMonetarioTests | 5 | 21 | "20 métodos" ✗ |
| TransacaoServiceTests | 15 | 15 | 15 ✓ |
| CarteiraServiceTests | 6 | 6 | 7 ✗ |
| DatabaseTests | 6 | 6 | 7 ✗ |
| PlanejamentoFinanceiroTests | 13 | 13 | 15 ✗ |
| ExporterTests | 4 | 4 | 4 ✓ |
| AuditoriaTests | 14 | 14 | 14 ✓ |
| **Total** | **63** | **79** | "49 métodos" ✗ (e a coluna soma 82) |

Estados: **histórico** (auditoria anterior): 79/79 verdes. **Observado na 1ª auditoria desta série:** não executado. **Reproduzido nesta revisão:** não executado. **Contagem estática:** 79 casos (confere com "79 execuções"). O verde atual é **UNVERIFIED**.

## 7. Invariantes financeiros propostos (base para os testes)
- **I1 (conservação):** Σ saldos das carteiras + Σ `Meta.AtualCentavos` = Σ saldos iniciais + Σ(`Entrada` com categoria ≠ `RetirarMeta`) − Σ(`Saida` com categoria ≠ `AdicionarMeta`). Transferências não alteram o lado direito.
- **I2:** `Meta.AtualCentavos` = Σ(AdicionarMeta ligadas) − Σ(RetirarMeta ligadas) por meta (válido enquanto não há exclusão de meta e nenhum clamp foi ativado).
- **I3 (a decidir, SF-005):** saldo de cada carteira ≥ 0 após qualquer operação.
- **I4:** nenhuma operação deixa estado parcial (rollback total) — inclusive **instâncias em memória** (`Meta`) coerentes com o banco após falha.

SF-004 viola I2; SF-006 viola I1; SF-007 viola I1 (se meta é removida do lado esquerdo); SF-005 viola I3.

## 8. Omissões encontradas (resumo; detalhes em `19`)
SF-030 (`0.500`), SF-031 (tamanho mínimo de janela), SF-032 (backup/WAL), SF-033 (API `ComandoSelecaoTransacoes` interpola SQL), SF-034 (uma linha inválida derruba a inicialização), SF-035 (compatibilidade de strings de enum com o banco Java — não verificado), SF-036 (módulos Cartão/Empréstimos não conectados a carteiras), SF-037 (testes de moeda dependem de ICU/NLS).

## 9. Conclusão independente
Os achados da 1ª auditoria **resistem** à releitura: nenhum é falso; quatro estão **inconclusivos** e dependem de um teste visual de minutos ou do banco legado. O plano original era seguro em linhas gerais, mas (a) misturava decisões de produto com correções, (b) tinha um exemplo e duas fórmulas de teste incorretos, (c) não previa baseline de build/testes, backup do banco com WAL e checkpoint de confirmação por smoke. Esses pontos estão corrigidos em `20`, `21` e `22`.

**Pronto para o GLM?** Parcialmente. Pode começar imediatamente o trabalho **sem decisão de produto** (ver `22` §4). O restante está bloqueado por decisões e por verificações que exigem execução.
