using SmartFinance.Data;
using SmartFinance.Models;

namespace SmartFinance.Tests;

public class CartaoEmprestimoMetaServiceTests : IDisposable
{
    private readonly TestApp _app = new();
    private static readonly DateOnly Hoje = new(2026, 11, 10);

    // ── Cartão de crédito ─────────────────────────────────────────────────

    [Fact]
    public void SalvarCompra_Nova_InsereComIdGerado()
    {
        _app.Cartao.Salvar(null, "Notebook", 1200_00, 12, Hoje);

        var compra = _app.Cartao.ListarTodas().Single();
        Assert.Equal("Notebook", compra.Nome);
        Assert.Equal(100_00, compra.ValorParcelaCentavos);
    }

    [Fact]
    public void SalvarCompra_Existente_Atualiza()
    {
        _app.Cartao.Salvar(null, "Notebook", 1200_00, 12, Hoje);
        var id = _app.Cartao.ListarTodas().Single().Id;

        _app.Cartao.Salvar(id, "Notebook gamer", 1500_00, 10, Hoje);

        var compra = _app.Cartao.ListarTodas().Single();
        Assert.Equal("Notebook gamer", compra.Nome);
        Assert.Equal(10, compra.Parcelas);
    }

    [Fact]
    public void SalvarCompra_Validacoes()
    {
        Assert.Throws<ArgumentException>(() => _app.Cartao.Salvar(null, "", 100_00, 1, Hoje));
        Assert.Throws<ArgumentException>(() => _app.Cartao.Salvar(null, "X", 0, 1, Hoje));
        Assert.Throws<ArgumentException>(() => _app.Cartao.Salvar(null, "X", 100_00, 0, Hoje));
    }

    [Fact]
    public void ValorParcela_UsaArredondamentoPadrao() // SF-026: nome antigo descrevia o oposto
    {
        var compra = new CompraCartaoCredito(Guid.NewGuid(), "X", 100_00, 3, Hoje);
        Assert.Equal(33_33, compra.ValorParcelaCentavos);
    }

    [Fact]
    public void StatusVencimento_CoresPorProximidade()
    {
        var vencido = new CompraCartaoCredito(Guid.NewGuid(), "X", 100_00, 1, Hoje.AddDays(-2));
        var hoje = new CompraCartaoCredito(Guid.NewGuid(), "X", 100_00, 1, Hoje);
        var proximo = new CompraCartaoCredito(Guid.NewGuid(), "X", 100_00, 1, Hoje.AddDays(5));
        var distante = new CompraCartaoCredito(Guid.NewGuid(), "X", 100_00, 1, Hoje.AddDays(30));

        Assert.Equal(StatusVencimento.Vermelho, vencido.CalcularStatusVencimento(Hoje));
        Assert.Equal(StatusVencimento.Vermelho, hoje.CalcularStatusVencimento(Hoje));
        Assert.Equal(StatusVencimento.Laranja, proximo.CalcularStatusVencimento(Hoje));
        Assert.Equal(StatusVencimento.Verde, distante.CalcularStatusVencimento(Hoje));
        Assert.Equal("Vencido há 2 dia(s)", vencido.AvisoVencimento(Hoje));
        Assert.Equal("Vence hoje", hoje.AvisoVencimento(Hoje));
        Assert.Equal("Vence em 5 dia(s)", proximo.AvisoVencimento(Hoje));
        Assert.Equal("", distante.AvisoVencimento(Hoje));
    }

    [Fact]
    public void TotaisDoCartao_SomamValoresCorretos()
    {
        _app.Cartao.Salvar(null, "A", 300_00, 3, Hoje.AddDays(2));
        _app.Cartao.Salvar(null, "B", 500_00, 5, Hoje.AddDays(10));

        Assert.Equal(800_00, _app.Cartao.CalcularTotalAbertoCentavos());
        Assert.Equal(200_00, _app.Cartao.CalcularTotalParcelasMesCentavos());
        Assert.Equal(1, _app.Cartao.ContarVencendoEmAte(7, Hoje));
        Assert.Equal(2, _app.Cartao.ContarVencendoEmAte(15, Hoje));
    }

    // ── Empréstimos ───────────────────────────────────────────────────────

