# Pendências e riscos remanescentes — auditoria 10/11/2026

| # | Pendência | Motivo de não resolvida | Impacto | Próxima ação recomendada |
|---|---|---|---|---|
| P1 | Testes de GUI automatizados (renderização, interação, temas visuais, DPI, redimensionamento) | Ambiente sem ferramenta de automação de desktop (mouse/teclado/captura) | Cobertura de UI depende de verificação manual | Usar WinAppDriver/FlaUI ou Appium Win32 numa máquina com suporte a automação; adicionar ao pipeline |
| P2 | Busca FTS não encontra infixo CJK (`中文` não acha `说中文`) | Limitação do tokenizer `unicode61` (herdada da versão Java) | Irrelevante para pt-BR; relevante para buscas em CJK | Migrar para `tokenize='trigram'` (SQLite ≥ 3.34) se busca CJK for requisito |
| P3 | Soak prolongado (>30min), suspensão/hibernação, troca de usuário, hibernação | Tempo e controle de SO indisponíveis nesta sessão | Baixo — app é acionado pelo usuário, sem operações em segundo plano | Executar manualmente uma sessão de uso de 1h |
| P4 | Executável sem assinatura digital | Certificado de assinatura tem custo | Aviso do SmartScreen/antivírus na 1ª execução | Assinar com certificado code-signing quando o projeto demandar distribuição mais ampla |
| P5 | Instalador tradicional (MSIX/Inno Setup), auto-atualização e associação de arquivos | Distribuição atual é exe único portável; sem mecanismo de update embutido | Usuário precisa baixar nova versão manualmente | Avaliar Inno Setup + verificação de versão via GitHub API em versão futura |
| P6 | Backup embutido do banco | Fora do escopo da versão 3.0 | Usuário depende de cópia manual do `financas.db` | Backup automático diário para subpasta com retenção |
| P7 | Pipeline de CI (GitHub Actions: build + testes a cada push) | Não configurado ainda | Regressões dependem de execução local | Workflow `dotnet test` no GitHub Actions |
| P8 | Rotação/tamanho máximo do `erro.log` | Volume esperado é mínimo | crescimento lento por append | Truncar se > 1MB na inicialização |
| P9 | Compatibilidade com Windows 11 / outros builds / multi-monitor / DPI por monitor | Só foi testado no Windows 10 19045 desta máquina | WPF é estável nessas plataformas; sem evidência própria | Rodar num Windows 11 quando disponível; `PerMonitorV2` no manifest se nitidez variar entre monitores |
| P10 | Acessibilidade (AutomationProperties, ordem de foco explícita, tooltips) | Não implementado | Usuários de leitor de tela podem ter experiência degradada | Adicionar AutomationProperties.Name nos controles principais e revisar ordem de Tab |
