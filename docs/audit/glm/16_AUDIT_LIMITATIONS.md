# 16 — Limitações da auditoria GLM

| Item | Motivo | Consequência |
|---|---|---|
| CI do commit auditado (`722ceeb`) | seguia em fila no GitHub Actions durante toda a auditoria | resultado final do CI observado apenas para a etapa GUI que **falhou** (log capturado); compensação: execução local completa verde |
| Causa raiz exata do SF-052 | duas hipóteses (foco do ENTER vs. cultura do parse) indistinguíveis sem instrumentação no runner | correção recomendada cobre ambas |
| Windows 11 / suspensão / hibernação | não simuláveis nesta máquina | pendência mantida (decisão do usuário) |
| Leitor de tela físico | automação limitada | substituído por varredura UIA |
| Fluxo de falha do instalador de atualização (UAC) | requer interação humana | coberto por leitura de fluxo (SF-054/SF-041) |
| Profiling profundo | sem profiler configurado | métricas reutilizadas das rodadas 3-4 |
| Banco 2.x real do usuário | não acessado (regra de proteção) | migrações testadas com bancos sintéticos |
| Auditoria de acessibilidade física (Narrador) | automação limitada | substituída por varredura UIA de nomes acessíveis |

## Viés declarado

Esta auditoria foi executada pelo mesmo agente que implementou o projeto (rodadas 1-4). Mitigações: execução real de tudo que era executável, re-leitura crítica por fluxos (não por intenções), comparação explícita com as auditorias anteriores (`18_PREVIOUS_AUDIT_COMPARISON.md`) e busca ativa por defeitos no código próprio (SF-052 confirmado via CI é um exemplo).
