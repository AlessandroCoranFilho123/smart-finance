# SmartFinance — Notas de segurança

## Estado atual

O SmartFinance é um aplicativo desktop local com banco SQLite. O banco fica em:

```text
%APPDATA%\SmartFinance\financas.db
```

O arquivo é protegido pelas permissões padrão do perfil do usuário do Windows (outros usuários da máquina não têm acesso ao perfil). O aplicativo não envia dados para a internet — não há rede, telemetria nem sincronização.

## Medidas aplicadas

- Consultas SQL 100% parametrizadas (`SqliteCommand` com `SqliteParameter`), sem concatenação de entrada do usuário.
- `PRAGMA foreign_keys = ON` preserva integridade referencial.
- `PRAGMA busy_timeout = 5000` evita falhas imediatas em concorrência local.
- `PRAGMA journal_mode = WAL` melhora leitura/escrita simultânea.
- `PRAGMA synchronous = NORMAL` equilibra segurança de gravação e desempenho.
- `PRAGMA secure_delete = ON` reduz sobra de dados apagados em páginas reutilizadas.
- Operações compostas (metas, atualização de transação, exclusão com reversão) rodam em transações SQL com rollback completo em falha.
- Busca textual via FTS5, sem varredura de aplicação.
- Preferências (tema) em `settings.json`; nenhum dado financeiro é escrito fora do banco.

## Não implementado (limitações honestas)

A versão JavaFX 2.x continha uma **fundação** de criptografia (AES-256-GCM com chave no Windows Credential Manager), porém ela não era invocada por nenhum fluxo — o banco nunca foi criptografado. A reescrita removeu esse código morto. Ou seja:

- **O banco não é criptografado em repouso.** Quem tiver acesso ao arquivo (por exemplo, acesso ao disco) lê os dados.
- Não há backup automático embutido; exportar `.csv`/`.txt` ou copiar `financas.db` (com o app fechado) é o meio de backup atual.

## Próximos passos

1. Criptografia em repouso do arquivo (SQLCipher ou criptografia por campo com chave via DPAPI/Credential Manager) — requer decisão sobre a busca FTS, que indexa texto claro.
2. Backup automático agendado do `financas.db` com opção de exportação protegida por senha (derivação PBKDF2/Argon2id).
3. Revisão de permissão do arquivo do banco (ACL restritiva) na criação.
4. Evitar logs com valores financeiros, comentários ou identificadores sensíveis.

## Backup

Snapshot diário automático em `%APPDATA%\SmartFinance\backups` (API de backup online do SQLite, retenção de 7 cópias). O snapshot é consistente mesmo com o aplicativo aberto. Restauração manual: substituir `financas.db` por um snapshot com o aplicativo fechado.
