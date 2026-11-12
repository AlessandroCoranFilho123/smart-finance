# 04 — Auditoria financeira (GLM)

## Representação e precisão

`long` centavos em toda a cadeia (grep sem `double`/`float`); parsing decimal estrito (D-03) com teto de long; half-up apenas na parcela (D-06). SQLite INTEGER. Nenhuma conversão com perda identificada.

## Invariantes formalizados e executados (verdes no commit auditado)

| Invariante | Teste |
|---|---|
| I1 conservação (Σ carteiras + Σ metas = inicial + entradas − saídas externas) | `TR_CenarioMisto_ConservaODinheiro`, `TR_CenarioComCartaoEEmprestimo` |
| I2 meta = Σ vinculadas − Σ resgatadas | T-004/b/c |
| I3 saldo de carteira ≥ 0 após qualquer operação | T-005a/b/c + validações em Registrar/Atualizar/Excluir |
| Atomicidade | `Atualizar_ComValidacaoFalha`, `AtualizacaoComposta_FalhaNaSegundaEtapa` |

## Operações auditadas (criação/edição/exclusão/competência)

| Operação | Resultado |
|---|---|
| Receita/despesa CRUD | OK (unit + GUI persistência) |
| Editar = reverter + aplicar (delete+insert) | OK atômico; **competência distorce na edição** (SF-040) |
| Excluir entrada/saída com fundos | OK (bloqueio D-04) |
| Aplicar/resgatar meta com clamp | OK (T-004, D-09) |
| Excluir meta com acumulado | Bloqueado (D-01/T-007) |
| Cartão: aberto amortiza com pagas | OK (F1_*) |
| Cartão/empréstimo movem carteira | OK (F2_*, D-10) — **data de competência: SF-040** |
| Transferência conserva | OK (CarteiraServiceTests) |

## Valores extremos (executados)

R$ 0,01 / 0,05 / 999,99 / 10.000,00 / 999.999.999,99 / teto de long — parsing e persistência exatos (`ValorMonetarioTests`, `RecursosNovosTests`). Sub-centavo recusado (D-03).

## Conclusão financeira

Sem nova divergência de integridade encontrada nesta auditoria. Risco aberto: **SF-040** (competência), já catalogado com correção recomendada — a decisão de produto (data preservada vs. campo de competência) deve ser registrada antes da correção.
