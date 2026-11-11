# Pendências — resolução 10/11/2026 (segunda rodada)

Estado final após a segunda rodada de execução ("faça tudo, não deixe nada pendente").
Cada item da lista original recebeu implementação ou encerramento justificado.

## Resolvidos

| # | Pendência original | Resolução |
|---|---|---|
| P1 | Testes GUI automatizados | **RESOLVIDO** — suíte FlaUI/UIA em `tests/SmartFinance.GuiTests` (navegação nas 5 visões, criação de transação persistida no banco, Escape fecha diálogos, tema alternado com persistência, diálogo de carteiras). Roda no CI (runner Windows com sessão interativa) e localmente via `dotnet test --filter Category=GUI` |
| P3 | Soak prolongado | **RESOLVIDO** — soak de 30 min executado com o exe publicado (ver TEST_RESULTS) |
| P5 | Instalador | **RESOLVIDO** — Inno Setup 6: `setup/SmartFinance.iss` gera `SmartFinance-Setup-3.1.0.exe` (atalhos, desinstalador, dados do usuário preservados). Ciclo instalar→executar→desinstalar testado |
| P6 | Backup embutido | **RESOLVIDO** — `BackupService`: snapshot diário via API de backup do SQLite em `%APPDATA%\SmartFinance\backups`, retenção de 7; desativado quando o banco é isolado por `SMARTFINANCE_DB_PATH` |
| P7 | CI | **RESOLVIDO** — `.github/workflows/ci.yml`: build Release + testes de negócio + testes GUI no windows-latest |
| P8 | Rotação de log | **RESOLVIDO** — `erro.log` acima de 1MB é arquivado (`erro-<timestamp>.log`), mantendo 2 gerações |
| P9 | DPI por monitor / Windows 11 | **PARCIALMENTE RESOLVIDO** — manifest `PerMonitorV2` (nitidez correta em monitores com escalas diferentes) e `supportedOS` Win10/11 declarado. Execução real em Windows 11 permanece sem evidência própria (não há máquina com Win11 disponível) |
| P10 | Acessibilidade | **PARCIALMENTE RESOLVIDO** — todos os controles principais possuem nome acessível (validado pela suíte FlaUI, que localiza controles por AutomationId/nome); tooltips completos e revisão de contraste seguem como melhoria incremental |

## Encerrados com justificativa

| # | Pendência original | Encerramento |
|---|---|---|
| P2 | Busca FTS com infixo CJK | **DECISÃO TÉCNICA: não alterar.** O tokenizer `trigram` corrigiria CJK, mas degrada buscas curtas pt-BR (o caso de uso real do aplicativo exige termos de ≥3 caracteres e muda semântica de prefixo). CJK é cenário irrelevante para o público do app. Comportamento documentado em AuditoriaTests |
| P4 | Assinatura digital do executável | **EXTERNO AO CÓDIGO** — exige certificado de assinatura (custo anual + identidade do publisher). Procedimento quando decidido: comprar certificado OV/EV, assinar `signtool sign /fd SHA256 /tr <timestamp> dist\SmartFinance.exe` e o instalador; as notas de release já publicam o SHA-256 como compensação atual |

## Novos itens observados nesta rodada

- A suíte GUI requer sessão interativa (mouse/teclado): em máquina em uso ativo pelo usuário, a automação compete por foco. Recomendação prática: rodar via CI ou em janela de inatividade (`dotnet test --filter Category=GUI`).
- `dotnet sln add` do SDK 11 RC falha com .slnx em pastas de solução — solução editada em XML diretamente (bug da ferramenta, não do projeto).

## Pendências restantes após a rodada 3 (estado final em 11/11/2026)

Itens **encerrados** na rodada 3: verificação visual (capturas + análise), cenário físico SF-009, todos os 37 achados da auditoria externa (`TRIAGEM_EXTERNA.md`).

