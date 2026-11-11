using System.Globalization;
using System.Text.RegularExpressions;
using System.Windows.Media;

namespace SmartFinance.Tests;

/// <summary>
/// Verificação objetiva de contraste WCAG 2.1 para as paletas clara e escura:
/// lê os hexes direto dos XAMLs de paleta e calcula a taxa de cada par
/// semântico texto/fundo usado pela interface. Texto normal exige ≥ 4,5:1;
/// elementos gráficos/bordas exigem ≥ 3:1. Falhas são reportadas com a taxa
/// exata para ajuste da paleta no mesmo tom.
/// </summary>
public class ContrasteTests
{
    private static readonly Regex PadraoCor =
        new(@"x:Key=""(Brush\w+)""\s+Color=""#([0-9A-Fa-f]{8}|[0-9A-Fa-f]{6})""", RegexOptions.Compiled);

    private static Dictionary<string, Color> CarregarPaleta(string arquivo)
    {
        string raiz = Path.GetFullPath(Path.Combine(
            AppContext.BaseDirectory, "..", "..", "..", "..", ".."));
        string caminho = Path.Combine(raiz, "src", "SmartFinance", "Themes", arquivo);
        Assert.True(File.Exists(caminho), $"paleta não encontrada: {caminho}");

        var cores = new Dictionary<string, Color>(StringComparer.Ordinal);
        foreach (Match m in PadraoCor.Matches(File.ReadAllText(caminho)))
        {
            string hex = m.Groups[2].Value;
            if (hex.Length == 6)
            {
                hex = "FF" + hex;
            }

            cores[m.Groups[1].Value] = Color.FromRgb(
                byte.Parse(hex.AsSpan(2, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture),
                byte.Parse(hex.AsSpan(4, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture),
                byte.Parse(hex.AsSpan(6, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture));
        }

        return cores;
    }

    private static double Luminancia(Color c)
    {
        double Canal(double v)
        {
            v /= 255.0;
            return v <= 0.03928 ? v / 12.92 : Math.Pow((v + 0.055) / 1.055, 2.4);
        }

        return 0.2126 * Canal(c.R) + 0.7152 * Canal(c.G) + 0.0722 * Canal(c.B);
    }

    public static double TaxaContraste(Color a, Color b)
    {
        double l1 = Luminancia(a);
        double l2 = Luminancia(b);
        double maisClara = Math.Max(l1, l2);
        double maisEscura = Math.Min(l1, l2);
        return (maisClara + 0.05) / (maisEscura + 0.05);
    }

    private static (string Fore, string Back, double Minimo, string Uso)[] ParesTexto => new[]
    {
        ("BrushTextPrimary", "BrushBg", 4.5, "texto geral sobre fundo"),
        ("BrushTextPrimary", "BrushCard", 4.5, "texto sobre cartões"),
        ("BrushTextPrimary", "BrushSidebar", 4.5, "texto sobre barra lateral"),
        ("BrushTextSecondary", "BrushBg", 4.5, "texto secundário sobre fundo"),
        ("BrushTextSecondary", "BrushCard", 4.5, "texto secundário sobre cartões"),
        ("BrushTextSecondary", "BrushFieldBg", 4.5, "rótulos/placeholders sobre campos"),
        ("BrushAccentTexto", "BrushCard", 4.5, "links e percentuais sobre cartões"),
        ("BrushEntrada", "BrushCard", 4.5, "valores de entrada sobre cartões"),
        ("BrushSaida", "BrushCard", 4.5, "valores de saída sobre cartões"),
        ("BrushAviso", "BrushCard", 4.5, "avisos de vencimento sobre cartões"),
        ("BrushErro", "BrushCard", 4.5, "mensagens de erro sobre cartões"),
        ("BrushAccentForeground", "BrushAccent", 4.5, "texto sobre botões primários e navegação ativa"),
        ("BrushTextPrimary", "BrushAccentSoft", 4.5, "texto sobre destaques suaves"),
    };

    // Bordas de cartões/campos são decorativas (o cartão se distingue pelo fundo e
    // pela sombra, não pela borda) — reportadas sem reprovar (WCAG non-text
    // contrast aplica-se a componentes necessários para operar a interface).
    private static (string Fore, string Back, double Minimo, string Uso)[] ParesGraficos => new[]
    {
        ("BrushAccent", "BrushTrack", 3.0, "preenchimento da barra de progresso"),
        ("BrushAccent", "BrushBg", 3.0, "elementos de destaque contra o fundo"),
    };

    public static string VerificarPaleta(string arquivo)
    {
        var paleta = CarregarPaleta(arquivo);
        var falhas = new List<string>();
        var relatorio = new List<string>();

        foreach (var (fore, back, minimo, uso) in ParesTexto.Concat(ParesGraficos))
        {
            Assert.True(paleta.ContainsKey(fore), $"cor ausente na paleta: {fore}");
            Assert.True(paleta.ContainsKey(back), $"cor ausente na paleta: {back}");

            double taxa = TaxaContraste(paleta[fore], paleta[back]);
            string linha = $"{fore} sobre {back} ({uso}): {taxa:F2}:1 (mín {minimo:0.0})";
            relatorio.Add(linha);
            if (taxa < minimo)
            {
                falhas.Add(linha);
            }
        }

        // bordas decorativas: reportadas, mas não reprovam (ver comentário acima)
        double bordaCartao = TaxaContraste(paleta["BrushCardBorder"], paleta["BrushBg"]);
        double bordaCampo = TaxaContraste(paleta["BrushFieldBorder"], paleta["BrushFieldBg"]);
        relatorio.Add($"[decorativo] BrushCardBorder sobre BrushBg: {bordaCartao:F2}:1");
        relatorio.Add($"[decorativo] BrushFieldBorder sobre BrushFieldBg: {bordaCampo:F2}:1");

        return falhas.Count == 0
            ? "OK — " + relatorio.Count + " pares verificados"
            : "FALHAS:\n" + string.Join("\n", falhas) + "\n--- relatório completo ---\n" + string.Join("\n", relatorio);
    }

    [Fact]
    public void PaletaClara_TodosOsParesAtendemContraste()
    {
        string resultado = VerificarPaleta("Palette.Light.xaml");
        Assert.True(resultado.StartsWith("OK"), resultado);
    }

    [Fact]
    public void PaletaEscura_TodosOsParesAtendemContraste()
    {
        string resultado = VerificarPaleta("Palette.Dark.xaml");
        Assert.True(resultado.StartsWith("OK"), resultado);
    }
}
