# 17 — Matriz de rastreabilidade

| Achado | Arquivos | Correção recomendada | Teste/Procedimento | Critério de aprovação |
|---|---|---|---|---|
| SF-040 | CartaoCreditoService.cs, EmprestimoService.cs, 2 diálogos | competência preservada/campo de data | NT-1, NT-3 + T-R | data da movimentação = competência; reconciliação OK |
| SF-041 | MainWindow.xaml.cs, AtualizacaoService.cs | aguardar instalador + reabrir em falha + log | manual (recusar UAC) + log contém evento | app reaberto ou mensagem específica |
| SF-042 | README.md, ARQUITETURA.md, SECURITY.md | atualizar superfície 3.3/3.4 | revisão | docs coerentes com o código |
| SF-043 | Database.cs | decidir FK retroativa ou documentar | NT-2 | decisão registrada + teste |
| SF-044 | EmprestimoService.cs, CartaoCreditoService.cs | tratar vínculo órfão (inserir sem estorno) | NT-5 | sem duplicação |
| SF-045 | (docs/decisão) | documentar categoria de movimentações de cartão | revisão | registrado em DECISOES |
| SF-046 | MainWindow.xaml.cs (CriarJanelaBaixando) | Owner + cancelamento (CancellationToken) | NT-6 | janela modal à principal, cancelável |
| SF-047 | docs/SECURITY.md | documentar limite do canal do hash | revisão | SECURITY descreve o limite |
| SF-048 | AtualizacaoService.cs (BaixarInstaladorAsync) | limpar instalações anteriores na pasta | manual | pasta contém só a versão atual |
| SF-049 | ContrasteTests.cs | alternativa: embutir paletas no teste ou caminho por solução | revisão | teste roda fora do layout padrão |
| SF-050 | TemaManager.cs, AtualizacaoService.cs | convenção de versionamento nos JSONs | revisão | registrado |
| SF-051 | EmprestimoDialog.xaml | rótulo "Previsão de retorno" | revisão visual | rótulo claro |
