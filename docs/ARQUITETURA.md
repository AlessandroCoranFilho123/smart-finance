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
- `Themes/Global.xaml` concentra estilos (botões, campos, ComboBox, calendário, listas, barra de progresso, scrollbar fina); os componentes usam `DynamicResource`, então a troca de tema é instantânea.
- Barra de título acompanha o modo escuro do Windows via `DwmSetWindowAttribute` (`Views/JanelaNativa.cs`), com fallback silencioso.
- Ícones reaproveitam os PNGs da versão JavaFX (empacotados como recursos).

### Convenções e pegadinhas de template (protegidas por teste)

- **Calendar** (`Views/SeletorData`): o template padrão do WPF usa texto preto sobre fundo transparente e fica ilegível no tema escuro. Os estilos do calendário vivem em `Global.xaml`. Dois detalhes que o template padrão exige:
  - Os botões de dia/mês (`CalendarDayButton`/`CalendarButton`) são criados em código **dentro da árvore do template do `CalendarItem`**, onde estilos implícitos de aplicativo não são aplicados — por isso entram pelas propriedades `Calendar.CalendarDayButtonStyle` e `Calendar.CalendarButtonStyle` (chaves `BotaoDiaCalendario`/`BotaoMesCalendario`).
  - A troca visão de dias ↔ visão de meses/anos é feita por **`DataTrigger` no `DisplayMode` do `Calendar` ancestral** (`RelativeSource FindAncestor`), pois `CalendarItem` não tem essa propriedade; o `CalendarItem` só popula as grades `PART_MonthView`/`PART_YearView` (nomes obrigatórios).
- **TextBox**: o `TextBoxView` interno já aplica o `Padding` do controle — o template NÃO deve repetir o padding como `Margin` no `PART_ContentHost`, senão o campo fica ~18px mais alto que ComboBox/SeletorData na mesma linha.
- **Cores de texto em botões (SF-025 estendido)**: o estilo implícito de `TextBlock` (que fixa `Foreground`) **vence** o `Foreground` herdado/anexado dentro de templates — corrigido nos templates de `BotaoPrimario`/`BotaoPerigo`/`BotaoAlternar`/`BotaoExcluir` e dos botões do calendário usando `TextBlock` explícito com `TemplateBinding` (valor local vence o estilo implícito). O estilo implícito e os templates de conteúdo elemento (`BotaoTema`, `BotaoData`) continuam com `ContentPresenter`. `BotaoExcluir` (base `BotaoSecundario` + texto vermelho) compartilha tamanho e linha com Cancelar/Salvar.
- Esses pontos têm regressão coberta por testes de GUI: `AlinhamentoTests` (alturas/alinhamento via retângulos UIA) e `CapturaTests` (cores do calendário amostradas por pixel nas capturas dos dois temas).

## Codificação de fontes (crítico para strings acentuadas)

- Todo `.cs` do projeto deve ser **UTF-8 estrito** (sem bytes inválidos). O compilador Roslyn valida o arquivo como UTF-8 e, **se houver um único byte inválido, recodifica o arquivo INTEIRO no codepage ANSI (cp1252)** — todas as strings acentuadas viram mojibake no binário (bug real da 3.4.x: a MessageBox de instância única exibia "jÃ¡ estÃ¡").
- Arquivos UTF-8 válido **sem BOM** compilam corretamente (verificado com repro mínimo no SDK em uso); BOM é opcional, consistência não é.
- Deteção: `python -c "open(arquivo, 'rb').read().decode('utf-8')"` em todos os `.cs`; o `erro.log` do app (`%APPDATA%\SmartFinance`) exibe as strings do runtime — mojibake lá = arquivo corrompido.

## Testes

`tests/SmartFinance.Tests` cobre com banco SQLite real em pasta temporária (`TestApp`):

- parsing e formatação monetária;
- validações e fluxos do `TransacaoService` (incluindo rollback de atualização, clamp de meta e reversão na exclusão);
- saldos e transferências de `CarteiraService`;
- bootstrap, migration de banco legado, FTS5 (prefixo, acento, purge automático) e filtro combinado;
- regras de cartão, empréstimos e metas;
- formatos dos exportadores CSV/TXT.

`tests/SmartFinance.GuiTests` exercita a interface real via UIA/FlaUI (app com banco isolado em `%TEMP%`):

- `GuiTests` — navegação, criação de transação pela UI, diálogos, teclado, persistência;
- `DataTransacoesTests` — seletores de data da tela de transações (o filtro por datas é coberto em unitários; **excluído do CI**: semear o banco por segunda conexão SQLite não é visível para a conexão WAL já aberta pelo app — limitação de ambiente documentada em SF-058);
- `AlinhamentoTests` — alturas/alinhamento dos campos do filtro e da linha de botões do diálogo de edição;
- `CapturaTests` — captura todas as telas/diálogos e o calendário nos dois temas em `docs/auditoria/imagens` (base para verificação visual) e **asserta cores por pixel** (cartão do popup, dia de hoje, campo de data) para prevenir regressão de contraste;
- `AcessibilidadeTests` — varredura de árvore UIA.

Contagens atuais: **144 testes unitários** + **15 GUI** (`DataTransacoesTests` excluído do CI). Os testes de GUI exigem sessão interativa do Windows (no CI rodam no runner windows-latest, exceto os dois testes sensíveis a ambiente — ver `ci.yml`).

## Publicação

`dotnet publish src/SmartFinance -c Release -o dist` produz um único `SmartFinance.exe` autocontido (`win-x64`, ReadyToRun, bibliotecas nativas embutidas — extraídas para `%TEMP%\.net` na primeira execução).

### Release completa (automatizada)

`powershell -File setup/Publicar.ps1 -Versao 3.4.4` (a partir do worktree da `main`, árvore limpa) executa o fluxo inteiro:

1. bump da versão em `SmartFinance.csproj` e `setup/SmartFinance.iss` + commit;
2. build Release + testes unitários;
3. publicação do exe (`dist/`) e geração do instalador com Inno Setup (`setup/Output/SmartFinance-Setup-<versão>.exe`);
4. push do `main`;
5. criação da release no GitHub (via API, usando a credencial do Git Credential Manager) com o instalador como asset e os hashes SHA-256 do exe e do instalador nas notas — é esse hash que o auto-atualizador confere (`AtualizacaoService.ExtrairHashSetup`);
6. verificação ponta a ponta: `releases/latest` apontando para a nova versão, hash parseável nas notas e SHA-256 do asset baixado igual ao publicado.

O aplicativo verifica atualização 1× por dia (`AtualizacaoService`): release publicada = atualização entregue automaticamente. Requisitos locais: .NET SDK, Inno Setup 6 e credencial GitHub do `git-credential-manager`.
