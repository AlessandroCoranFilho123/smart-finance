using SmartFinance.Services;

namespace SmartFinance.Tests;

/// <summary>Formatação pt-BR explícita: os valores exibidos na UI dependem
/// destas funções e NÃO podem depender da cultura da máquina (SF-019) —
/// por isso cada assert fixa o resultado literal esperado.</summary>
public class FormatacaoTests
{
    [Theory]
    [InlineData(0, "R$ 0,00")]
    [InlineData(5, "R$ 0,05")]
    [InlineData(100, "R$ 1,00")]
    [InlineData(123456, "R$ 1.234,56")]
    [InlineData(-150, "-R$ 1,50")]
    public void Moeda_FormataComCulturaBr(long centavos, string esperado)
    {
        // negativo: sinal ANTES do símbolo ("-R$ 1,50"), como o "C2" pt-BR produz

        Assert.Equal(esperado, Formatacao.Moeda(centavos));
    }

    [Theory]
    [InlineData(100, true, "+ R$ 1,00")]
    [InlineData(100, false, "- R$ 1,00")]
    [InlineData(420000, true, "+ R$ 4.200,00")]
    [InlineData(450, false, "- R$ 4,50")]
    public void MoedaComSinal_PrefixaSegundoOTipo(long centavos, bool entrada, string esperado)
    {
        Assert.Equal(esperado, Formatacao.MoedaComSinal(centavos, entrada));
    }

    [Theory]
    [InlineData("2026-11-15", "15/11/2026")]
    [InlineData("2026-01-05", "05/01/2026")]
    public void Data_FormataDiaMesAno(string iso, string esperado)
    {
        Assert.Equal(esperado, Formatacao.Data(DateOnly.Parse(iso)));
    }
}
