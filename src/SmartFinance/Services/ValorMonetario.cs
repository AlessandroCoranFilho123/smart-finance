using System.Globalization;
using System.Text.RegularExpressions;

namespace SmartFinance.Services;

/// <summary>
/// Conversão exata entre texto e centavos (sem ponto flutante), com validação
/// estrita do padrão pt-BR (decisão D-03): entradas ambíguas são recusadas em vez
/// de convertidas para um valor silenciosamente errado.
/// </summary>
public static partial class ValorMonetario
{
    [GeneratedRegex(@"^\d{1,3}(\.\d{3})*,\d{1,2}$")]
    private static partial Regex MilharComDecimal();

    [GeneratedRegex(@"^\d+\.\d{1,2}$")]
    private static partial Regex DecimalComPonto();

    [GeneratedRegex(@"^\d+$")]
    private static partial Regex SomenteDigitos();

    [GeneratedRegex(@"^[1-9]\d{0,2}(\.\d{3})+$")]
    private static partial Regex MilharPuro();

    public static long ParseCentavos(string? texto)
    {
        if (string.IsNullOrWhiteSpace(texto))
        {
            return 0L;
        }

        string normalizado = texto.Replace("R$", "", StringComparison.OrdinalIgnoreCase).Trim();
        bool valido;

        if (normalizado.Contains(','))
        {
            // padrão BR: ponto é milhar (grupos de 3), vírgula é decimal (1-2 casas)
            valido = MilharComDecimal().IsMatch(normalizado);
            if (valido)
            {
                normalizado = normalizado.Replace(".", "").Replace(',', '.');
            }
        }
        else if (normalizado.Contains('.'))
        {
            if (PontoComoMilhar(normalizado))
            {
                // "1.005" / "1.234.567": grupos exatos de 3 dígitos indicam milhar
                normalizado = normalizado.Replace(".", "");
                valido = true;
            }
            else
            {
                valido = DecimalComPonto().IsMatch(normalizado); // "10.50", "0,05" com ponto
            }
        }
        else
        {
            valido = SomenteDigitos().IsMatch(normalizado); // "10"
        }

        if (!valido || !decimal.TryParse(normalizado, NumberStyles.Number, CultureInfo.InvariantCulture, out decimal valor))
        {
            throw new ArgumentException("Valor inválido");
        }

        decimal centavos = valor * 100m;
        if (centavos < 0m || centavos > long.MaxValue)
        {
            throw new ArgumentException("Valor inválido");
        }

        return (long)Math.Round(centavos, MidpointRounding.AwayFromZero);
    }

    public static string FormatarCentavos(long centavos)
    {
        return (centavos / 100m).ToString("0.00", CultureInfo.InvariantCulture);
    }

    private static bool PontoComoMilhar(string texto)
    {
        return MilharPuro().IsMatch(texto);
    }
}
