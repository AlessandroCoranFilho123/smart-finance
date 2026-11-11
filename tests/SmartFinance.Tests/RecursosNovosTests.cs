using SmartFinance.Data;
using SmartFinance.Models;
using SmartFinance.Services;

namespace SmartFinance.Tests;

/// <summary>Testes da rodada 4 (F1/F2/F5): parcelas pagas, movimentações de
/// carteira em cartão/empréstimos e verificação de atualização.</summary>
public class RecursosNovosTests : IDisposable
{
    private readonly TestApp _app = new();
    private static readonly DateOnly Hoje = new(2026, 11, 11);

    // ── F1/D-11: parcelas pagas amortizam o em aberto ─────────────────────

    [Fact]
    public void F1_TotalAberto_AmortizaComParcelasPagas()
    {
        _app.Cartao.Salvar(null, "Notebook", 300_00, 3, 0, Hoje, Database.CarteiraBancoId);
        var compra = _app.Cartao.ListarTodas().Single();
        Assert.Equal(300_00, _app.Cartao.CalcularTotalAbertoCentavos());

        _app.Cartao.Salvar(compra.Id, compra.Nome, compra.ValorTotalCentavos, compra.Parcelas, 1, compra.Vencimento, Database.CarteiraBancoId);
        Assert.Equal(200_00, _app.Cartao.CalcularTotalAbertoCentavos()); // 2 restantes × 100

        _app.Cartao.Salvar(compra.Id, compra.Nome, compra.ValorTotalCentavos, compra.Parcelas, 3, compra.Vencimento, Database.CarteiraBancoId);
        Assert.Equal(0, _app.Cartao.CalcularTotalAbertoCentavos());
        Assert.Equal(0, _app.Cartao.CalcularTotalParcelasMesCentavos()); // quitada não conta no mês
    }

    [Fact]
    public void F1_ParcelasPagas_ForadeFaixa_SaoRecusadas()
    {
        Assert.Throws<ArgumentException>(() =>
            _app.Cartao.Salvar(null, "X", 100_00, 3, 4, Hoje, Database.CarteiraBancoId));
        Assert.Throws<ArgumentException>(() =>
            _app.Cartao.Salvar(null, "X", 100_00, 3, -1, Hoje, Database.CarteiraBancoId));
    }

    [Fact]
    public void F1_VencimentoContagem_IgnoraComprasQuitadas()
    {
        _app.Cartao.Salvar(null, "Antiga", 100_00, 1, 1, Hoje.AddDays(2), Database.CarteiraBancoId);
        _app.Cartao.Salvar(null, "Nova", 100_00, 1, 0, Hoje.AddDays(3), Database.CarteiraBancoId);

        Assert.Equal(1, _app.Cartao.ContarVencendoEmAte(7, Hoje));
    }

    // ── F2/D-10: compra no cartão gera saída na carteira ──────────────────

