# 10 — Cobertura de testes e validação

## Execução nesta auditoria (commit `4810844`)

| Suíte | Comando | Resultado |
|---|---|---|
| Unitária | `dotnet test tests/SmartFinance.Tests` | **136/136** (36s) |
| GUI/UIA | `dotnet test tests/SmartFinance.GuiTests -c Release` | **12/12** (1m39s) |

## Inventário (métodos por classe)

| Classe | Métodos | Categoria |
|---|---|---|
| ValorMonetarioTests | 6 | Unitário (parsing) |
| ContrasteTests | 2 | WCAG programático |
| TransacaoServiceTests | 15 | Negócio |
| CarteiraServiceTests | 6 | Negócio |
| DatabaseTests | 7 | Persistência/FTS/migração |
| PlanejamentoFinanceiroTests | 15 | Cartão/empréstimos/metas |
| AuditoriaTests | 14 | Robustez/volumes/concorrência |
| ManutencaoTests | 5 | Backup/rotação |
| RecursosNovosTests | 19+2 | F1/F2/F5/F6 |
| AuditoriaExternaTests | 17 | T-003…T-R |
| ExporterTests | 5 | CSV/TXT |
| **GUI** GuiTests / CapturaTests / AcessibilidadeTests / DataTransacoesTests | 6/2/2/2 | Interação/captura/a11y/datas |

## Lacunas (sem evidência de cobertura)

1. **Data da movimentação gerada** (cartão/empréstimo) — nenhum teste afirma a competência (conecta ao SF-040).
2. **Fluxo de falha da instalação de atualização** (UAC recusado / hash inválido) — coberto apenas por leitura de fluxo.
3. **Migração 2.x das tabelas `compra_cartao_credito`/`emprestimo`** (a migração de `transacao` tem teste; a de planejamento não).
4. **`AtualizacaoService.VerificarNovaVersaoAsync`** (rede) — sem teste (exige mock de HTTP; recomendado abstrair o cliente).
5. **Leitor de tela físico / Windows 11 / suspensão** — fora do ambiente.

## Testes recomendados (para o GLM implementar na etapa de correção)

| ID | Objetivo | Critério |
|---|---|---|
| NT-1 | Data da movimentação gerada é a competência informada (pós-SF-040) | `movimentacao.Data == competencia` |
| NT-2 | Migração 2.x de compra/emprestimo adiciona as 3 colunas sem perder registros | colunas presentes + linhas preservadas |
| NT-3 | Editar compra não reposiciona a movimentação | `movimentacao.Data` inalterada |
| NT-4 | `VerificarNovaVersaoAsync` com cliente HTTP abstraído (versão nova/igual/erro) | resultado coerente |