    [Fact]
    public void Emprestimos_TotaisPorTipo()
    {
        _app.Emprestimos.Salvar(null, TipoEmprestimo.Emprestei, "Ana", 100_00, Hoje);
        _app.Emprestimos.Salvar(null, TipoEmprestimo.Emprestei, "Bruno", 50_00, Hoje);
        _app.Emprestimos.Salvar(null, TipoEmprestimo.PediEmprestado, "Carlos", 200_00, Hoje);

        Assert.Equal(150_00, _app.Emprestimos.TotalPorTipoCentavos(TipoEmprestimo.Emprestei));
        Assert.Equal(200_00, _app.Emprestimos.TotalPorTipoCentavos(TipoEmprestimo.PediEmprestado));
        Assert.Equal(2, _app.Emprestimos.ListarPorTipo(TipoEmprestimo.Emprestei).Count);
    }

    [Fact]
    public void Emprestimos_Validacoes()
    {
        Assert.Throws<ArgumentException>(() => _app.Emprestimos.Salvar(null, TipoEmprestimo.Emprestei, "", 100_00, Hoje));
        Assert.Throws<ArgumentException>(() => _app.Emprestimos.Salvar(null, TipoEmprestimo.Emprestei, "Ana", -1, Hoje));
    }

    [Fact]
    public void Emprestimos_EditarEExcluir()
    {
        _app.Emprestimos.Salvar(null, TipoEmprestimo.Emprestei, "Ana", 100_00, Hoje);
        var id = _app.Emprestimos.ListarTodas().Single().Id;

        _app.Emprestimos.Salvar(id, TipoEmprestimo.PediEmprestado, "Ana", 150_00, Hoje);
        Assert.Equal(150_00, _app.Emprestimos.ListarTodas().Single().ValorCentavos);

        _app.Emprestimos.Deletar(id);
        Assert.Empty(_app.Emprestimos.ListarTodas());
    }

    // ── Metas ─────────────────────────────────────────────────────────────

    [Fact]
    public void Metas_CriarValidacoes()
    {
        Assert.Throws<ArgumentException>(() => _app.Metas.CriarMeta("   ", 100_00));
        Assert.Throws<ArgumentException>(() => _app.Metas.CriarMeta("Meta", 0));
    }

    [Fact]
    public void Metas_ProgressoEConclusao()
    {
        _app.Metas.CriarMeta("Viagem", 1000_00);
        var meta = _app.Metas.ListarTodasMetas().Single();

        Assert.False(_app.Metas.MetaConcluida(meta));
        Assert.Equal(0.0, _app.Metas.CalcularProgressoPercentual(meta));

        meta.Adicionar(500_00);
        _app.Metas.AtualizarMeta(meta);
        Assert.Equal(50.0, _app.Metas.CalcularProgressoPercentual(meta), 2);

        meta.Adicionar(500_00);
        _app.Metas.AtualizarMeta(meta);
        Assert.True(_app.Metas.MetaConcluida(meta));
        Assert.Equal(1, _app.Metas.ContarMetasConcluidas());
        Assert.Equal(0, _app.Metas.CalcularRestante(meta));
    }

    [Fact]
    public void Metas_SemAlvo_ProgresoZero()
    {
        var meta = new Meta("Livre", null);
        Assert.True(meta.SemAlvo);
        Assert.Equal(0.0, meta.Progresso);
        Assert.Equal(long.MaxValue, meta.RestanteParaAlvo);
    }

    [Fact]
    public void Metas_Excluir_SemValorAcumulado_RevinculaTransacoesParaNulo()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
        _app.Metas.CriarMeta("Viagem", 500_00);
        var meta = _app.Metas.ListarTodasMetas().Single();
        // transação vinculada sem acumulado na meta (referência direta via repositório)
        _app.TransacaoRepo.Inserir(new Transacao(
            Guid.NewGuid(), "Histórico", "", 100_00, TipoTransacao.Saida, Hoje, meta.Id, Categoria.Outros));

        _app.Metas.DeletarMeta(meta.Id);

        var transacao = _app.TransacaoRepo.ListarTodas().Single();
        Assert.Null(transacao.MetaId);
    }

    [Fact]
    public void Metas_Excluir_ComValorAcumulado_EhBloqueado()
    {
        // D-01: dinheiro aplicado não pode desaparecer com a meta
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
        _app.Metas.CriarMeta("Viagem", 500_00);
        var meta = _app.Metas.ListarTodasMetas().Single();
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 100_00, meta, "", Hoje);

        var excecao = Assert.Throws<ArgumentException>(() => _app.Metas.DeletarMeta(meta.Id));

        Assert.Contains("acumulados", excecao.Message);
        Assert.NotNull(_app.MetaRepo.BuscarPorId(meta.Id));
    }

    public void Dispose() => _app.Dispose();
}
