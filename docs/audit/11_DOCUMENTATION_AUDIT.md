# 11 — Auditoria de documentação

| Documento | Estado | Divergências |
|---|---|---|
| `README.md` | **Desatualizado (SF-042)** | Não documenta: parcelas pagas (F1), movimentações de cartão/empréstimo (F2), instância única (F3), auto-atualização (F6), acessibilidade (F4); badge CI presente ✓; screenshots presentes ✓ |
| `docs/ARQUITETURA.md` | **Desatualizado (SF-042)** | Sem D-10 (movimentações de cartão/empréstimo), F1 (parcelas pagas), F3 (mutex), F6 (AtualizacaoService + relaunch pelo instalador), particionamento BrushAccent/Texto |
| `docs/SECURITY.md` | **Desatualizado (SF-042)** | Não menciona a rede do F6 (checagem + download com verificação de hash) nem o F3 |
| `docs/auditoria/*` | Atualizado até rodada 4 | Rodada 5 registrada em `docs/audit/` (esta) |
| `docs/SECURITY.md` backup/WAL | Correto | — |
| Comandos do README (`publish` com flags) | Corretos | — |
| Nenhuma feature documentada inexistente; nenhum diagrama quebrado | — | — |

## Ação recomendada (sem executar nesta etapa)

Atualizar os três documentos para a superfície atual: tabela de recursos 3.3/3.4, D-10/D-11/D-12 na arquitetura (movimentações vinculadas, mutex, divisão de accent), seção de atualização automática na segurança (TLS + hash publicado no mesmo canal — SF-047).
