# 15 — Limitações da auditoria

| Item | Motivo | Consequência |
|---|---|---|
| CI do commit `4810844` | seguia **em fila** no GitHub Actions durante toda a auditoria (runner ocupado); último CI concluído verde: `dc4e9a0` | o CI do commit auditado não foi observado concluído; compensado por execução local completa (136+12) no mesmo commit |
| Avaliação estética fina (alinhamento por pixel) | requer análise visual subjetiva | coberta por capturas + leitura, sem precisão de pixel |
| Windows 11 / suspensão / hibernação | não simuláveis nesta máquina | pendência mantida por decisão do usuário ("um dia") |
| Leitor de tela físico (Narrador/NVDA) | automação limitada | substituído por varredura UIA (o que os leitores consomem) |
| Fluxo de falha do instalador (UAC recusado) | requer interação humana com UAC | coberto por leitura de fluxo (SF-041); não reproduzido |
| Hash do instalador de versões antigas | notas 3.3.x não publicavam hash do setup | verificação de hash aplica-se a releases futuras |
| Banco 2.x real do usuário | não acessado (regra de segurança) | sondagem usou cópia em rodada anterior; migração de compra/emprestimo testada com banco sintético |
| Profiling profundo | ferramentas de profiler não configuradas | métricas reutilizadas das rodadas 3-4 (mesma arquitetura) |

## Hipóteses pendentes

1. O CI de `4810844` conclui verde após a fila (esperado — local verde e mudanças cobertas por testes; verificar em `actions/`).
2. Se o usuário edita compras/emprestimos antigos com frequência, SF-040 é percebido de imediato no histórico.
