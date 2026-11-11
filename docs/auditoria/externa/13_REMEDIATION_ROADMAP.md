# 13 — Roadmap de correção

Ordem recomendada, com dependências técnicas. Cada etapa fecha com `dotnet test` verde + os testes novos citados.

## Etapa 0 — Smoke manual e bloqueios de uso
- **IDs:** SF-001, SF-002, SF-003, SF-010, SF-009.
- **Objetivo:** app abre, navega por todas as telas, diálogos mostram dados legíveis, tipo nunca fica vazio, falha de startup encerra o processo.
- **Dependências:** rodar antes o smoke da seção A de `14_TEST_PLAN_FOR_GLM.md` para confirmar/descartar SF-001/002/003/010.
- **Arquivos:** `TransacoesView.xaml(.cs)`, `TransacaoDialog.xaml(.cs)`, `EmprestimoDialog.xaml(.cs)`, `TransferenciaDialog.xaml`, `Views/Dialogs/*.xaml`, `Templates.xaml`, `App.xaml.cs`.
- **Ordem:** SF-001 → SF-010 → SF-002 → SF-003 → SF-009.
- **Riscos:** estilos de `ComboBox`/`ToggleButton` customizados.
- **Conclusão:** smoke A passa sem exceção; ícones aparecem.
- **Regressão:** filtros, edição de transação, carregamento em modo edição.

## Etapa 1 — Integridade financeira e dados
- **IDs:** SF-004, SF-005, SF-006, SF-007, SF-008, SF-013, SF-014, SF-029, SF-011, SF-012.
- **Objetivo:** invariante "total de dinheiro só muda por entrada/saída explícita"; nenhum saldo negativo por edição/exclusão; parsing estrito.
- **Dependências:** SF-004 antes de SF-006 (mesmo método). SF-005 e SF-006 compartilham a regra "reversão integral ou bloqueio" — decidir uma vez. SF-011/012 só após verificar uma **cópia** do banco real.
- **Arquivos:** `TransacaoService.cs`, `Meta.cs`, `MetaService.cs`, `MetaDialog.xaml.cs`, `TransacaoRepository.cs`, `ValorMonetario.cs`, `Database.cs`, repositórios, `DashboardView.xaml.cs`.
- **Decisões de produto a registrar antes de codar:** (a) excluir meta com saldo: resgatar ou bloquear; (b) `Emprestimo` entra em receitas/despesas do mês?; (c) padrão de entrada numérica aceito.
- **Riscos:** alterar regras muda totais históricos; testes existentes de clamp e `1,005`.
- **Conclusão:** T-004…T-013 passam; teste de reconciliação (soma total constante) passa.
- **Regressão obrigatória:** toda a classe `TransacaoServiceTests`, `CarteiraServiceTests`, `ValorMonetarioTests`.

## Etapa 2 — Problemas funcionais
- **IDs:** SF-016, SF-020, SF-021, SF-023, SF-024.
- **Dependências:** SF-023 depois de SF-004.
- **Conclusão:** testes T-016, T-020, T-021 e roteiro manual.

## Etapa 3 — Arquitetura e qualidade
- **IDs:** SF-015, SF-022, SF-026 (código morto), SF-027.
- **Observação:** remover código morto **só** depois de confirmar que não é usado em testes; manter `NoWarn` ou corrigir os avisos (decidir).
- **Conclusão:** build sem avisos com `CS8602` reativado (ou decisão documentada).

## Etapa 4 — Interface e experiência
- **IDs:** SF-017, SF-018, SF-019, SF-025, SF-028.
- **Conclusão:** verificação visual nos dois temas (roteiro A).

## Etapa 5 — Desempenho
- Sem achados novos. Não repetir medições sem motivo; reexecutar T-volume (10k) após Etapa 1.

## Etapa 6 — Documentação
- **IDs:** SF-026.
- Corrigir `TEST_INVENTORY.md` (5 métodos), `ARQUITETURA.md` (barra de título), `AUDIT_REPORT.md` (status da GUI), README (testado só em Win10); unificar `docs/auditoria` e `docs/audit`.
