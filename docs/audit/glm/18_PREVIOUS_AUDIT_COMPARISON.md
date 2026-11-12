# 18 — Comparação entre auditorias (externa × GLM independente)

Base comparada: auditoria externa estática (SF-001…029 + revisão SF-030…037, sobre a 3.0.0) × auditoria GLM executável (esta, sobre `722ceeb`/3.4.0). As correções das rodadas 2-4 **já aplicadas** mudam o estado de vários achados: para o catálogo atual, "RESOLVIDO" significa que a correção está no código com teste.

## SF-001…029 (auditoria externa estática)

| ID | Externo (3.0.0) | GLM (3.4.0, estado atual) |
|---|---|---|
| SF-001 Transações quebrava (ItemsSource) | VALID CRITICAL | **RESOLVIDO** (rodada 2; GUI navega pela tela) |
| SF-002 combos `ToString()` bruto | VALID HIGH | **RESOLVIDO** (DisplayMemberPath — capturas mostram "Banco"/metas) |
| SF-003 tipo Entrada/Saída pode ficar vazio | VALID HIGH | **RESOLVIDO** (reconciliação Unchecked + defesa no serviço) |
| SF-004 editar meta usa instância obsoleta | VALID HIGH | **RESOLVIDO** (serviço recarrega do banco; T-004/b/c) |
| SF-005 excluir/reduzir entrada deixa negativo | PARTIALLY → MEDIUM (D-04) | **RESOLVIDO** (bloqueio; T-005a/b/c) |
| SF-006 excluir aplicação pós-resgate cria dinheiro | VALID HIGH | **RESOLVIDO** (D-05; T-006/b) |
| SF-007 excluir meta destrói acumulado | VALID MEDIUM (D-01) | **RESOLVIDO** (bloqueio; T-007) |
| SF-008 totais do mês incluem meta | PARTIALLY → LOW (D-02) | **RESOLVIDO** (SQL exclui; T-008/b) |
| SF-009 bootstrap zumbi | VALID MEDIUM | **RESOLVIDO** (try/catch + Shutdown(1); cenário físico assertado) |
| SF-010 URIs relativas em subpastas | INCONCLUSIVE | **RESOLVIDO** (`/Assets/...`; capturas mostram ícones) |
| SF-011 Guid maiúsculo × 2.x | INCONCLUSIVE | **CONFIRMADO E RESOLVIDO** (sondagem: lib grava maiúsculas; `Guid.ToString()` minúsculo; T-011) |
| SF-012 FTS/triggers 2.x divergentes | INCONCLUSIVE LOW | **REFUTADO** (schema real idêntico — sondagem do banco 2.x) |
| SF-013 parsing ambíguo | PARTIALLY · D-03 | **RESOLVIDO** (parsing estrito; testes atualizados) |
| SF-014 overflow | VALID LOW | **RESOLVIDO** (teto em ParseCentavos) |
| SF-015a parcela/teste nome errado | PARTIALLY LOW (D-06) | **RESOLVIDO** (teste renomeado; regra mantida) |
| SF-015b parcelas pagas | (D-07) | **IMPLEMENTADO** (F1/D-11) |
| SF-016 `Hoje` estático | VALID LOW | **RESOLVIDO** (por Atualizar) |
| SF-017 DatePickerTextBox sem PART_ContentHost | INCONCLUSIVE | **CONFIRMADO E RESOLVIDO** (template removido; captura mostra 11/11/2026) |
| SF-018 barra escura só nos diálogos | VALID LOW | **RESOLVIDO** (MainWindow liga/desliga no TemaAlterado) |
| SF-019 cultura do SO no DatePicker | VALID LOW | **RESOLVIDO** (pt-BR fixo) |
| SF-020 CSV injeção/catch | PARTIALLY LOW (D-08 parcial) | **RESOLVIDO** (sanitização + catch; rótulos/ordem mantidos por D-08) |
| SF-021 catch estreito no TemaManager | VALID LOW | **RESOLVIDO** |
| SF-022 rowid na edição | PARTIALLY LOW | **DECISÃO** (mantido — cosmético; FTS sincronizado) |
| SF-023 meta não pré-selecionada; clamp | VALID LOW (D-09) | **RESOLVIDO** (pré-seleção; clamp mantido) |
| SF-024 sem IMMEDIATE; transferência sem edição | PARTIALLY → INFO | **MITIGADO** (mutex F3 impede 2 instâncias); edição de transferência segue como feature (REMAINING) |
| SF-025 TextBlock anula cor selecionada | PARTIALLY LOW | **RESOLVIDO** (TextoNav rebind) |
| SF-026 docs incorretas | VALID INFO | **RESOLVIDO** (rodada 3; rodada 5 re-verificou) |
| SF-027 combinação SQLitePCLRaw; NoWarn | PARTIALLY INFO | **RESOLVIDO** (validação empírica + NoWarn removido) |
| SF-028 duplo clique no vazio; sem Enter | VALID INFO | **RESOLVIDO** (item sob cursor + Enter) |
| SF-029 Tipo × Categoria no serviço | VALID LOW | **RESOLVIDO** (CategoriasValidas no domínio; T-003) |

## SF-030…037 (novos da revisão externa)

| ID | Estado GLM |
|---|---|
| SF-030 `0.500` → R$ 500 | **RESOLVIDO** (parsing estrito; teste) |
| SF-031 janela mínima | **RESOLVIDO** (960×600) |
| SF-032 backup/WAL | **RESOLVIDO** (docs SECURITY orientam; backup interno usa API online) |
| SF-033 interpolação SQL | **REFUTADO** (constantes; comentado no código) |
| SF-034 linha inválida derruba bootstrap | **MITIGADO** (catch SF-009) |
| SF-035 enums 2.x | **VERIFICADO** (teste Sf035) |
| SF-036 cartão/empréstimos × carteiras | **IMPLEMENTADO** (F2/D-10 — além do que o achado pedia) |
| SF-037 cultura nos testes de moeda | **REFUTADO** (cultura explícita) |

## Rodada 5 (SF-038/039) e auditoria executável (SF-040…051)

- SF-038 (crash com metas) e SF-039 (barras vazias): **RESOLVIDOS** com capturas.
- SF-040/041/042…051: **ABERTOS** — são os achados desta auditoria GLM (SF-052 é novo; SF-053/054/055/056 correspondem a SF-053/SF-041/SF-046/SF-048 da auditoria executável, renumerados aqui por serem re-confirmados independentemente).

## Diferenças de interpretação relevantes

1. A auditoria externa chamou de "defeito" itens que eram **decisões de produto** (SF-005/007/008/013/015) — a revisão externa já apontava isso; nesta auditoria GLM foram tratados como decisões registradas (D-01…D-12) e aprovadas pelo usuário.
2. A auditoria externa declarou a auditoria visual como "não executável" e mesmo assim o app tinha 4 defeitos visíveis (SF-001/002/003/010) — nesta auditoria GLM a verificação visual **foi executada** (CapturaTests + capturas) e revelou SF-038/039.
3. Nenhum achado da auditoria externa foi rejeitado como inexistente — 3 foram refutados por sondagem (SF-012/033/037) e o restante confirmado ou decidido.
