# 10 — Cobertura de testes e validação (GLM)

## Execução (commit `722ceeb`, nesta auditoria)

| Suíte | Local | CI (runner GitHub) |
|---|---|---|
| Unitária | **136/136** | **136/136** |
| GUI/UIA | **12/12** | **11/12 — 1 falha (SF-052)** |

## Avaliação por qualidade (não por contagem)

- Isolamento: banco real por teste em `%TEMP%` (TestApp) — sem ordem-dependência unitária.
- GUI: fixture única serializada — ordem intrínseca (alfabética por classe) tolerada; **sensível a foco/cultura no runner** (SF-052).
- Asserts: verificam valores concretos e estado no banco (não mocks para integração).
- Categorias presentes: unit, integração, negócio, persistência, recuperação (kill/WAL), concorrência, UIA, acessibilidade (UIA), datas, captura visual, distribuição (instal/desinst), performance (limites), segurança (estática).

## Lacunas (sem implementar aqui — plano em `15_TEST_PLAN_FOR_IMPLEMENTATION.md`)

1. Competência das movimentações (conecta SF-040) — NT-1/NT-3.
2. Migração 2.x de compra/emprestimo — NT-2.
3. `VerificarNovaVersaoAsync` com HTTP abstraído — NT-4.
4. Vínculo `transacao_id` órfão — NT-5.
5. DataTransacoes robusto a cultura/foco (SF-052) — correção do próprio teste.
