using SmartFinance.Services;

namespace SmartFinance.Tests;

public class ValorMonetarioTests
{
    [Theory]
    [InlineData("1.234,56", 123456)]
    [InlineData("10,50", 1050)]
    [InlineData("10", 1000)]
    [InlineData("10.50", 1050)]
    [InlineData("R$ 1.234,56", 123456)]
    [InlineData("R$1.234,56", 123456)]
    [InlineData("0,05", 5)]
    [InlineData("1.005", 100500)]
    [InlineData(" 2,00 ", 200)]
    [InlineData("1,005", 101)]
    public void ParseCentavos_ConverteValoresBr(string texto, long esperado)
    {
        Assert.Equal(esperado, ValorMonetario.ParseCentavos(texto));
    }

    [Theory]
    [InlineData(null, 0)]
    [InlineData("", 0)]
    [InlineData("   ", 0)]
    public void ParseCentavos_TextoVazio_RetornaZero(string? texto, long esperado)
    {
        Assert.Equal(esperado, ValorMonetario.ParseCentavos(texto));
    }

    [Theory]
    [InlineData("abc")]
    [InlineData("10,5,0")]
    [InlineData("--10")]
    public void ParseCentavos_TextoInvalido_LancaArgumentException(string texto)
    {
        Assert.Throws<ArgumentException>(() => ValorMonetario.ParseCentavos(texto));
    }

    [Theory]
    [InlineData(123456, "1234.56")]
    [InlineData(5, "0.05")]
    [InlineData(0, "0.00")]
    [InlineData(1000000, "10000.00")]
    public void FormatarCentavos_FormataDuasCasas(long centavos, string esperado)
    {
        Assert.Equal(esperado, ValorMonetario.FormatarCentavos(centavos));
    }

    [Fact]
    public void ParseEFormatar_SaoInversos()
    {
        long centavos = ValorMonetario.ParseCentavos("1.234,56");
        Assert.Equal(123456L, centavos);
        Assert.Equal("1234.56", ValorMonetario.FormatarCentavos(centavos));
    }
}
