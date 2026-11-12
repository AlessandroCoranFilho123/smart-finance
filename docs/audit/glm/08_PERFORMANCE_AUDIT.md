# 08 — Auditoria de desempenho (GLM)

**Medições novas: não realizadas nesta auditoria** (sem alterações de código desde as medições das rodadas 3-4; regra: não inventar números).

## Medidas vigentes (válidas — mesmo binário de arquitetura)

| Métrica | Valor |
|---|---|
| Warm start | 69–114ms |
| Cold start | 1.678ms (extração única de nativos) |
| RAM repouso/30min | 132–134MB → estável (soak 30min sem crescimento) |
| 10k registros: listar/FTS | dentro dos limites dos testes (<2s/<1s) |
| Consultas por renderização de dashboard | ~3 agregações SQLite (<1ms cada no volume pessoal) |

## Análise estática (sem gargalo comprovado novo)

- `async/await` correto no F6 (rede fora da UI thread); sem bloqueio de UI.
- Dispose: Database/conexao única; janelas de captura/fechamento best-effort — sem vazamentos observados.
- Virtualização padrão das listas ativa.
- Oportunidade (INFO): cache de agregações do dashboard — sem benefício demonstrado no volume atual.
