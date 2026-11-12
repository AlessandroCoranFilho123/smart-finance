# 00 — Sumário executivo (auditoria GLM independente)

**Projeto:** Smart Finance 3.4.0 · **Commit auditado:** `722ceeb` (main) · **Data:** 12/11/2026
**Tipo:** auditoria independente **com execução** (build, suítes completas, CI analisado, sondagens locais)
**Código alterado:** nenhum · **Única alteração:** criação de `docs/audit/glm/`

## Números

| Item | Valor |
|---|---|
| Build Debug/Release | 0 erros, 0 warnings (ambos) |
| Testes executados | **136 unitários + 12 GUI — 148/148 aprovados localmente** |
| CI no runner GitHub | `dc4e9a0` verde · **`722ceeb` FALHOU** (1 teste GUI) · `4810844` em fila |
| Achados novos | **6**: 1 MEDIUM (CI vermelho em main) · 2 LOW · 3 INFO/MEDIUM |
| Achados anteriores revalidados | SF-001…029 + SF-030…051 — matriz em `18_PREVIOUS_AUDIT_COMPARISON.md` |
| Dependências vulneráveis | 0 |

## Estado geral

Base sólida: arquitetura em camadas coerente, invariants financeiros I1-I3 cobertos por testes executados, WCAG AA verificado programaticamente, migrações compatíveis 2.x, segurança sem superfície de rede além do F6 (com verificação de hash). **O achado mais grave desta auditoria é de processo, não de produto**: o main está **vermelho no CI** — `DataTransacoesTests` não commita a data digitada no runner (sensível a foco/cultura; local passa). Correção de 1 linha recomendada no teste (ver `13_ISSUES_CATALOG.md` SF-052).

## Principais riscos

1. **CI vermelho em main** (SF-052, MEDIUM, CONFIRMED por log) — bloqueia confiança no pipeline.
2. **SF-040 (herdado, HIGH)**: competência das movimentações de cartão/empréstimo fixada na data de criação — pendente de correção.
3. **SF-041 (herdado, MEDIUM)**: falha de instalação da atualização deixa o usuário sem app aberto.
4. **SF-053 (novo, MEDIUM)**: a auto-atualização usa sempre o instalador — usuários do **exe portátil** migram para Program Files e a cópia portátil fica obsoleta (decisão de produto necessária).
5. Integridade financeira: sem novas divergências — invariantes I1/I2/I3 executados e verdes.

## Conclusão

O projeto **está pronto para iniciar a etapa de correções**: os riscos remanescentes estão catalogados com evidência, plano e critérios. Nenhuma correção foi implementada nesta auditoria.
