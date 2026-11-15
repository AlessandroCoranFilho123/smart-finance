# SmartFinance — Notas de segurança

## Estado atual

O SmartFinance é um aplicativo desktop local com banco SQLite. O banco fica em:

```text
%APPDATA%\SmartFinance\financas.db
```

O arquivo é protegido pelas permissões padrão do perfil do usuário do Windows (outros usuários da máquina não têm acesso ao perfil). **Nenhum dado financeiro sai da máquina** — não há telemetria nem sincronização. O único tráfego de rede é a verificação de atualização (uma consulta diária ao `api.github.com`) e, com consentimento do usuário, o download do instalador da nova versão.

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
- Não há exportação de backup protegida por senha; exportar `.csv`/`.txt` ou copiar `financas.db` (com o app fechado) é o meio de cópia manual.

## Atualização automática (F5/F6)

- Checagem diária silenciosa em `https://api.github.com/repos/.../releases/latest` (HTTPS, sem envio de dados).
- O instalador só é baixado e executado **com consentimento do usuário** e somente se o SHA-256 do instalador estiver publicado nas notas da release — o hash é conferido antes de executar.
- Se a release não publicar o hash (ou estiver em formato inesperado), o aplicativo **recusa a instalação silenciosa** e apenas abre a página da release para download manual.
- Risco residual documentado (SF-047): o hash reside no mesmo canal (GitHub) que o instalador — comprometimento da conta anularia a verificação.

## Próximos passos

1. Criptografia em repouso do arquivo (SQLCipher ou criptografia por campo com chave via DPAPI/Credential Manager) — requer decisão sobre a busca FTS, que indexa texto claro.
2. Exportação de backup protegida por senha (derivação PBKDF2/Argon2id).
3. Revisão de permissão do arquivo do banco (ACL restritiva) na criação.
4. Evitar logs com valores financeiros, comentários ou identificadores sensíveis.

## Backup

Snapshot diário automático em `%APPDATA%\SmartFinance\backups` (API de backup online do SQLite, retenção de 7 cópias). O snapshot é consistente mesmo com o aplicativo aberto. Restauração manual: substituir `financas.db` por um snapshot com o aplicativo fechado.

### Restauração e WAL (SF-032)

O snapshot automático interno usa a API de backup online do SQLite e captura também o conteúdo do WAL — é o método recomendado. Se for copiar arquivos manualmente, **feche o aplicativo primeiro** e copie `financas.db`, `financas.db-wal` e `financas.db-shm` juntos; copiar apenas `financas.db` com o app aberto pode perder transações recentes.
