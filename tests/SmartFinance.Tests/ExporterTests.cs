using SmartFinance.Models;
using SmartFinance.Services;

namespace SmartFinance.Tests;

public class ExporterTests
{
    private static readonly DateOnly Data = new(2026, 11, 10);

    private static List<Transacao> Amostra() => new()
    {
        new Transacao(Guid.NewGuid(), "Salário", "", 1500_00, TipoTransacao.Entrada, Data, null, Categoria.Salario),
        new Transacao(Guid.NewGuid(), "Alimentação", "mercado; com vírgula", 100_50, TipoTransacao.Saida, Data.AddDays(-1), null, Categoria.Alimentacao),
    };

    [Fact]
    public void Csv_ComCabecalhoOrdemCrescenteEEscapamento()
    {
        var linhas = TransacaoCsvExporter.Formatar(Amostra()).Split('\n');

        Assert.StartsWith("Data;Tipo;Descricao;Categoria;Comentario;Valor", linhas[0]);
        Assert.Contains("09/11/2026;Saida;Alimentação;Alimentacao;\"mercado; com vírgula\";100,50", linhas[1]);
        Assert.Contains("10/11/2026;Entrada;Salário;Salario;;1500,00", linhas[2]);
    }

    [Fact]
    public void Csv_Exportar_GravaComBom()
    {
        string destino = Path.Combine(Path.GetTempPath(), "sf-test-" + Guid.NewGuid().ToString("N") + ".csv");
        try
        {
            TransacaoCsvExporter.Exportar(destino, Amostra());
            byte[] bytes = File.ReadAllBytes(destino);
            Assert.Equal(0xEF, bytes[0]);
            Assert.Equal(0xBB, bytes[1]);
            Assert.Equal(0xBF, bytes[2]);
        }
        finally
        {
            File.Delete(destino);
        }
    }

    [Fact]
    public void Txt_IncluiResumoEDetalhes()
    {
        string conteudo = TransacaoTxtExporter.Formatar(Amostra());

        Assert.Contains("Total de Entradas: R$ 1.500,00", conteudo);
        Assert.Contains("Total de Saídas: R$ 100,50", conteudo);
        Assert.Contains("1. 10/11/2026 | Entrada | Salário | R$ 1.500,00", conteudo);
        Assert.Contains("Comentário: mercado; com vírgula", conteudo);
        Assert.Contains("Categoria: Alimentacao", conteudo);
    }

    [Fact]
    public void Txt_ListaVazia()
    {
        string conteudo = TransacaoTxtExporter.Formatar(new List<Transacao>());
        Assert.Contains("Nenhuma transação encontrada.", conteudo);
    }
}
