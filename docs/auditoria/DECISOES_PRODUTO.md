# Decisões de produto — auditoria externa (SF-001…SF-037)

Data: 11/11/2026 · Registradas antes da implementação, conforme `16_GLM_HANDOFF.md` §5.5.

| ID | Decisão | Justificativa |
|---|---|---|
| **D-01** (SF-007) | **Bloquear exclusão de meta com valor acumulado > 0.** Mensagem orienta resgatar antes. | Nem destruir dinheiro silenciosamente nem criar transações automáticas que o usuário não pediu. O fluxo de resgate normal mantém tudo consistente (I1/I2). |
| **D-02** (SF-008) | **"Receitas/Despesas do mês" excluem as categorias Aplicar/Resgatar meta** (dinheiro que só muda de lugar). Categoria `Emprestimo` **continua** nos totais (dinheiro que entra/sai de fato). | O indicador passa a representar consumo/renda real. Empréstimo é fluxo externo real. |
| **D-03** (SF-013/030) | **Parsing estrito pt-BR:** vírgula decimal com 1–2 casas (ponto opcional como milhar em grupos exatos de 3); sem vírgula, ponto decimal com 1–2 casas ou ponto de milhar em grupos de 3 (`1.005`, `1.234.567`); `0.500`, `1,234.56`, `1.2.3,4`, `1,005` (sub-centavo) são **recusados** com "Valor inválido". Todos os campos de valor reformatam ao perder o foco. | Valor errado gravado em silêncio é o pior desfecho em um app financeiro; recusar e deixar o usuário corrigir é seguro. `1.005`→milhar mantido (uso comum, já testado). |
| **D-04** (SF-005) | **Bloquear** qualquer operação que deixe carteira com saldo negativo (inclui excluir/reduzir/mover entrada e excluir resgate). Mensagem explica o bloqueio. | Coerente com os invariantes já aplicados em `AtualizarSaldoInicial` e no registro de saídas (I3). Saldo negativo contraria o que a própria UI declara. |
| **D-05** (SF-006) | **Bloquear exclusão/edição de aplicação em meta cujo valor já foi resgatado** (reversão não integral). Mensagem orienta excluir os resgates mais recentes primeiro. | Reversão parcial cria dinheiro do nada (viola I1) — inaceitável. |
| **D-06** (SF-015) | **Manter arredondamento half-up da parcela** (`100/3 → 33,33`); renomear o teste com nome incorreto e documentar que a soma das parcelas pode divergir 1 centavo do total. | Modelar parcelas individuais (resto na 1ª/última) é mudança de produto sem demanda; divergência de 1 centavo é prática de mercado e está visível na UI ("3x de R$ 33,33 · total R$ 100,00"). |
| **D-07** (SF-015b) | **Manter o modelo atual** (compras cadastradas sem baixa de parcelas). | Modelo de parcelas pagas é feature nova, fora do escopo da auditoria. Registrado em REMAINING_ISSUES como evolução. |
| **D-08** (SF-020) | **CSV/TXT mantêm o nome do enum** na categoria e as ordens atuais (CSV crescente, TXT decrescente). Corrigir apenas: injeção de fórmula e captura de `UnauthorizedAccessException`. | Formato testado e estável; mudar rótulos/ordem quebra reimportação e comparabilidade. Os dois defeitos reais são corrigidos. |
| **D-09** (SF-023) | **Manter clamp** de aplicação ao restante da meta (silencioso); **adicionar** pré-seleção da meta ao editar. | Comportamento original do Java, testado; a pré-seleção elimina o atrito que induzia SF-004. |
| — (SF-022) | **Manter delete+insert** na edição. | Mudança de `rowid` só reordena empates de data (cosmético); FTS permanece sincronizado pelos triggers; UPDATE condicional adicionaria risco por ganho estético. |
| — (SF-024) | **Documentar** preferência por instância única; sem `BEGIN IMMEDIATE`. | Uso pessoal; corrida exige duas instâncias simultâneas. Registrado em REMAINING_ISSUES. |
| — (SF-026) | **Remover** `NoWarn CS8602` e o código morto listado; `MetaService` mantém os métodos de progresso (API de domínio exercitada pelos testes). | Qualidade e honestidade da documentação. |
| — (SF-036) | **Manter** Cartão/Empréstimos desconectados de carteiras. | Igual à versão Java; conectar é feature nova (REMAINING_ISSUES). |

## Rodada 4 (11/11/2026) — resposta do usuário às pendências U/F

| ID | Decisão |
|---|---|
| **D-10** (F2) | Compras no cartão e empréstimos **geram movimentação na carteira no ato do cadastro**: compra = saída do valor total na carteira escolhida; "Emprestei" = saída; "Pedi emprestado" = entrada. Exclusão estorna a movimentação; edição a reajusta. A transação gerada fica vinculada (`transacao_id`) à origem. Cartões parcelados: o valor total sai no cadastro (modelo de comprometimento) e "Total em aberto" passa a rastrear a dívida restante com o cartão. |
| **D-11** (F1) | Parcelas pagas são um **campo explícito** (`parcelas_pagas`, migração com default 0 — compatível com 2.x). "Total em aberto" = parcelas restantes × valor da parcela; "Parcelas por mês" soma apenas compras com parcelas restantes. |
| **U1** | **Sem certificado digital** — decisão do usuário (projeto não opera dinheiro real). Pendência encerrada; SmartScreen pode exibir aviso na primeira execução. |
| **U2** | **D-01…D-09 APROVADAS pelo usuário em 11/11/2026** ("concordo com tudo") — regras consolidadas; qualquer revisão futura é revertível mediante solicitação. |
| **F5** | Verificação automática de atualização via GitHub API, **1× por dia**, silenciosa em falha, com consentimento implícito de rede apenas para checagem de versão (nenhum dado é enviado). |
