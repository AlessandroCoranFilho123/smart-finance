# Relatório de segurança — 10/11/2026

Classificações seguem BUG_REPORT (CRÍTICO/ALTO/MÉDIO/BAIXO/INFORMATIVO). Nenhum teste destrutivo foi executado sobre dados reais.

## Verificações executadas

| # | Verificação | Resultado | Evidência |
|---|---|---|---|
| S1 | Injeção SQL | PASSOU | Toda entrada do usuário via `SqliteParameter`; única interpolação em SQL usa constante de classe (`RepositoryBase.ColunasTransacao`); consulta FTS é parametrizada (`$busca`) e os termos são normalizados para `[letras/dígitos]*` |
| S2 | Superfície de rede | PASSOU | `grep HttpClient\|WebRequest\|Socket\|Process.Start` → 0 ocorrências; aplicativo sem comunicação externa |
| S3 | Segredos/credenciais | PASSOU | Nenhum segredo no código (varredura de padrões) |
| S4 | Dependências vulneráveis | PASSOU | `dotnet list package --vulnerable --include-transitive` → nenhuma (bundle SQLitePCLRaw 3.0.5 fixado propositalmente por causa do CVE do bundle 2.1.11 transitivo) |
| S5 | Informações sensíveis em logs | PASSOU | `erro.log` grava apenas stack traces e `startup ok`; nenhum dado financeiro é logado |
| S6 | Path traversal / manipulação de arquivos | PASSOU | Caminho do banco fixo em `%APPDATA%` (override apenas via variável de ambiente definida pelo próprio usuário); exportação usa `SaveFileDialog` |
| S7 | Desserialização insegura | PASSOU | Único JSON lido é `settings.json` via `System.Text.Json` em tipos fechados |
| S8 | Privilégios | PASSOU | Aplicativo executa como usuário comum (sem manifest de elevação); testes de runtime rodaram sem admin |
| S9 | Integridade do executável distribuído | PASSOU | SHA-256 publicado nas notas da release; conferir com `certutil -hashfile SmartFinance.exe SHA256` |

## Riscos residuais

| # | Risco | Gravidade | Detalhe |
|---|---|---|---|
| R1 | Banco sem criptografia em repouso | MÉDIO (aceito) | Documentado em `docs/SECURITY.md` — arquivo protegido pelas permissões do perfil do usuário; roadmap de criptografia listado |
| R2 | Executável não assinado digitalmente | BAIXO | Windows SmartScreen/antivírus podem exibir aviso na primeira execução; assinatura de código exige certificado pago (fora do escopo atual) |
| R3 | Sem rotação de log | INFORMATIVO | `erro.log` cresce por append; volume esperado é mínimo (apenas falhas) |
| R4 | `settings.json` gravado sem proteção extra | INFORMATIVO | Contém apenas o tema |
