# 07 — Auditoria WPF e interface (GLM)

## XAML/bindings (leitura + execução)

- Bindings revisados: nenhum TwoWay contra propriedade somente leitura (SF-039 corrigido — barras preenchem nas capturas); `Mode=OneWay` explícito no progresso.
- Sem recursos inexistentes (build compila todos os DynamicResource; carga confirmada pelas capturas).
- Sem códigos mortos de template; estilos únicos por controle; `Condition` de MultiTrigger corrigido.
- DatePicker: template padrão do WPF restaurado (data visível — SF-017); Language pt-BR fixo app-wide (SF-019).

## Estados e eventos (executados via UIA)

- Checked/Unchecked com reconciliação manual (SF-003) — nunca sem tipo marcado.
- ValuePattern + Enter commita datas (testes de filtro); LostFocus reformata valor em todos os campos de moeda.
- Duplo clique com item sob cursor + Enter (SF-028); Escape fecha diálogos (IsCancel).
- Lista sem item selecionado no duplo-clique: não abre (corrigido).

## Layout/responsividade

- MinWidth 960/MinHeight 600; cartões iguais (`*,18,*`); `ScrollViewer` no dashboard.
- PerMonitorV2 no manifest.
- Não verificado: DPI multi-monitor real, resoluções < 960px, textos extensos extremos (limitação).

## Constatações (sem novo defeito de produto confirmado)

| ID | Constatação | Severidade |
|---|---|---|
| — | Contraste WCAG AA nas duas paletas (ContrasteTests) | (positivo) |
| — | Nomes acessíveis em todos os controles interativos (AcessibilidadeTests) | (positivo) |
| — | Bindings `async void` no MainWindow (`VerificarAtualizacaoSeNecessario`) — exceções pós-await caem no handler global; fluxo de rede silencioso em falha | (positivo/observado) |
| — | Janela "Baixando atualização" sem Owner e não cancelável (SF-046 herdado) | LOW (herdado) |

## Lacuna

Avaliação estética por pixel e Narrador físico — não executáveis aqui (limitação 16_AUDIT_LIMITATIONS).
