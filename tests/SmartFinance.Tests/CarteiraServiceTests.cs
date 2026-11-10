using SmartFinance.Data;
using SmartFinance.Models;

namespace SmartFinance.Tests;

public class CarteiraServiceTests : IDisposable
{
    private readonly TestApp _app = new();
    private static readonly DateOnly Hoje = new(2026, 11, 10);

    [Fact]
    public void CarteirasPadrao_SaoCriadasComSaldoZero()
    {
        var resumos = _app.Carteiras.ListarResumos();

        Assert.Equal(2, resumos.Count);
        Assert.Contains(resumos, r => r.Carteira.Id == Database.CarteiraBancoId && r.Carteira.Nome == "Banco");
        Assert.Contains(resumos, r => r.Carteira.Id == Database.CarteiraDinheiroFisicoId && r.Carteira.Nome == "Dinheiro físico");
        Assert.All(resumos, r => Assert.Equal(0, r.SaldoCentavos));
    }

    [Fact]
    public void SaldoDaCarteira_SomaSaldoInicialTransacoesETransferencias()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 100_00);
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.Salario, 500_00, null, "", Hoje);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Aluguel, 200_00, null, "", Hoje);
        _app.Carteiras.Transferir(Database.CarteiraBancoId, Database.CarteiraDinheiroFisicoId, 50_00, Hoje, "saque");

        Assert.Equal(350_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
        Assert.Equal(50_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraDinheiroFisicoId));
        Assert.Equal(400_00, _app.Carteiras.CalcularSaldoTotalCentavos());
    }

    [Fact]
    public void Transferir_Validacoes()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 100_00);

        Assert.Throws<ArgumentException>(() =>
            _app.Carteiras.Transferir(Database.CarteiraBancoId, Database.CarteiraBancoId, 10_00, Hoje, ""));
        Assert.Throws<ArgumentException>(() =>
            _app.Carteiras.Transferir(Database.CarteiraBancoId, Database.CarteiraDinheiroFisicoId, 0, Hoje, ""));
        Assert.Throws<ArgumentException>(() =>
            _app.Carteiras.Transferir(Database.CarteiraBancoId, Database.CarteiraDinheiroFisicoId, 200_00, Hoje, ""));

        Assert.Equal(100_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
        Assert.Equal(0, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraDinheiroFisicoId));
    }

    [Fact]
    public void Transferir_ComSaldoSuficiente_MoveValorEntreCarteiras()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 100_00);

        _app.Carteiras.Transferir(Database.CarteiraBancoId, Database.CarteiraDinheiroFisicoId, 30_00, Hoje, "depósito");

        Assert.Equal(70_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
        Assert.Equal(30_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraDinheiroFisicoId));
    }

    [Fact]
    public void AtualizarSaldoInicial_Negativo_Lanca()
    {
        Assert.Throws<ArgumentException>(() =>
            _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, -1));
    }

    [Fact]
    public void AtualizarSaldoInicial_MenorQueMovimentacoes_Lanca()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 100_00);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Aluguel, 80_00, null, "", Hoje);

        var excecao = Assert.Throws<ArgumentException>(() =>
            _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 50_00));

        Assert.Equal("Saldo inicial insuficiente para cobrir movimentações da carteira", excecao.Message);
        Assert.Equal(20_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
    }

    public void Dispose() => _app.Dispose();
}
