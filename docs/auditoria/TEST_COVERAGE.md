# Cobertura de testes por componente — Smart Finance

| Componente / funcionalidade | Testes existentes | Cobertura | Cenários ausentes | Recomendação |
|---|---|---|---|---|
| Parsing monetário (`ValorMonetario`) | 20 casos | Alta | Locale com `.` decimal (en-US em máquina de usuário) | Teste com cultura trocada |
| Registro de transações | 8 testes | Alta | Data no futuro/distante; carteira excluída em paralelo | Baixo risco |
| Edição/exclusão com meta | 6 testes | Alta | Meta deletada entre leitura e gravação (janela mínima) | Aceitável (app single-user) |
| Saldo/transferências de carteira | 7 testes | Alta | Transferência simultânea de duas instâncias | Documentado em REMAINING_ISSUES |
| Bootstrap/migrations do banco | 5 testes | Alta | Migrar banco da 2.x com FTS já criado e triggers antigos | Coberto parcialmente (teste de colunas) |
| Busca FTS5 | 6 testes | Média | Termo com aspas/operadores FTS (`NEAR(`) | Adicionar teste com operadores FTS |
| Cartão de crédito | 6 testes | Alta | Edição mudando nº de parcelas para baixo | Baixo risco |
| Empréstimos | 4 testes | Alta | — | — |
| Metas | 6 testes | Alta | Renomear meta (reflete na descrição de transações antigas? — não, por design) | Documentado |
| Exportadores CSV/TXT | 5 testes | Alta | Arquivo em uso/sem permissão (tratado na UI com IOException) | Teste de UI não aplicável |
| Temas (TemaManager) | 0 | Nula | Troca em runtime, persistência, detecção do sistema | NÃO EXECUTADO — requer UI |
| Diálogos (validação de formulário) | 0 | Nula | Campos vazios/inválidos na UI | NÃO EXECUTADO — requer UI |
| Janela principal/navegação | 0 | Nula | Renderização, foco, redimensionamento | NÃO EXECUTADO — requer UI |
| Logs/diagnóstico | 0 automatizados | Manual | Rotação de log (não implementada) | Ver REMAINING_ISSUES |
