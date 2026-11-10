# Relatório de desempenho — 10/11/2026

Ambiente: Windows 10 19045 x64, SSD, exe autocontido com ReadyToRun (`dist/SmartFinance.exe`).

## Inicialização (medida: spawn do processo → `startup ok` no log, polling 30–50ms — aproximação ±50ms)

| Cenário | Tempo |
|---|---|
| Cold start absoluto (1ª execução pós-download, inclui extração de nativos para `%TEMP%\.net`) | 1.678ms |
| Warm start (mediana de 5) | **69–114ms** |
| Warm start com banco real de ~130KB | dentro da mesma faixa |

A extração de nativos acontece uma vez por versão do executável; execuções seguintes reutilizam o cache.

## Memória

| Condição | WorkingSet | Private |
|---|---|---|
| Em repouso (dashboard) | 134MB | 84MB |
| Após 30s ocioso pós-ciclos de uso | 132MB | — |

Sem evidência de crescimento descontrolado no recorte testado (30s; soak longo registrado como não executado).

## Consultas com volume (xUnit, banco em memória de arquivo temporário)

| Operação | Volume | Limite do teste | Resultado |
|---|---|---|---|
| Inserção em massa (transação única) | 10.000 registros | — | PASSOU |
| `ListarTodas` | 10.000 | < 2.000ms | PASSOU |
| Busca FTS (`compra 99`) | 10.000 | < 1.000ms | PASSOU |

O índice FTS5 + índices de data/tipo/categoria sustentam o uso pessoal com folga; volumes 100× maiores não foram testados (fora do escopo de uso do aplicativo).

## Gargalos conhecidos (informativo)

- Listas renderizam todos os itens visíveis de uma vez (`VirtualizationMode=Recycling` ativo; painel padrão VirtualizingStackPanel) — adequado ao volume esperado.
- Todas as operações de banco são síncronas na thread de UI: com volume pessoal (milhares de registros) o custo é < 1ms por operação; não é gargalo no escopo atual.

## Comparação com a versão anterior (JavaFX 2.x)

Não há medição controlada da versão JavaFX nesta sessão (NÃO EXECUTADO). Diferença arquitetural: eliminação do boot da JVM + JavaFX toolkit, principal componente do tempo de abertura da versão anterior (segundos → dezenas de ms).
