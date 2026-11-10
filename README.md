# Smart Finance

Smart Finance é um aplicativo desktop **para Windows** de controle financeiro pessoal: transações, carteiras, metas, compras parceladas no cartão e empréstimos — com busca textual, filtros, exportação e tema claro/escuro.

A versão 3.1 é uma **reescrita completa em C# / WPF (.NET 10)**. A versão anterior era JavaFX e foi substituída por um aplicativo nativo mais leve: sem JVM, inicialização praticamente instantânea e distribuição em um único executável.

## Funcionalidades

- **Transações** — entradas e saídas com 13 categorias, comentário, data e carteira; edição e exclusão com duplo clique.
- **Carteiras** — Banco e Dinheiro físico com saldo inicial editável e transferências entre elas, com validação de saldo.
- **Metas** — acompanhamento de progresso com barra colorida; aplicar/resgatar dinheiro de uma meta gera transações automáticas, com limites ao restante e ao saldo disponível.
- **Cartão de crédito** — compras parceladas com valor da parcela calculado e status de vencimento (verde / laranja / vermelho).
- **Empréstimos** — registros separados entre "Emprestei pra alguém" e "Pedi emprestado", com totais por tipo.
- **Dashboard** — saldo total, receitas e despesas do mês, saldos por carteira, transações recentes e metas.
- **Busca e filtros** — busca textual sem acento com prefixo (FTS5) combinada com tipo, categoria e período.
- **Exportação** — transações filtradas em `.csv` e `.txt`.
- **Temas** — claro e escuro (segue o sistema por padrão), com persistência da escolha.

## Instalação

Disponível somente para Windows 10/11.

Baixe o `SmartFinance-Setup-3.1.0.exe` (instalador com atalhos e desinstalador) ou o `SmartFinance.exe` portátil na área de releases.

[![GitHub](https://img.shields.io/badge/GitHub-%23121011.svg?logo=github&logoColor=white)](https://github.com/AlessandroCoranFilho123/smart-finance)

> O antivirus pode pedir confirmação na primeira execução por se tratar de um executável novo. O primeiro início também extrai bibliotecas nativas para uma pasta temporária e é um pouco mais lento; nas próximas execuções a inicialização é praticamente imediata.

## Dados

Os dados ficam em banco SQLite local:

```text
%APPDATA%\SmartFinance\financas.db
```

O schema é o mesmo da versão JavaFX: **quem já usava a versão 2.x mantém transações, metas, carteiras, compras no cartão e empréstimos sem migração manual**. O aplicativo aplica automaticamente as migrations incrementais em bancos antigos.

Preferências de tema ficam em `%APPDATA%\SmartFinance\settings.json`.

## Segurança

O banco é um arquivo local protegido pelas permissões do seu perfil do Windows. A camada de banco usa `PRAGMA foreign_keys`, WAL, `secure_delete` e consultas parametrizadas. Detalhes e próximos passos em [docs/SECURITY.md](docs/SECURITY.md).

## Desenvolvimento

Requisitos: [.NET SDK 10 ou superior](https://dotnet.microsoft.com/download) (o projeto foi desenvolvido com o SDK 11 RC, alvo `net10.0-windows`).

```bash
# restaurar e rodar
dotnet run --project src/SmartFinance

# testes (xUnit)
dotnet test tests/SmartFinance.Tests

# publicar executável único autocontido (win-x64)
dotnet publish src/SmartFinance -c Release -o dist
```

Para apontar o aplicativo para outro banco durante o desenvolvimento:

```bash
SMARTFINANCE_DB_PATH=C:\caminho\para\teste.db dotnet run --project src/SmartFinance
```

## Estrutura

```
src/SmartFinance/            aplicativo WPF
├── Data/                    acesso ao SQLite (Database + repositórios)
├── Models/                  registros de domínio e enums
├── Services/                regras de negócio, parsing monetário e exportadores
├── Themes/                  paletas clara/escura, estilos e templates
├── Views/                   janela principal, visões e diálogos
└── Assets/                  ícones
tests/SmartFinance.Tests/    testes de negócio, dados e exportação
docs/                        arquitetura e segurança
```

## Tecnologias

- C# 14 / .NET 10 (WPF)
- SQLite via Microsoft.Data.Sqlite (FTS5 para busca textual)
- xUnit para testes

## Status do projeto

Projeto acadêmico em evolução. A versão 3.1 cobre o mesmo conjunto de funcionalidades da 2.x, com correções de bugs de parsing monetário, atomicidade transacional nas operações compostas e interface redesenhada. Veja [docs/ARQUITETURA.md](docs/ARQUITETURA.md) para as decisões de design e [docs/auditoria/AUDIT_REPORT.md](docs/auditoria/AUDIT_REPORT.md) para a auditoria técnica completa (10/11/2026).

## Backup automático

A cada dia de uso o aplicativo gera um snapshot do banco em `%APPDATA%\SmartFinance\backups` (retenção de 7 cópias), usando a API de backup online do SQLite. Para restaurar: feche o aplicativo e copie um snapshot de volta para `%APPDATA%\SmartFinance\financas.db`.
