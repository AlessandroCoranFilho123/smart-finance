# AUDIT_REPORT — Smart Finance 3.1

**Data:** 10/11/2026 (duas rodadas) · **Alvo:** `main` a partir de `1a8bf16` · **Ambiente:** Windows 10 Pro 19045 x64, .NET 10 (SDK 11 RC), CI GitHub Actions (windows-latest)

## Escopo

Aplicativo desktop Smart Finance 3.0 (C#/WPF/SQLite): camadas Data/Services/Views, suíte xUnit, distribuição por exe único self-contained. As 20 categorias da metodologia foram percorridas; os resultados estão discriminados por categoria abaixo e detalhados nos relatórios irmãos (mesma pasta).

## Metodologia

1. Reconhecimento e inventário (sem alterações) → 2. Execução da suíte existente → 3. Auditoria por categoria com testes de investigação → 4. Priorização → 5. Correções com teste de regressão → 6. Bateria de runtime no exe publicado → 7. Documentação. Distinção rigorosa entre **PASSOU**, **FALHOU** e **NÃO EXECUTADO**; nenhuma evidência inventada; dados do usuário nunca usados destrutivamente.

## Resultados por categoria

| Categoria | Resultado | Síntese |
|---|---|---|
| Funcionalidades | **PASSOU** (com 1 FALHOU → corrigido) | CRUDs, metas, cartão, empréstimos, transferências e exportação validados por 79 testes; AUD-001 (crash na busca de símbolos) confirmado e corrigido |
| Unitários | **PASSOU** | 49 métodos/79 execuções; nenhum teste superficial; 1 asserção frágil corrigida |
| Integração | **PASSOU** | Serviços↔banco com SQLite real; migration do banco Java 2.x testada |
| GUI | **PASSOU** (2ª rodada) | Suíte FlaUI/UIA automação real: dashboard renderiza, navegação nas 5 visões, criação de transação via UI persistida no banco, Escape fecha diálogos, tema alterna com persistência. Roda no CI (runner Windows interativo). Detalhes visuais finos (alinhamento por pixels) seguem manuais |
| Interação | **PASSOU** (2ª rodada) | Teclado (Escape), cliques via padrões UIA (Invoke/Select), foco verificado nos diálogos; persistência após ações validada por consulta direta ao banco |
| Persistência e integridade | **PASSOU** | Transações atômicas com rollback testado; banco vazio/corrompido/tipo inválido falham de forma identificável; Unicode/emoji idempotentes; WAL recupera após 5 kills forçados |
| Desempenho | **PASSOU** | Warm start 69–114ms; RAM 132–134MB estável; 10k registros: listagem e FTS dentro dos limites |
| Estabilidade/concorrência | **PASSOU (recorte)** | 2 instâncias simultâneas OK; kill-restart ×5 OK; memória estável; soak de 30 min com o binário final OK (rodada 2) |
| Qualidade de código | **PASSOU** | 0 warnings, sem código morto, camadas coerentes; duplicação de handlers de erro nos diálogos registrada como dívida aceitável |
| Segurança | **PASSOU** | SQL parametrizado, sem rede, sem segredos, dependências íntegras, logs sem dados sensíveis; riscos aceitos: banco sem criptografia em repouso, exe não assinado |
| Windows (plataforma) | **PASSOU (recorte)** | Caminhos com espaços/Unicode/longos OK; usuário comum OK; Defender/SmartScreen não verificáveis nesta sessão |
| Instalação/distribuição | **PASSOU (2ª rodada)** | Instalador Inno Setup com ciclo instalar→executar→desinstalar testado; dados do usuário preservados; alternativa portátil mantida |
| Compatibilidade | **PARCIAL** | Somente Windows 10 19045 testado; manifest declara suporte a Win11 (sem evidência própria) |
| Acessibilidade | **PARCIAL/INFORMATIVO** | Controles com nomes acessíveis validados pela suíte UIA; leitor de tela real e contraste seguem manuais |
| Localização | **PASSOU** | pt-BR consistente após AUD-003; Unicode/CJK documentados |
| Regressão | **PASSOU** | 79/79 após cada correção |
| Exploratório | **PARCIAL — NÃO EXECUTADO** | Uso como usuário real não foi possível sem automação; investigação de código cobriu sequências incomuns |
| Recuperação/falhas | **PASSOU (recorte)** | Kill forçado durante operação → WAL recupera; banco corrompido falha com erro claro |
| Concorrência | **PASSOU** | Multi-instância e lock com busy_timeout testados (unidade + runtime) |
| Observabilidade | **PASSOU (mínimo viável)** | `erro.log` com stack completo + `startup ok`; rotação pendente (P8) |

## Problemas encontrados e correções

- **AUD-001 (ALTO)** crash na busca de símbolos — corrigido (`TransacaoRepository`).
- **AUD-003 (MÉDIO)** DatePicker en-US — corrigido (`OverrideMetadata` de cultura).
- **AUD-004 (MÉDIO)** estado parcial no diálogo de carteiras — corrigido (`EmTransacao`).
- **AUD-005 (BAIXO)** Escape nos diálogos — corrigido (`IsCancel`).
- **AUD-002/006/007 (BAIXO/INFORMATIVO)** — documentados (limitações/decisões), sem correção.
- Pré-auditoria: crash CRÍTICO de startup já corrigido no commit `1a8bf16` (fora desta janela, referenciado por completude).

Detalhes, evidências e passos: BUG_REPORT.md · REGRESSION_REPORT.md.

## Limitações e nível de confiança

- **Confiança alta** nas camadas de dados/negócio (84 testes com banco real, volumes, corrupção, concorrência), no ciclo de distribuição (instalador testado) e nos fluxos principais de GUI (6 testes de automação real via CI).
- **Confiança parcial** em estética visual fina, leitores de tela reais, Windows 11 físico e cenários de SO (suspensão/troca de usuário) — listados em REMAINING_ISSUES.
- Uma auditoria finita não prova ausência de defeitos; os cenários não executados estão explícitos em TEST_RESULTS.md e REMAINING_ISSUES.md.

## Conclusão

O software está apto ao uso pessoal no escopo testado: os defeitos reproduzíveis encontrados foram corrigidos com regressão verde, e os riscos remanescentes estão documentados com plano de ação.
