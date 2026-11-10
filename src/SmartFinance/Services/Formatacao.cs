using System.Globalization;

namespace SmartFinance.Services;

/// <summary>Formatação de valores e datas para exibição (pt-BR).</summary>
public static class Formatacao
{
    public static readonly CultureInfo CulturaBr = CultureInfo.GetCultureInfo("pt-BR");

    private const string PadraoData = "dd/MM/yyyy";

    public static string Moeda(long centavos)
    {
        return (centavos / 100m).ToString("C2", CulturaBr);
    }

    public static string MoedaComSinal(long centavos, bool entrada)
    {
        string sinal = entrada ? "+ " : "- ";
        return sinal + Moeda(centavos);
    }

    public static string Data(DateOnly data)
    {
        return data.ToString(PadraoData, CulturaBr);
    }

    public static bool TryParseData(string? texto, out DateOnly data)
    {
        return DateOnly.TryParseExact(texto?.Trim(), PadraoData, CulturaBr, DateTimeStyles.None, out data);
    }
}
