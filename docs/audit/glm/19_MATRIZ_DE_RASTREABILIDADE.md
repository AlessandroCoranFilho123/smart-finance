# 19 — Matriz de rastreabilidade (GLM)

| Achado | Arquivos | Correção recomendada | Teste/Procedimento | Critério de aprovação |
|---|---|---|---|---|
| SF-052 | DataTransacoesTests.cs | commit por LostFocus/teclado determinístico | o próprio teste no CI | 2 execuções CI verdes |
| SF-053 | MainWindow.xaml.cs, AtualizacaoService.cs, .iss | decisão portátil vs. instalado (in-place ou migração) | roteiro de atualização portátil | conforme decisão |
| SF-054 | MainWindow.xaml.cs, AtualizacaoService.cs | aguardar instalador + reabrir/logar em falha | manual (recusar UAC) + log | app reaberto ou mensagem |
| SF-055 | MainWindow.xaml.cs (CriarJanelaBaixando) | Owner + cancelamento | UIA/manual | janela modal e cancelável |
| SF-056 | AtualizacaoService.cs, CapturaTests.cs | limpeza de pasta/destino configurável | verificação de pasta | sem acúmulo |
| (herdados) SF-040…051 | ver `docs/audit/12_ISSUES_CATALOG.md` | conforme TRIAGEM/DECISOES | suítes existentes + NT-* | conforme catálogo executável |
| F1/F2/F6 (funcionalidades) | CartaoCreditoService, EmprestimoService, AtualizacaoService | — | F1_*/F2_*/F6_* + GUI + capturas | verdes e visuais OK |
