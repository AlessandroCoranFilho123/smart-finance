# 12 — Catálogo de achados (SF-040 … SF-051)

**Numeração:** continua de `SF-038/SF-039` (rodada 5) para não reutilizar IDs das auditorias anteriores (SF-001…037 + AUD-*). Base: commit `4810844` (3.4.0). Viés declarado: esta auditoria foi feita pelo mesmo agente que implementou o projeto — mitigada por execução real (148 testes) e re-leitura crítica guiada pelos fluxos, não pelas intenções.

| ID | Sev. | Categoria | Título | Confiança |
|---|---|---|---|---|
| SF-040 | HIGH | Financeiro | Movimentações de cartão/empréstimo fixadas na data de criação; edição reposiciona no histórico | Alto |
| SF-041 | MEDIUM | Funcional | Falha na instalação da atualização encerra o app sem reabertura nem explicação | Alto |
| SF-042 | MEDIUM | Documentação | README/ARQUITETURA/SECURITY sem os recursos 3.3/3.4 | Alto |
| SF-043 | LOW | Banco | `carteira_id`/`transacao_id` de compra e empréstimo sem FOREIGN KEY | Alto |
| SF-044 | LOW | Banco | Vínculo `transacao_id` órfão transformaria edição em movimentação duplicada (exige manipulação manual do banco) | Baixo |
| SF-045 | INFO | Funcional | Movimentações de cartão usam categoria `Compras` (misturam-se em filtros — comportamento D-10) | Alto |
| SF-046 | LOW | Funcional | Janela "Baixando atualização" sem Owner e download não cancelável | Alto |
| SF-047 | INFO | Segurança | Hash do instalador publicado no mesmo canal da release (mitigado por TLS + repositório) | Alto |
| SF-048 | LOW | Funcional | `%TEMP%\SmartFinance-Update` acumula instaladores antigos | Alto |
| SF-049 | INFO | Testes | `ContrasteTests` resolve paletas por caminho relativo do repositório | Alto |
| SF-050 | INFO | Funcional | `settings.json`/`atualizacao.json` sem versionamento de schema | Alto |
| SF-051 | LOW | Funcional/UX | Rótulo "Data de pagamento" do empréstimo ambíguo | Alto |

## SF-040 — Movimentações fixadas na data de criação

**Severidade:** HIGH · **Categoria:** Financeiro · **Confiança:** Alto (código + ausência de teste)
**Localização:** `Services/CartaoCreditoService.cs:25`, `Services/EmprestimoService.cs:34` (`var hoje = DateOnly.FromDateTime(DateTime.Today)`), método `Salvar` (criação **e** recriação na edição).
**Descrição:** as transações geradas por D-10 usam a data do dia do cadastro; na edição, a movimentação antiga é excluída e uma nova é inserida com a data do dia da edição — o lançamento "salta" no histórico e muda de mês de competência.
**Evidência técnica:** código citado; nenhum teste afirma `movimentacao.Data` (10_TEST_COVERAGE lacuna 1).
**Como reproduzir:** cadastrar "Pedi emprestado" hoje → editar o valor amanhã → a movimentação aparece datada de amanhã.
**Comportamento atual:** data = dia do cadastro/edição. **Esperado:** competência escolhida/estável (ex.: usar a data do registro original na edição, ou campo de data no diálogo).
**Impacto:** totais mensais e histórico distorcidos.
**Correção recomendada:** adicionar competência ao fluxo (preservar a data original na recriação; opcionalmente expor campo), com teste NT-1/NT-3 do plano.
**Arquivos:** `CartaoCreditoService.cs`, `EmprestimoService.cs`, `CartaoDialog.xaml.cs`, `EmprestimoDialog.xaml.cs`.
**Risco de regressão:** T-R, F2_*.
**Validação:** NT-1/NT-3 verdes + reconciliação.

## SF-041 — Atualização falha deixa o usuário sem aplicativo