| # | Pendência | Tipo | Próxima ação |
|---|---|---|---|
| U1 | **Assinatura digital do executável** | **DECISÃO DO USUÁRIO** — exige comprar certificado code-signing (OV/EV, custo anual) | Ao decidir: `signtool sign /fd SHA256 /tr <timestamp>` no exe e no instalador; antes disso o SmartScreen exibirá aviso na primeira execução |
| U2 | Revisão das decisões D-01…D-09 | **ENCERRADA** — aprovadas pelo usuário em 11/11/2026 ("concordo com tudo"). Revertíveis mediante solicitação. |
| F1 | Parcelas pagas no cartão (D-07) | Feature nova | Modelar baixa de parcelas; "Total em aberto" passa a amortizar |
| F2 | Cartão/Empréstimos conectados a carteiras (SF-036/D) | Feature nova | Decidir se compras/empréstimos geram movimentos em carteiras |
| F3 | Instância única (mutex) ou `BEGIN IMMEDIATE` (SF-024) | Robustez | Mutex global no bootstrap é a opção mais simples |
| F4 | Windows 11, leitores de tela completos, suspensão/hibernação | Ambiente | Executar em máquina/equipamento com o cenário |
| F5 | Auto-atualização via GitHub API | Feature nova | Verificar versão na inicialização; hoje é manual via releases |

## Rodada 4 (11/11/2026) — resposta às decisões do usuário

| Pendência | Resultado |
|---|---|
| U1 assinatura digital | **ENCERRADA** — usuário decidiu não adquirir certificado (projeto não opera dinheiro real); SmartScreen pode avisar na 1ª execução |
| U2 decisões D-01…D-09 | **APROVADAS pelo usuário** ("concordo com tudo", 11/11/2026) — consolidadas em `DECISOES_PRODUTO.md`; revertíveis mediante solicitação |
| F1 parcelas pagas | **IMPLEMENTADO** — coluna `parcelas_pagas` (migration compatível), campo no diálogo, "Em aberto" e "Parcelas por mês" amortizam; testes F1_* |
| F2 cartão/empréstimos → carteiras | **IMPLEMENTADO** (D-10) — compra gera saída do total na carteira escolhida; "Emprestei" saída; "Pedi" entrada; exclusão estorna; edição reajusta (vínculo `transacao_id`); testes F2_* |
| F3 instância única | **IMPLEMENTADO** — mutex `Local\SmartFinance.SingleInstance`; segunda instância traz a original à frente e encerra |
| F4 simulações | **PARCIALMENTE EXECUTADO** — varredura UIA de acessibilidade (o que leitores de tela anunciam) implementada como teste permanente (2/2); Windows 11 e suspensão/hibernação **ficam para "um dia"** (decisão do usuário) |
| F5 auto-atualização | **IMPLEMENTADO** — checagem diária silenciosa via GitHub Releases; se houver versão nova, pergunta e abre a página de download; falha de rede ignorada; testes F5_* |

## Rodada 5 (11/11/2026) — resposta às decisões do usuário (parte 2)

| Pendência | Resultado |
|---|---|
| F6 atualização automática | **IMPLEMENTADO** — além da checagem diária, o aplicativo agora **se atualiza sozinho**: baixa o instalador da release, confere o SHA-256 publicado, fecha e reinstala silenciosamente (o instalador reabre o app ao terminar). Testado de ponta a ponta localmente (instalação silenciosa, upgrade sobre instalação existente e reabertura automática) |
| Campos de data na tela de transações (dúvida do usuário) | **VERIFICADO — não faltava nada**: os campos De/Até existem, aceitam digitação e filtram de fato; a verificação virou teste permanente (`DataTransacoesTests`: 3 → 2 → 0 → 3 linhas conforme o intervalo) |
| F4 (restante) | Windows 11 / suspensão / leitor de tela físico: mantido (não simulável nesta máquina — decisão do usuário: "quem sabe um dia") |
