# 02 — Auditoria de arquitetura

## Arquitetura real

```
App.OnStartup (composition root, mutex F3)
 ├─ Database (conexão única, migrations incrementais, FTS5)
 ├─ Repositórios: Transacao, Meta, Carteira, CartaoCredito, Emprestimo
 ├─ Services: TransacaoService (EmTransacao reentrante), CarteiraService,
 │   MetaService, CartaoCreditoService (movimentação vinculada D-10),
 │   EmprestimoService (idem), BackupService (diário), AtualizacaoService (F6)
 └─ MainWindow → 5 visões + 6 diálogos (code-behind; Linha* pré-formatadas)
Tema: TemaManager (paletas trocáveis) · JanelaNativa (DWM) · TemaManager logs
```

A arquitetura implementada **confere** com `docs/ARQUITETURA.md` no que o documento descreve; o documento, porém, **está atrás** das features 3.3/3.4 (SF-042): não descreve `CartaoCreditoService`/`EmprestimoService` gerando movimentações (D-10), `AtualizacaoService` (F6), mutex de instância única (F3) nem o particionamento `BrushAccent` × `BrushAccentTexto` (D-12).

## Constatações

| ID | Constatação | Severidade |
|---|---|---|
| — | Camadas coerentes: Views → Services → Data; nenhuma consulta SQL em Views; nenhum MessageBox em Services | (positivo) |
| — | `Database.EmTransacao` reentrante usado em **todas** as operações compostas, incluindo as novas de F2 | (positivo) |
| SF-040 | Movimentações de F2 fixadas em `DateTime.Today` — a data de competência não é controlável e a edição reposiciona a movimentação | HIGH |
| SF-042 | Docs desatualizadas (ARQUITETURA/SECURITY/README) | MEDIUM |
| — | `Servicos` como agregador estático: pragmático para o porte; trocar por DI não traz benefício mensurável (regra 13/14 da auditoria) | (não-achado) |

## Manutenibilidade

- Um desenvolvedor localiza cada feature por 1 arquivo de serviço + 1 visão + 1 classe de teste (nomenclatura consistente).
- Pontos de atenção: `TransacaoService.Atualizar` (delete+insert com reversão de meta — alterações exigem os testes T-004*) e `MainWindow.xaml.cs` (shell + tema + atualização + navegação; ~230 linhas, aceitável).
- Sem dependências circulares; sem código morto (grep de usos confirmado na rodada 3; `CapturaTests`/`AcessibilidadeTests` são testes de propósito).
