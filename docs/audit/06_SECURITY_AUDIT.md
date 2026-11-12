# 06 — Auditoria de segurança

## Superfície de ataque (modelo)

**Ativos:** `financas.db` (dados financeiros), `settings.json`/`atualizacao.json` (preferências), binário distribuído.
**Fronteiras:** arquivo local (perfil do usuário); rede (apenas GitHub API/releases para F5/F6).
**Agentes:** malware local com acesso ao perfil; canal de distribuição; entradas do próprio usuário.

## Verificações executadas nesta auditoria

| Verificação | Resultado |
|---|---|
| Credenciais/segredos (grep de padrões) | Nenhum |
| SQL concatenado com entrada do usuário | Nenhum — 100% parametrizado; interpolações usam constantes de classe (SF-033 comentado) |
| Rede: endpoints e métodos | Somente leitura da API GitHub (`releases/latest`) e download do asset — nenhum dado enviado |
| Logs com dados sensíveis | `erro.log` contém apenas stack traces e "startup ok"/"backup" |
| `dotnet list package --vulnerable --include-transitive` | Vazio |
| Execução sem privilégios | Confirmado (rodadas 3-5) |
| Caminho do banco configurável | Apenas via variável de ambiente definida pelo próprio usuário (uso de teste) |

## Achados

| ID | Achado | Severidade |
|---|---|---|
| SF-047 | **Integridade do auto-update**: o SHA-256 do instalador é publicado nas notas da release e verificado no download — porém o hash reside **no mesmo canal** da release (GitHub). Comprometimento da conta GitHub anularia a verificação. Mitigações atuais: TLS, autenticidade do repositório, tamanho mínimo checado implicitamente. Risco plausível e documentado (não especulativo) | INFO |
| — | Banco sem criptografia em repouso: risco conhecido e aceito (docs/SECURITY.md; usuário decidiu não investir em certificado/custos) | (aceito) |
| — | `erro.log` com rotação 1MB e sem dados financeiros | (positivo) |

## Conclusão

Sem vulnerabilidades novas. O modelo de ameaça realista (malware local lendo `financas.db`) permanece dependente da decisão de criptografia em repouso — já documentada como risco aceito com roadmap em `docs/SECURITY.md`.
