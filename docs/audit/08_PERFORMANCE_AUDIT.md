# 08 — Auditoria de desempenho

**Medições novas nesta auditoria: nenhuma** (regra: não inventar métricas). Dados válidos vigentes, medidos nas rodadas 3-4 sobre o mesmo binário de arquitetura (mudanças da rodada 4-5 são de UI/persistência pontual, sem novo gargalo identificado):

| Métrica | Valor medido | Método |
|---|---|---|
| Warm start | **69–114ms** (5 execuções, log `startup ok`) | spawn → log, polling 30-50ms |
| Cold start (1ª execução) | 1.678ms (extração de nativos única) | idem |
| RAM em repouso / pós-soak 30min | 132–134MB WS · 84MB Private · estável | Get-Process |
| ListarTodas / FTS sobre 10k registros | < 2s / < 1s (limites de teste) | xUnit com Stopwatch |
| Volume | 10k registros em transação única | idem |

## Análise estática (sem nova medição)

- Consultas: índices cobrem os filtros; FTS5 evita varredura; `CalcularSaldoCarteiraCentavos` executa 3 agregações por chamada — chamada múltipla por renderização de dashboard é aceitável no volume pessoal (regra 14 da auditoria: sem benefício plausível demonstrado para cache).
- Operações síncronas na UI thread: latência < 1ms por operação no volume esperado — não é gargalo.
- `AtualizacaoService`: checagem em Task fora da UI (não bloqueia); download com timeout de 10min.

## Oportunidades (INFO, sem ação recomendada agora)

- Cache de resumos do dashboard se o volume de dados crescer 100×.
- Rotação do `erro.log` já implementada (1MB, 2 gerações).
