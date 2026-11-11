# 16 — Handoff para o GLM

**O GLM deverá consultar os documentos da auditoria antes de alterar qualquer código. Deverá investigar o contexto de cada achado, corrigir os problemas individualmente ou em grupos tecnicamente relacionados, executar os testes correspondentes e verificar regressões. Não deverá assumir que a recomendação do auditor é necessariamente a única implementação possível.**

O GLM deverá preservar funcionalidades corretas e evitar refatorações não relacionadas aos problemas identificados.

## 1. O que é
Smart Finance 3.0: app desktop Windows de finanças pessoais (C# / WPF / .NET 10 / SQLite local em `%APPDATA%\SmartFinance\financas.db`). Reescrita de uma versão JavaFX 2.x; o **schema do banco é compatível com a 2.x** (mesmas tabelas, IDs de carteiras padrão, datas ISO, valores em centavos).

## 2. Estrutura
`Views (WPF)` → `Services (regras)` → `Data (SQLite)`. Composição em `App.OnStartup` (`Servicos`). Uma conexão SQLite única; `Database.EmTransacao` é reentrante. Linhas de lista são view-models pré-formatados (`Views/Linhas.cs`) com templates em `Themes/Templates.xaml`. Testes em `tests/SmartFinance.Tests` (xUnit, banco real em pasta temporária via `TestApp`); **não há teste de UI**.

## 3. Estado atual
- Suíte existente: 79 execuções, reportadas verdes pela auditoria anterior (não reverificado nesta auditoria).
- **Esta auditoria foi estática.** Nada foi compilado ou executado. Leia `15_AUDIT_LIMITATIONS.md`.
- 29 achados em `12_ISSUES_CATALOG.md`: 1 CRITICAL, 5 HIGH, 7 MEDIUM, 13 LOW, 3 INFO.

## 4. Prioridades
SF-001 → SF-010 → SF-002 → SF-003 → SF-009 (Etapa 0); depois SF-004, SF-005, SF-006, SF-007, SF-008, SF-013 (Etapa 1). Demais conforme `13_REMEDIATION_ROADMAP.md`.

## 5. Sequência de execução
1. Ler `00`, `12`, `13`, `14`, `15`.
2. Rodar `dotnet build` e `dotnet test` e **registrar o resultado real** (baseline).
3. Rodar o smoke manual (seção A de `14`) com um banco **de teste** (`SMARTFINANCE_DB_PATH`). Marcar cada achado de UI como confirmado ou refutado; refutados são descartados e anotados.
4. Para cada achado: reproduzir (teste que falha) → corrigir → teste verde → `dotnet test` completo.
5. Decisões de produto pendentes (SF-007, SF-008, SF-013): registrar a decisão num arquivo antes de implementar; não decidir em silêncio.
6. Atualizar documentação (Etapa 6) por último.

## 6. Cuidados especiais
- **Nunca** abrir, migrar ou testar contra o `financas.db` real. Use cópia em `%TEMP%`.
- `TransacaoService.Atualizar` é delete+insert dentro de `EmTransacao`; qualquer mudança deve preservar o rollback completo (testes `Atualizar_ComValidacaoFalha…` e `AtualizacaoComposta…`).
- Objetos `Meta` vindos da UI são **estado potencialmente obsoleto**; o banco é a fonte da verdade (SF-004).
- Valores monetários sempre `long` em centavos; não introduzir `double`/`float`.
- Não alterar nomes de tabelas/colunas/IDs padrão: quebra compatibilidade com bancos 2.x.
- Mudanças de regra (clamp, parsing) alteram testes existentes: alterar o teste só com decisão registrada.
- FTS5 usa tabela de conteúdo externo (`rowid`); mexer em `Atualizar` ou `Database.CriarBuscaTextual` exige rodar os testes de busca.

## 7. Testes obrigatórios
`dotnet test` completo + T-001…T-R de `14_TEST_PLAN_FOR_GLM.md` + smoke A nos dois temas.

## 8. Critério de "concluído" por achado
(1) teste automatizado ou passo manual do roteiro que falhava e agora passa; (2) suíte completa verde; (3) linha correspondente da matriz (`14`, seção D) preenchida com o resultado real; (4) nenhuma alteração fora do escopo do achado.
