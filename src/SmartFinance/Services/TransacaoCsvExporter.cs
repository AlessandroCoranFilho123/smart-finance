using System.Globalization;
using System.IO;
using System.Text;
using SmartFinance.Models;

namespace SmartFinance.Services;

/// <summary>Exportação em CSV (separador ponto e vírgula, valores no padrão pt-BR, com BOM).</summary>
public static class TransacaoCsvExporter
{
    public static void Exportar(string destino, IReadOnlyList<Transacao> transacoes)
    {
        File.WriteAllText(destino, "\uFEFF" + Formatar(transacoes), new UTF8Encoding(false));
    }

    public static string Formatar(IReadOnlyList<Transacao> transacoes)
    {
        var builder = new StringBuilder();
        builder.AppendLine("Data;Tipo;Descricao;Categoria;Comentario;Valor");

        foreach (Transacao transacao in transacoes.OrderBy(t => t.Data))
        {
            builder.Append(Escapar(Formatacao.Data(transacao.Data))).Append(';')
                .Append(Escapar(transacao.Tipo.ToString())).Append(';')
                .Append(Escapar(transacao.Descricao)).Append(';')
                .Append(Escapar(transacao.Categoria?.ToString() ?? "Sem categoria")).Append(';')
                .Append(Escapar(transacao.Comentario)).Append(';')
                .Append(Escapar(FormatarValor(transacao.ValorCentavos)))
                .AppendLine();
        }

        return builder.ToString();
    }

    private static string FormatarValor(long valorCentavos)
    {
        return (valorCentavos / 100m).ToString("0.00", Formatacao.CulturaBr);
    }

    private static string Escapar(string? valor)
    {
        string conteudo = valor ?? string.Empty;
        bool precisaAspas = conteudo.Contains(';') || conteudo.Contains('"')
            || conteudo.Contains('\n') || conteudo.Contains('\r');

        return precisaAspas ? "\"" + conteudo.Replace("\"", "\"\"") + "\"" : conteudo;
    }
}
