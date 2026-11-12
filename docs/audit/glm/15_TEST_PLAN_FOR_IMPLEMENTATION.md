# 15 — Plano de testes para implementação (GLM)

Regressão mínima após CADA correção: `dotnet test tests/SmartFinance.Tests` (136) + `dotnet test tests/SmartFinance.GuiTests -c Release` (12, serializado).

## Testes novos por correção

| Correção | Teste | Critério de aceite |
|---|---|---|
| SF-040 | NT-1/NT-3 (competência preservada no banco) | movimentação.Data = competência; T-R conserva |
| SF-041 | Roteiro manual: recusar UAC com banco isolado; log contém o evento | app reaberto OU mensagem específica |
| SF-052 | O próprio `DataTransacoesTests` no CI | 2 execuções CI consecutivas verdes |
| SF-053 | Decisão de distribuição documentada; se in-place: teste de substituição agendada | comportamento conforme decisão |
| SF-055 | UIA: janela de download possui Owner e fecha ao cancelar | manual/UIA |
| SF-056 | Verificar pasta `%TEMP%\SmartFinance-Update` após download | contém apenas o instalador atual |
| F6/NT-4 | `VerificarNovaVersaoAsync` com HTTP abstraído | nova→informa; igual→null; erro→null |
| F2 (regressão financeira) | T-R + F2_* | conservação de dinheiro em todos os cenários |

## Verificação visual

`CapturaTests` (2 testes) após qualquer mudança em `Themes/*` ou `Views/*` — comparar contra `docs/auditoria/imagens/` (regressão de contraste/progresso/cards).
