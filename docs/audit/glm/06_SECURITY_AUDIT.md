# 06 — Auditoria de segurança (GLM)

## Superfície real (sem invenção)

Arquivo local (perfil do usuário) + rede de leitura (GitHub API/releases para F6). Sem autenticação (app local de usuário único). Nenhum dado enviado para fora.

## Verificações executadas nesta auditoria

| Verificação | Resultado |
|---|---|
| SQL com entrada concatenada | Nenhum (parametrizado; interpolações = constantes de classe) |
| Segredos/credenciais no repositório | Nenhum (grep de padrões) |
| Dependências vulneráveis (transitivas) | Nenhuma |
| Rede — origem/destino | Somente `api.github.com` (leitura) e `github.com` (download); `User-Agent` próprio; nenhum dado do usuário transmitido |
| Dados sensíveis em logs | `erro.log`: stack traces + eventos de backup/startup — sem valores |
| F6 integridade | SHA-256 do instalador conferido contra hash das notas; falha → cancelado com mensagem |
| Auto-update supply chain | Hash publicado no mesmo canal da release (SF-047, INFO — mitigado por TLS/repositório autenticado) |
| Path traversal | Nenhum caminho construído com entrada do usuário (exportação via SaveFileDialog) |
| Execução sem privilégios | Confirmado |

## Modelo de ameaças (realista)

1. **Malware local** lê `financas.db` — mitigação futura: criptografia em repouso (fora de escopo, decisão U1/Custos).
2. **Comprometimento do canal de distribuição** (conta GitHub) — mitigações: TLS, hash nas notas; eliminação real exigiria assinatura de código (decisão U1: não adquirir).
3. **Instalador baixado executado pelo usuário** — verificação de hash implementada no F6 automático.
