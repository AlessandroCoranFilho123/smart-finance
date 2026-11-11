# 15 — Limitações da auditoria

## O que NÃO foi feito
| Item | Motivo | Consequência |
|---|---|---|
| Compilação (`dotnet build`) | repositório não estava em disco/SDK nesta sessão; só o conteúdo dos arquivos foi fornecido | erros de compilação não verificados |
| Execução da suíte (79 execuções) | idem | o "79/79" vem da auditoria anterior e **não foi reverificado** |
| Execução do app / reprodução de qualquer achado | idem | nenhum achado tem confiança "Confirmado" |
| Números de linha | arquivos recebidos sem numeração | localização por arquivo + membro |
| Medições de desempenho | não executável | valores de `docs/auditoria/PERFORMANCE_REPORT.md` não foram reconfirmados |
| Inspeção do banco real do usuário | proibido por regra e indisponível | SF-011/SF-012 dependem dele (usar **cópia**) |
| Verificação de CVE/versões de pacotes | sem consulta externa | SF-027 é hipótese |
| `.gitignore` x arquivos versionados | árvore do repositório não foi listada | não se sabe se há binários/bancos versionados |
| Arquivos não recebidos | `Assets/`, `Views/Dialogs/*` não listados acima, `CarteirasDialog.xaml` etc. foram lidos; **binários/PNGs não** | existência dos PNGs não verificada |

## Incertezas por achado
- **SF-001, 002, 003, 004, 005, 006**: dedução direta do código + regras do WPF/SQLite; alta probabilidade, sem execução.
- **SF-010, 017, 025**: dependem de comportamento de renderização do WPF; confirmar visualmente (roteiro A).
- **SF-011**: depende de como a versão exata de `Microsoft.Data.Sqlite` grava `Guid` e de como o Java gravou os IDs.
- **SF-012**: depende do esquema FTS real da versão 2.x.
- Cenários não analisados a fundo: DPI/multi-monitor, acessibilidade com leitor de tela, suspensão/hibernação, Windows 11.

## Hipóteses pendentes
1. O app em uso real já abriu "Transações" sem erro? (se sim, SF-001 está errado — a regra do `ItemsSource` precisa ser reavaliada).
2. Os ícones de diálogo aparecem hoje? (se sim, SF-010 está errado).
3. IDs do banco legado têm letras a–f minúsculas?

## Correção de rota
Se alguma hipótese acima for refutada pelo uso real, **descartar o achado** em vez de "corrigir" código que funciona.
