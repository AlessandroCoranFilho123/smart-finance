# Arquitetura do Smart Finance

Este documento descreve a implementação real da versão 3.0 (C# / WPF).

## Visão geral

O aplicativo segue uma separação em três camadas, sem frameworks de DI ou abstrações extras — o tamanho do projeto não justifica mais do que isso:

```
Views (WPF)  →  Services (regras de negócio)  →  Data (SQLite)
```

- **Data** — `Database` mantém uma única conexão SQLite de longa duração (com pragmas de integridade aplicados na abertura) e realiza bootstrap/migrations do schema. Repositórios (`TransacaoRepository`, `CarteiraRepository`, `MetaRepository`, `CartaoCreditoRepository`, `EmprestimoRepository`) encapsulam SQL parametrizado e mapeamento para registros de domínio.
- **Services** — regras de negócio: validações de saldo, limites de meta, transferências, parcelas e exportação. Recebem repositórios por construtor.
- **Views** — janela principal (shell com navegação lateral), visões (`UserControl`) e diálogos (`Window`). Os itens de lista usam modelos de exibição pré-formatados (`Views/Linhas.cs`) com templates XAML compartilhados (`Themes/Templates.xaml`), evitando conversores.

A composição acontece em `App.OnStartup` (classe `Servicos`), que também resolve o caminho do banco.

## Banco de dados

Arquivo: `%APPDATA%\SmartFinance\financas.db` (override para desenvolvimento via variável `SMARTFINANCE_DB_PATH`).

O schema é mantido **compatível com a versão JavaFX 2.x** (mesmos nomes de tabelas, colunas, IDs de carteiras padrão e formato ISO `yyyy-MM-dd` para datas, centavos inteiros para valores), então bancos existentes funcionam sem migração manual. `Database.Initialize()`:

1. Cria tabelas se não existirem (`meta`, `carteira` com seed das duas carteiras padrão, `transacao`, `compra_cartao_credito`, `emprestimo`, `transferencia_carteira`).
2. Aplica migrations incrementais (adiciona `comentario`, `categoria` e `carteira_id` em bancos antigos e preenche `carteira_id` nulo com a carteira Banco).
3. Cria índices das consultas frequentes.
4. Cria o índice textual FTS5 (`transacao_fts`) com tokenizer `unicode61 remove_diacritics 2` e triggers que mantêm o índice sincronizado em INSERT/UPDATE/DELETE.

### Transações SQL

Toda operação que combina mais de um comando (aplicar/resgatar meta, atualizar transação com reajuste de meta, exclusão com reversão) roda dentro de `Database.EmTransacao`, com reentrância para chamadas aninhadas. Garantias:

- Falha de validação no meio do caminho **não deixa estado parcial** (rollback completo).
- A atualização de uma transação exclui a original antes de validar o saldo (para não contar a mesma movimentação duas vezes) e, se a validação falha, o rollback restaura tudo.

## Regras de negócio principais

- Valores são sempre `long` em centavos; conversão texto↔centavos usa `ValorMonetario` (decimal exato, sem ponto flutuante), aceitando padrão pt-BR (`1.234,56`), vírgula decimal, ponto decimal e ponto de milhar (`1.005`).
- Saídas exigem saldo disponível **na carteira escolhida** (saldo inicial + transações + transferências recebidas − enviadas).
- "Aplicar em meta" é saída da carteira e entrada na meta, limitado ao restante do alvo; "Resgatar meta" é entrada na carteira, limitado ao acumulado da meta. Excluir/atualizar essas transações reverte o saldo da meta.
- Saldo inicial de carteira não pode ser negativo nem deixar a carteira com saldo negativo.
- O saldo total do dashboard é a soma dos saldos das carteiras.

## UI e temas

- Paletas em `Themes/Palette.Light.xaml` e `Palette.Dark.xaml`; `TemaManager` troca o dicionário de recursos em tempo de execução e persiste a escolha em `settings.json` (padrão: seguir o sistema, via registro do Windows).
- `Themes/Global.xaml` concentra estilos (botões, campos, ComboBox, DatePicker, listas, barra de progresso, scrollbar fina); os componentes usam `DynamicResource`, então a troca de tema é instantânea.
- Barra de título acompanha o modo escuro do Windows via `DwmSetWindowAttribute` (`Views/JanelaNativa.cs`), com fallback silencioso.
- Ícones reaproveitam os PNGs da versão JavaFX (empacotados como recursos).

## Testes

`tests/SmartFinance.Tests` cobre com banco SQLite real em pasta temporária (`TestApp`):

- parsing e formatação monetária;
- validações e fluxos do `TransacaoService` (incluindo rollback de atualização, clamp de meta e reversão na exclusão);
- saldos e transferências de `CarteiraService`;
- bootstrap, migration de banco legado, FTS5 (prefixo, acento, purge automático) e filtro combinado;
- regras de cartão, empréstimos e metas;
- formatos dos exportadores CSV/TXT.

A interface não tem testes automatizados; a validação de UI é manual (smoke test de inicialização + uso).

## Publicação

`dotnet publish src/SmartFinance -c Release -o dist` produz um único `SmartFinance.exe` autocontido (`win-x64`, ReadyToRun, bibliotecas nativas embutidas — extraídas para `%TEMP%\.net` na primeira execução).
