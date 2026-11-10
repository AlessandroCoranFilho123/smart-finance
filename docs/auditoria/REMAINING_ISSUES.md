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
