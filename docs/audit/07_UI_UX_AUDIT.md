# 07 — Auditoria de interface e UX

## Evidências

19 capturas dos dois temas (`docs/auditoria/imagens/`) cobrindo: dashboard, transações (filtros + histórico), metas (3 faixas de progresso), cartão (vencido/laranja/verde + "pagas X/N"), empréstimos (2 grupos), diálogos (transação, transferência, meta, carteiras), erro de banco corrompido.

## Verificações executadas (automatizadas)

- **Contraste WCAG AA**: 15 pares × 2 temas — todas ≥ 4,5:1 após ajuste (D-12); bordas decorativas relatadas à parte.
- **Acessibilidade UIA** (o que leitores de tela anunciam): 2 varreduras — **todos os controles interativos com nome** (partes `PART_*` de DatePicker excluídas por herdar o rótulo do pai).
- **Fluxos reais**: criação de transação via UI persiste no banco; campos De/Até filtram (digitação → commit → 3→2→0→3); Escape fecha diálogos; tema alterna com persistência; duplo-clique/Enter ativam itens.
- **Uniformidade**: cartões do dashboard com larguras iguais (`*,18,*`) e mesma altura por linha — conferido nas capturas `01-*`.

## Constatações

| ID | Constatação | Severidade |
|---|---|---|
| — | Cores/contraste nos dois temas: consistente após D-12; escuro meio azulado conforme pedido | (positivo) |
| — | Estados hover/pressed/disabled/focus presentes nos estilos (BotãoPrimario/Secundario/Nav/campos) | (positivo) |
| — | Feedback de operações: exportação, exclusões e atualização têm mensagens; salvamentos fecham o diálogo (feedback = lista atualizada) | (positivo) |
| SF-045 | Movimentações de cartão usam categoria `Compras` — aparecem misturadas a lançamentos manuais em filtros por categoria (comportamento D-10; documentar na UI seria melhoria) | INFO |
| SF-051 | Rótulo "Data de pagamento" do empréstimo é ambíguo (é a data esperada de **retorno**; a saída do dinheiro ocorre no cadastro) | LOW |
| — | Enter ativa itens; Tab percorre; indicadores de foco presentes (estilos de campo com borda accent) | (positivo) |
| — | Duplo clique no vazio não abre mais (SF-028 corrigido) | (positivo) |

## Não executado (limitação)

Avaliação estética fina (alinhamento por pixel), Narrador físico, DPI de múltiplos monitores — cobertos por capturas estáticas onde possível; resto em `REMAINING_ISSUES.md` (F4).
