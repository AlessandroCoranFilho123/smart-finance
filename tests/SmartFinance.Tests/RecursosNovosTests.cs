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

        // saldo descontou o valor total (dinheiro comprometido com o cartão — D-10)
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

    [Fact]
    public void F2_EditarCompra_PreservaDataDaMovimentacao()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 5000_00);
        _app.Cartao.Salvar(null, "Notebook", 1200_00, 12, 0, Hoje, Database.CarteiraBancoId);
        var movimentacao = _app.TransacaoRepo.ListarTodas().Single();

        // re-data a movimentação para o mês anterior (competência)
        var competencia = Hoje.AddDays(-30);
        _app.Transacoes.Atualizar(movimentacao.Id, TipoTransacao.Saida, Categoria.Compras,
            movimentacao.ValorCentavos, null, movimentacao.Comentario, competencia, movimentacao.CarteiraId);

        // editar a compra não pode re-datar a movimentação (D-13)
        var compra = _app.Cartao.ListarTodas().Single();
        _app.Cartao.Salvar(compra.Id, "Notebook novo", 1500_00, 12, 0, compra.Vencimento, Database.CarteiraBancoId);

        var apos = _app.TransacaoRepo.ListarTodas().Single();
        Assert.Equal(competencia, apos.Data);
        Assert.Equal(1500_00, apos.ValorCentavos);
    }

    [Fact]
    public void F2_EstornoReverteMeta_QuandoMovimentacaoFoiReclassificada()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 500_00);
        _app.Cartao.Salvar(null, "Notebook", 100_00, 2, 0, Hoje, Database.CarteiraBancoId);
        var movimentacao = _app.TransacaoRepo.ListarTodas().Single();

        // usuário reclassifica a movimentação "Compra no cartão" como Aplicar em meta
        _app.Metas.CriarMeta("Reserva", 1000_00);
        var meta = _app.Metas.ListarTodasMetas().Single();
        _app.Transacoes.Atualizar(movimentacao.Id, TipoTransacao.Saida, Categoria.AdicionarMeta,
            movimentacao.ValorCentavos, meta, "", movimentacao.Data, movimentacao.CarteiraId);
        Assert.Equal(100_00, _app.Metas.BuscarPorId(meta.Id)!.AtualCentavos);

        // excluir a compra precisa reverter a meta — o DELETE direto criava
        // dinheiro do nada (meta acumulada + carteira de volta)
        _app.Cartao.Deletar(_app.Cartao.ListarTodas().Single().Id);

        Assert.Equal(0, _app.Metas.BuscarPorId(meta.Id)!.AtualCentavos);
        Assert.Equal(500_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
        Assert.Empty(_app.TransacaoRepo.ListarTodas());
    }

    [Fact]
    public void F2_EstornoBloqueado_QuandoMetaParcialmenteResgatada()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 500_00);
        _app.Cartao.Salvar(null, "Notebook", 100_00, 2, 0, Hoje, Database.CarteiraBancoId);
        var movimentacao = _app.TransacaoRepo.ListarTodas().Single();

        _app.Metas.CriarMeta("Reserva", 1000_00);
        var meta = _app.Metas.ListarTodasMetas().Single();
        _app.Transacoes.Atualizar(movimentacao.Id, TipoTransacao.Saida, Categoria.AdicionarMeta,
            movimentacao.ValorCentavos, meta, "", movimentacao.Data, movimentacao.CarteiraId);

        // resgate parcial da aplicação: excluir a original criaria dinheiro (SF-006)
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.RetirarMeta, 60_00, meta, "", Hoje,
            Database.CarteiraBancoId);

        Assert.Throws<ArgumentException>(() => _app.Cartao.Deletar(_app.Cartao.ListarTodas().Single().Id));
        Assert.Equal(40_00, _app.Metas.BuscarPorId(meta.Id)!.AtualCentavos);
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

    [Fact]
    public void F2_EditarEmprestimo_PreservaDataDaMovimentacao()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 300_00);
        _app.Emprestimos.Salvar(null, TipoEmprestimo.Emprestei, "Ana", 200_00, Hoje.AddDays(10), Database.CarteiraBancoId);
        var movimentacao = _app.TransacaoRepo.ListarTodas().Single();

        // re-data a movimentação para o mês anterior (competência)
        var competencia = Hoje.AddDays(-30);
        _app.Transacoes.Atualizar(movimentacao.Id, TipoTransacao.Saida, Categoria.Emprestimo,
            movimentacao.ValorCentavos, null, movimentacao.Comentario, competencia, movimentacao.CarteiraId);

        // editar o empréstimo não pode re-datar a movimentação (D-13)
        var emprestimo = _app.Emprestimos.ListarTodas().Single();
        _app.Emprestimos.Salvar(emprestimo.Id, TipoEmprestimo.Emprestei, "Ana", 250_00, emprestimo.DataPagamento,
            Database.CarteiraBancoId);

        var apos = _app.TransacaoRepo.ListarTodas().Single();
        Assert.Equal(competencia, apos.Data);
        Assert.Equal(250_00, apos.ValorCentavos);
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
        // arquivo de controle em temp: tocar o %APPDATA% real sujaria o estado
        // do usuário e bloquearia a checagem diária do app (auditoria 15/11/2026)
        var original = AtualizacaoService.CaminhoControle;
        var temporario = Path.Combine(Path.GetTempPath(), $"sf-f5-{Guid.NewGuid():N}.json");
        AtualizacaoService.CaminhoControle = temporario;
        try
        {
            Assert.True(AtualizacaoService.DeveVerificarHoje()); // sem arquivo de controle
            AtualizacaoService.MarcarVerificadoHoje();
            Assert.False(AtualizacaoService.DeveVerificarHoje());
            AtualizacaoService.MarcarVerificadoHoje(); // idempotente no mesmo dia
            Assert.False(AtualizacaoService.DeveVerificarHoje());
        }
        finally
        {
            AtualizacaoService.CaminhoControle = original;
            File.Delete(temporario);
        }
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

    // ── F6: atualização automática (hash + integridade) ──────────────────────

    [Fact]
    public void F6_ExtrairHashSetup_DoCorpoDasNotas()
    {
        const string corpo = """
            ## Verificação de integridade

            SHA-256 do `SmartFinance.exe`: `1111111111111111111111111111111111111111111111111111111111111111`
            SHA-256 do `SmartFinance-Setup-3.4.0.exe`: `2222222222222222222222222222222222222222222222222222222222222222`
            """;

        Assert.Equal(new string('2', 64), AtualizacaoService.ExtrairHashSetup(corpo));
        Assert.Null(AtualizacaoService.ExtrairHashSetup("notas sem hash"));
        Assert.Null(AtualizacaoService.ExtrairHashSetup(null));
    }

    [Fact]
    public void F6_VerificarIntegridade_ConfereSha256()
    {
        string arquivo = Path.Combine(Path.GetTempPath(), "sf-f6-" + Guid.NewGuid().ToString("N"));
        File.WriteAllText(arquivo, "conteúdo de teste");

        using var sha = System.Security.Cryptography.SHA256.Create();
        string hashReal = Convert.ToHexString(sha.ComputeHash(File.ReadAllBytes(arquivo))).ToLowerInvariant();

        Assert.True(AtualizacaoService.VerificarIntegridade(arquivo, hashReal));
        Assert.False(AtualizacaoService.VerificarIntegridade(arquivo, new string('0', 64)));
        File.Delete(arquivo);
    }

        public void Dispose() => _app.Dispose();
}
