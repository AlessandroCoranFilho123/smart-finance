using System.IO;
using System.Text;
using SmartFinance.Models;

namespace SmartFinance.Services;

/// <summary>Exportação em TXT legível, com resumo de totais no cabeçalho.</summary>
public static class TransacaoTxtExporter
{
    public static void Exportar(string destino, IReadOnlyList<Transacao> transacoes)
    {
        File.WriteAllText(destino, Formatar(transacoes), Encoding.UTF8);
    }

    public static string Formatar(IReadOnlyList<Transacao> transacoes)
    {
        long totalEntradas = SomarPorTipo(transacoes, TipoTransacao.Entrada);
        long totalSaidas = SomarPorTipo(transacoes, TipoTransacao.Saida);

        var builder = new StringBuilder();
        builder.AppendLine("Smart Finance - Exportação de Transações");
        builder.AppendLine($"Gerado em: {DateTime.Now:dd/MM/yyyy HH:mm}");
        builder.AppendLine($"Quantidade: {transacoes.Count}");
        builder.AppendLine($"Total de Entradas: {Formatacao.Moeda(totalEntradas)}");
        builder.AppendLine($"Total de Saídas: {Formatacao.Moeda(totalSaidas)}");
        builder.AppendLine();

        if (transacoes.Count == 0)
        {
            builder.AppendLine("Nenhuma transação encontrada.");
            return builder.ToString();
        }

        var ordenadas = transacoes.OrderByDescending(t => t.Data).ToList();
        for (int i = 0; i < ordenadas.Count; i++)
        {
            Transacao transacao = ordenadas[i];
            builder.AppendLine($"{i + 1}. {Formatacao.Data(transacao.Data)} | {transacao.Tipo} | {transacao.Descricao} | {Formatacao.Moeda(transacao.ValorCentavos)}");
            builder.AppendLine($"Categoria: {transacao.Categoria?.ToString() ?? "Sem categoria"}");

            if (!string.IsNullOrWhiteSpace(transacao.Comentario))
            {
                builder.AppendLine($"Comentário: {transacao.Comentario}");
            }

            if (i < ordenadas.Count - 1)
            {
                builder.AppendLine();
            }
        }

        return builder.ToString();
    }

    private static long SomarPorTipo(IReadOnlyList<Transacao> transacoes, TipoTransacao tipo) =>
        transacoes.Where(t => t.Tipo == tipo).Sum(t => t.ValorCentavos);
}
