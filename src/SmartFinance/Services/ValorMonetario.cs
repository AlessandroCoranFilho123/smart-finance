using System.Globalization;

namespace SmartFinance.Services;

/// <summary>Conversão exata entre texto e centavos (sem ponto flutuante, aceita padrão pt-BR).</summary>
public static class ValorMonetario
{
    public static long ParseCentavos(string? texto)
    {
        if (string.IsNullOrWhiteSpace(texto))
        {
            return 0L;
        }

        string normalizado = texto.Replace("R$", "", StringComparison.OrdinalIgnoreCase).Trim();
        if (normalizado.Contains(','))
        {
            // padrão BR: ponto é milhar, vírgula é decimal
            normalizado = normalizado.Replace(".", "").Replace(',', '.');
        }
        else if (PontoComoMilhar(normalizado))
        {
            // "1.005" sem vírgula: grupo de exatamente 3 dígitos após o ponto indica milhar
            normalizado = normalizado.Replace(".", "");
        }

        if (!decimal.TryParse(normalizado, NumberStyles.Number, CultureInfo.InvariantCulture, out decimal valor))
        {
            throw new ArgumentException("Valor inválido");
        }

        return (long)Math.Round(valor * 100m, MidpointRounding.AwayFromZero);
    }

    public static string FormatarCentavos(long centavos)
    {
        return (centavos / 100m).ToString("0.00", CultureInfo.InvariantCulture);
    }

    private static bool PontoComoMilhar(string texto)
    {
        int ultimoPonto = texto.LastIndexOf('.');
        if (ultimoPonto <= 0)
        {
            return false;
        }

        int digitosApos = texto.Length - ultimoPonto - 1;
        if (digitosApos != 3)
        {
            return false;
        }

        for (int i = ultimoPonto + 1; i < texto.Length; i++)
        {
            if (!char.IsDigit(texto[i]))
            {
                return false;
            }
        }

        return true;
    }
}
