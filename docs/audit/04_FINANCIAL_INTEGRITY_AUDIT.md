# 04 — Auditoria financeira e integridade matemática

## Precisão monetária

- Representação: `long` centavos em **toda** a cadeia (grep confirmado — nenhum `double`/`float` em cálculo monetário).
- Conversões texto↔centavos: `ValorMonetario` (decimal estrito D-03, teto de long, half-up). 21 casos de teste.
- Arredondamento de parcela: half-up (D-06) — soma das parcelas pode divergir 1 centavo do total, visível na UI.
- Percentuais de meta: `Progresso` (0–1) — exibição `0%`–`100%`.

## Invariantes e reconciliação (executados)

- **I1** (conservação): `TR_CenarioComCartaoEEmprestimo_ConservaODinheiro` — Σ carteiras + Σ metas = inicial + entradas externas − saídas externas, **incluindo** movimentações de cartão/empréstimo (F2). PASSOU.
- **I2** (meta = Σ vinculadas − Σ resgatadas): T-004/b/c após SF-004. PASSOU.
- **I3** (saldo ≥ 0): T-005a/b/c. PASSOU.
- **Rollback**: `Atualizar_ComValidacaoFalha` e `AtualizacaoComposta_FalhaNaSegundaEtapa`. PASSOU.

## Datas e competência

- Armazenamento ISO `yyyy-MM-dd` (compatível 2.x); comparadores por string do início ao fim do mês (`data >= início AND data < fim`) — virada de mês/ano corretas; bissexto delegado a `DateOnly`.
- **SF-040 (HIGH)**: as movimentações geradas por cartão/empréstimo usam `DateTime.Today` como data (`CartaoCreditoService.cs:25`, `EmprestimoService.cs:34`) e a edição **recria** a movimentação com a data do dia da edição. Consequências: (a) registrar "Pedi emprestado" com pagamento futuro infla as receitas **do mês atual**; (b) editar um lançamento antigo o move para o topo do histórico e para o mês da edição. Não há teste cobrindo a data da movimentação.
- Virada de meia-noite: `DataTransacoesTests` executa filtros com data corrente; `CartoesView.Hoje` calculado por atualização (SF-016 corrigido).

## Reconciliação por período (F1/F2)

"Total em aberto" = parcelas restantes × parcela (amortiza com `parcelas_pagas` — testado F1). "Parcelas por mês" soma apenas compras com restantes > 0. "Receitas/Despesas do mês" excluem categorias de meta (D-02) e **incluem** movimentações de cartão/empréstimo na data de criação — consistente com o modelo D-10 **exceto** pela competência (SF-040).

## Riscos de inconsistência

- Edição de compra/empréstimo (F2) recria a movimentação — é atomicamente consistente (EmTransacao), porém reposiciona a data (SF-040).
- Exclusão de compra/empréstimo legado 2.x sem `transacao_id`: estorno é no-op silencioso — **correto** (não havia movimento para estornar) e coberto por teste (`F2_LegadoSemMovimentacao_ExcluirNaoFalha`).
