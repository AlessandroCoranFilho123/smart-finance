# 00 — Sumário executivo

**Projeto:** Smart Finance 3.0.0 (C# / WPF / .NET 10 / SQLite)
**Data:** 10/11/2026 · **Tipo:** auditoria **estática** (leitura integral do código, XAML, testes e docs entregues)
**Código alterado:** nenhum. **Compilação e testes:** **não executados** (sem acesso ao repositório em disco/SDK nesta sessão).

## Números

| Item | Valor |
|---|---|
| Arquivos lidos | 85 (fontes, XAML, testes, csproj, docs) |
| Funcionalidades inventariadas | 9 (transações, carteiras/transferências, metas, cartão, empréstimos, dashboard, busca/filtros, exportação, temas) |
| Testes executados por esta auditoria | 0 (suíte de 79 execuções existe; resultado da auditoria anterior **não reverificado**) |
| Achados | **29**: 1 CRITICAL · 5 HIGH · 7 MEDIUM · 13 LOW · 3 INFO |
| Confiança | nenhum "Confirmado" (nada foi reproduzido); 21 "Alto" (estático), 6 "Médio", 2 "Baixo" |

## Estado geral

A camada de dados/regras (Services + Data) é razoável e bem testada nos fluxos felizes. O risco real está em **três lugares que os testes não alcançam**: (1) a UI inteira (nenhum teste), onde há um provável crash da tela Transações (SF-001) e combos ilegíveis (SF-002); (2) fluxos de **edição/exclusão** com metas e entradas, que violam o invariante "dinheiro não é criado nem destruído" (SF-004/005/006/007); (3) entrada de valores, que aceita formatos ambíguos com resultado silenciosamente errado (SF-013).

## Principais riscos (ordem de prioridade)

1. **SF-001** Tela Transações lança exceção ao abrir.
2. **SF-003** Tipo Entrada/Saída pode ser invertido sem o usuário perceber.
3. **SF-004 / SF-006 / SF-005** Edição e exclusão corrompem saldo de meta e criam dinheiro/saldo negativo.
4. **SF-002** Impossível distinguir metas e carteiras nos combos.
5. **SF-013** `1,234.56` vira R$ 1,23.
6. **SF-009** Falha na inicialização deixa processo zumbi.
7. **SF-011/012** Compatibilidade com banco 2.x (a promessa "sem migração manual") tem lacunas de teste que podem esconder falhas reais — **verificar com cópia do banco real**.

## Situação por área

- **Integridade financeira:** insatisfatória em edição/exclusão (SF-004–008, 013). Criação simples está correta.
- **Segurança:** sem superfície de rede, SQL parametrizado (confirmado por leitura). Riscos: injeção de fórmula em CSV (SF-020, LOW); banco sem criptografia (aceito, documentado).
- **Documentação:** contém afirmações incorretas (SF-026). A auditoria anterior declarou "GUI não executada", mas classificou o conjunto como apto; a leitura do XAML indica que o aplicativo **não** deve ser considerado apto até SF-001/002/003 serem verificados.

## Limitações

Ver `15_AUDIT_LIMITATIONS.md`. Em resumo: nada foi executado; números de linha indisponíveis; achados de UI dependem de confirmação visual; SF-011/012 dependem do banco legado real.

## Recomendação

Antes de qualquer outra correção, rodar um **smoke test manual de 10 minutos** (roteiro em `14_TEST_PLAN_FOR_GLM.md`, seção A) para converter SF-001/002/003/010/017/025 de "Alto/Médio" em "Confirmado" ou descartá-los. Depois seguir o roadmap em `13_REMEDIATION_ROADMAP.md`.

## Mapa dos documentos

| Arquivo | Conteúdo |
|---|---|
| `00_EXECUTIVE_SUMMARY.md` | este |
| `12_ISSUES_CATALOG.md` | todos os achados, formato completo |
| `13_REMEDIATION_ROADMAP.md` | etapas, ordem, dependências |
| `14_TEST_PLAN_FOR_GLM.md` | smoke manual, testes novos, **matriz de rastreabilidade** |
| `15_AUDIT_LIMITATIONS.md` | o que não foi validado |
| `16_GLM_HANDOFF.md` | instrução de transferência |

**Consolidação:** inventário, arquitetura, banco, segurança, UI, desempenho, dependências e documentação (docs 01–11 do pedido) estão dentro do catálogo (achados por categoria) e deste sumário; não foram gerados como arquivos separados porque não há conteúdo novo além dos achados. Os dados de desempenho existentes são os da auditoria anterior e **não foram medidos de novo**.
