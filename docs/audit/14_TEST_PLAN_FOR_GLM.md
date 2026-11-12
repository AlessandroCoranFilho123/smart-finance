# 14 — Plano de testes pós-correção

Convenções: `TestApp` (banco isolado), centavos, `DateOnly` fixo quando determinístico. Executar com `dotnet test` completo + `CapturaTests` para evidências visuais.

## Novos testes (a implementar pelo GLM junto às correções)

| ID | Objetivo | Pré-condição | Passos | Esperado | Critério |
|---|---|---|---|---|---|
| NT-1 | Movimentação usa a competência definida | correção SF-040 aplicada | criar compra com competência 15/01/2026 | `movimentacao.Data == 15/01/2026` | passa |
| NT-2 | Migração 2.x de compra/emprestimo | banco legado com as tabelas antigas | `Initialize` | 3 colunas adicionadas; registros preservados | passa |
| NT-3 | Editar compra preserva a competência | compra de 15/01 editada hoje | `Salvar` com mesmo id | `movimentacao.Data == 15/01/2026` | passa |
| NT-4 | `VerificarNovaVersaoAsync` com HTTP abstraído | cliente falso (versão nova/igual/erro 500) | chamar 3× | informa / null / null | passa |
| NT-5 | Vínculo `transacao_id` órfão na edição | manipular banco apagando a transação | editar compra | 1 movimentação no total (sem duplicar) | passa |
| NT-6 | Janela de download possui Owner e cancelamento | correção SF-046 aplicada | abrir e cancelar | janela fecha; sem download parcial em uso | manual/UIA |

## Regressão obrigatória (existente)

- Toda a suíte unitária (136) — em especial T-004*, T-005*, T-006*, T-007, T-008, T-011, TR_*.
- `GuiTests` (12) — interação, acessibilidade, campos de data.
- `CapturaTests` — evidências visuais nos dois temas (comparação contra `docs/auditoria/imagens/`).

## Critérios de aprovação

Nenhuma falha; reconciliação I1/I2/I3 mantidas; capturas novas sem regressão visual de contraste (ContrasteTests + leitura das imagens).