**Severidade:** MEDIUM · **Categoria:** Funcional · **Confiança:** Alto (leitura de fluxo; não reproduzido)
**Localização:** `MainWindow.xaml.cs` (`OferecerAtualizacao` → `InstalarAtualizacao` + `Shutdown`).
**Descrição:** após iniciar o instalador silencioso, o aplicativo encerra imediatamente. Se o instalador falhar (UAC recusado, disco cheio), ninguém reabre o app e nenhuma mensagem específica é mostrada.
**Comportamento esperado:** detectar a falha (aguardar o processo do instalador; se ele terminar e o app não tiver sido reaberto, reabrir `{app}\SmartFinance.exe` e registrar no log) ou adiar o encerramento até o instalador ter iniciado com sucesso.
**Impacto:** o usuário precisa reabrir manualmente; a atualização falha silenciosamente.
**Correção recomendada:** aguardar o processo do instalador (timeout), checar exit code, registrar e reabrir o app em caso de falha; em caso de sucesso o instalador já reabre ([Run] incondicional).
**Arquivos:** `MainWindow.xaml.cs`, `AtualizacaoService.cs`.
**Validação:** simular recusa de UAC localmente.

## SF-042 — Documentação desatualizada para 3.3/3.4

**Severidade:** MEDIUM · **Categoria:** Documentação · **Confiança:** Alto (grep: 0 menções)
**Descrição/correção:** ver `11_DOCUMENTATION_AUDIT.md`.
**Validação:** revisão das três docs após atualização.

## SF-043 — FKs ausentes em compra/emprestimo

**Severidade:** LOW · **Categoria:** Banco · **Confiança:** Alto (schema `Database.cs:175-176,187-188` sem REFERENCES)
**Descrição:** `transacao.carteira_id` tem FK, mas as colunas novas de compra/emprestimo não. Integridade garantida só pela aplicação.
**Correção recomendada:** nas próximas migrations, `COLLATE`/FK não são retroativas sem rebuild de tabela — avaliar PRAGMA foreign_keys + recriação de tabela em migration futura, ou documentar como decisão.
**Validação:** teste de integridade referencial sintética.

## SF-044 — `transacao_id` órfão duplicaria movimentação

**Severidade:** LOW · **Categoria:** Banco · **Confiança:** Baixo (exige manipulação manual)
**Descrição:** sem FK, um `transacao_id` apontando para transação inexistente faz `EstornarMovimentacao` virar no-op; na edição, a movimentação antiga permanece + nova inserida = duplicada.
**Correção recomendada:** ao editar, se a movimentação vinculada não existir, apenas inserir (comportamento já correto); opcional: logar. Documentar.
**Validação:** teste sintético com vínculo órfão.

## SF-045 — Categoria `Compras` nas movimentações de cartão
**Severidade:** INFO · Comportamento D-10; filtros por categoria misturam. Recomendação: documentar ou criar categoria própria (decisão futura).

## SF-046 — Janela de download sem Owner/cancelamento
**Severidade:** LOW · `CriarJanelaBaixando` não define `Owner` e não pode ser cancelada. Correção: `Owner = MainWindow` + botão cancelar via CancellationToken.

## SF-047 — Hash do instalador no mesmo canal
**Severidade:** INFO · Ver `06_SECURITY_AUDIT.md`. Mitigado por TLS/repositório; mudança de canal (ex.: assinatura de código) está encerrada por decisão U1.

## SF-048 — Acúmulo em `%TEMP%\SmartFinance-Update`
**Severidade:** LOW · Correção: apagar versões anteriores antes de baixar.

## SF-049 — Caminho relativo no ContrasteTests
**Severidade:** INFO · Funciona no layout padrão/CI; frágil se a árvore mudar. Aceitável.

## SF-050 — Sem versionamento de schema dos JSONs
**Severidade:** INFO · Parser tolera campos novos/ausentes — documentar convenção.

## SF-051 — "Data de pagamento" ambígua
**Severidade:** LOW · Correção: renomear rótulo para "Previsão de retorno".