    [Fact]
    public void F2_CompraNoCartao_GeraSaidaNaCarteira()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);

        _app.Cartao.Salvar(null, "Notebook", 4800_00, 12, 0, Hoje, Database.CarteiraBancoId);

        // saldo descontou o valor total (dinheiro comprometido com o cartão)
        Assert.Equal(1000_00 - 4800_00 + 0, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId) + 4800_00 - 4800_00);
        Assert.Equal(-3800_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));

        var movimentacao = _app.TransacaoRepo.ListarTodas().Single();
        Assert.Equal(4800_00, movimentacao.ValorCentavos);
        Assert.Equal(TipoTransacao.Saida, movimentacao.Tipo);
        Assert.Equal("Compra no cartão: Notebook", movimentacao.Descricao);

        // a compra ficou vinculada à movimentação
        var compra = _app.Cartao.ListarTodas().Single();
        Assert.Equal(movimentacao.Id, compra.TransacaoId);
    }

    [Fact]
    public void F2_ExcluirCompra_EstornaMovimentacao()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 5000_00);
        _app.Cartao.Salvar(null, "Notebook", 1200_00, 12, 0, Hoje, Database.CarteiraBancoId);
        Assert.Equal(3800_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));

        _app.Cartao.Deletar(_app.Cartao.ListarTodas().Single().Id);

        Assert.Equal(5000_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
        Assert.Empty(_app.TransacaoRepo.ListarTodas());
    }

    [Fact]
    public void F2_EditarCompra_ReajustaMovimentacao()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 5000_00);
        _app.Cartao.Salvar(null, "Notebook", 1200_00, 12, 0, Hoje, Database.CarteiraBancoId);
        var compra = _app.Cartao.ListarTodas().Single();

        _app.Cartao.Salvar(compra.Id, compra.Nome, 1500_00, 10, 2, compra.Vencimento, Database.CarteiraDinheiroFisicoId);

        // movimentação antiga substituída: saída nova de 1500 na carteira dinheiro
        var movimentacoes = _app.TransacaoRepo.ListarTodas();
        Assert.Single(movimentacoes);
        Assert.Equal(1500_00, movimentacoes[0].ValorCentavos);
        Assert.Equal(Database.CarteiraDinheiroFisicoId, movimentacoes[0].CarteiraId);
        // estorno devolveu 1200 ao Banco; a nova saída de 1500 caiu no Dinheiro físico
        Assert.Equal(5000_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
        Assert.Equal(-1500_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraDinheiroFisicoId));
    }

    // ── F2/D-10: empréstimos movem a carteira ─────────────────────────────

    [Fact]
    public void F2_Emprestei_GeraSaidaNaCarteira()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);

        _app.Emprestimos.Salvar(null, TipoEmprestimo.Emprestei, "Ana", 200_00, Hoje.AddDays(10), Database.CarteiraBancoId);

        Assert.Equal(800_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
        var movimentacao = _app.TransacaoRepo.ListarTodas().Single();
        Assert.Equal(TipoTransacao.Saida, movimentacao.Tipo);
        Assert.Equal(Categoria.Emprestimo, movimentacao.Categoria);
        Assert.Equal("Empréstimo concedido: Ana", movimentacao.Descricao);
    }

    [Fact]
    public void F2_PediEmprestado_GeraEntradaNaCarteira()
    {
        _app.Emprestimos.Salvar(null, TipoEmprestimo.PediEmprestado, "Carlos", 300_00, Hoje, Database.CarteiraBancoId);

        Assert.Equal(300_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
        var movimentacao = _app.TransacaoRepo.ListarTodas().Single();
        Assert.Equal(TipoTransacao.Entrada, movimentacao.Tipo);
        Assert.Equal("Empréstimo recebido: Carlos", movimentacao.Descricao);
    }

    [Fact]
    public void F2_ExcluirEmprestimo_EstornaMovimentacao()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
        _app.Emprestimos.Salvar(null, TipoEmprestimo.Emprestei, "Ana", 200_00, Hoje, Database.CarteiraBancoId);
        var emprestimo = _app.Emprestimos.ListarTodas().Single();

        _app.Emprestimos.Deletar(emprestimo.Id);

        Assert.Equal(1000_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
        Assert.Empty(_app.TransacaoRepo.ListarTodas());
    }

    [Fact]
    public void F2_LegadoSemMovimentacao_ExcluirNaoFalha()
    {
        // registros 2.x não têm transacao_id: exclusão não pode estourar
        _app.EmprestimoRepo.Inserir(new Emprestimo(Guid.NewGuid(), TipoEmprestimo.Emprestei, "Legado", 100_00, Hoje));

        var excecao = Record.Exception(() => _app.Emprestimos.Deletar(_app.Emprestimos.ListarTodas().Single().Id));
        Assert.Null(excecao);
    }

    // ── F5: verificação de atualização ────────────────────────────────────

    [Theory]
    [InlineData("3.3.0", true)]
    [InlineData("v3.3.0", true)]
    [InlineData("3.2.0", false)]
    [InlineData("3.1.9", false)]
    [InlineData("x.y.z", false)]
    public void F5_EhMaisNova_ComparaVersoes(string tag, bool esperado)
    {
        Assert.Equal(esperado, AtualizacaoService.EhMaisNova(tag, new Version(3, 2, 0)));
    }

    [Fact]
    public void F5_DeveVerificarHoje_MarcaUmaVezPorDia()
    {
        Directory.CreateDirectory(Path.GetDirectoryName(AtualizacaoService.CaminhoControle)!);
        File.Delete(AtualizacaoService.CaminhoControle); // estado limpo, independente da máquina

        Assert.True(AtualizacaoService.DeveVerificarHoje()); // sem arquivo de controle
        AtualizacaoService.MarcarVerificadoHoje();
        Assert.False(AtualizacaoService.DeveVerificarHoje());
        AtualizacaoService.MarcarVerificadoHoje(); // idempotente no mesmo dia
        Assert.False(AtualizacaoService.DeveVerificarHoje());
    }

    // ── T-R: reconciliação incluindo cartão e empréstimo ──────────────────

    [Fact]
    public void TR_CenarioComCartaoEEmprestimo_ConservaODinheiro()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 2000_00);
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.Salario, 1500_00, null, "", Hoje);
        _app.Cartao.Salvar(null, "Notebook", 1200_00, 12, 0, Hoje, Database.CarteiraBancoId);
        _app.Emprestimos.Salvar(null, TipoEmprestimo.PediEmprestado, "Carlos", 300_00, Hoje, Database.CarteiraBancoId);

        long movimentosExternos = _app.TransacaoRepo.ListarTodas()
            .Where(t => t.Categoria != Categoria.AdicionarMeta && t.Categoria != Categoria.RetirarMeta)
            .Sum(t => t.Tipo == TipoTransacao.Entrada ? t.ValorCentavos : -t.ValorCentavos);

        long ladoEsquerdo = _app.Carteiras.CalcularSaldoTotalCentavos();
        long ladoDireito = 2000_00 + movimentosExternos;

        Assert.Equal(ladoDireito, ladoEsquerdo);
    }

    public void Dispose() => _app.Dispose();
}
