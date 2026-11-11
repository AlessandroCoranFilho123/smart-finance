using SmartFinance.Data;
using SmartFinance.Models;

namespace SmartFinance.Tests;

public class TransacaoServiceTests : IDisposable
{
    private readonly TestApp _app = new();
    private static readonly DateOnly Hoje = new(2026, 11, 10);

    [Fact]
    public void RegistrarEntrada_SalvaTransacaoComDescricaoDaCategoria()
    {
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.Salario, 100_00, null, "pagamento", Hoje);

        var transacao = _app.TransacaoRepo.ListarTodas().Single();
        Assert.Equal("Salário", transacao.Descricao);
        Assert.Equal(100_00, transacao.ValorCentavos);
        Assert.Equal("pagamento", transacao.Comentario);
        Assert.Equal(Database.CarteiraBancoId, transacao.CarteiraId);
    }

    [Fact]
    public void RegistrarSaida_SemSaldo_LancaEGravaNada()
    {
        var excecao = Assert.Throws<ArgumentException>(() =>
            _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Aluguel, 50_00, null, "", Hoje));

        Assert.Equal("Saldo insuficiente", excecao.Message);
        Assert.Empty(_app.TransacaoRepo.ListarTodas());
    }

    [Fact]
    public void RegistrarSaida_ComSaldoInicialSuficiente_Permite()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 100_00);

        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Aluguel, 100_00, null, "", Hoje);

        Assert.Equal(0, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
    }

    [Fact]
    public void RegistrarSaida_EmCarteiraEspecifica_ValidaSaldoDela()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 100_00);
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraDinheiroFisicoId, 5_00);

        var excecao = Assert.Throws<ArgumentException>(() =>
            _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Alimentacao, 10_00, null, "", Hoje, Database.CarteiraDinheiroFisicoId));

        Assert.Equal("Saldo insuficiente", excecao.Message);
        Assert.Empty(_app.TransacaoRepo.ListarTodas());
    }

    [Fact]
    public void RegistrarValorNegativoOuZero_Lanca()
    {
        Assert.Throws<ArgumentException>(() =>
            _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.Salario, 0, null, "", Hoje));
    }

    [Fact]
    public void RegistrarAplicarMeta_ClampaAoRestante()
    {
        var meta = CriarMeta("Viagem", 1000_00, atual: 900_00);

        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 500_00);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 500_00, meta, "", Hoje);

        Assert.Equal(1000_00, _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos); // SF-004
        var transacao = _app.TransacaoRepo.ListarTodas().Single();
        Assert.Equal(100_00, transacao.ValorCentavos);
        Assert.Equal(TipoTransacao.Saida, transacao.Tipo);
        Assert.Equal(meta.Id, transacao.MetaId);
    }

    [Fact]
    public void RegistrarAplicarMeta_JaAlcancada_Lanca()
    {
        var meta = CriarMeta("Viagem", 1000_00, atual: 1000_00);

        var excecao = Assert.Throws<ArgumentException>(() =>
            _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 100_00, meta, "", Hoje));

        Assert.Equal("Meta já alcançada", excecao.Message);
        Assert.Empty(_app.TransacaoRepo.ListarTodas());
    }

    [Fact]
    public void RegistrarResgateMeta_NaoRetiraMaisQueOSaldoDaMeta()
    {
        var meta = CriarMeta("Viagem", 1000_00, atual: 300_00);

        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.RetirarMeta, 500_00, meta, "", Hoje);

        Assert.Equal(0, _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos); // SF-004: banco é a fonte da verdade
        var transacao = _app.TransacaoRepo.ListarTodas().Single();
        Assert.Equal(300_00, transacao.ValorCentavos);
        Assert.Equal(TipoTransacao.Entrada, transacao.Tipo);
    }

    [Fact]
    public void RegistrarAplicarMeta_SemSaldoNaCarteira_LancaENaoMudaNada()
    {
        var meta = CriarMeta("Viagem", 1000_00, atual: 0);

        var excecao = Assert.Throws<ArgumentException>(() =>
            _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 100_00, meta, "", Hoje));

        Assert.Equal("Saldo insuficiente", excecao.Message);
        Assert.Equal(0, meta.AtualCentavos);
        Assert.Empty(_app.TransacaoRepo.ListarTodas());
        Assert.Equal(0, _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos);
    }

    [Fact]
    public void Atualizar_PreservaIdOriginal()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
        var id = NovaTransacao(TipoTransacao.Saida, Categoria.Aluguel, 100_00);

        _app.Transacoes.Atualizar(id, TipoTransacao.Saida, Categoria.Aluguel, 150_00, null, "reajuste", Hoje);

        var atualizada = _app.TransacaoRepo.BuscarPorId(id)!;
        Assert.Equal(150_00, atualizada.ValorCentavos);
        Assert.Equal("reajuste", atualizada.Comentario);
    }

    [Fact]
    public void Atualizar_ComValidacaoFalha_FazRollbackCompleto()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 100_00);
        var id = NovaTransacao(TipoTransacao.Saida, Categoria.Aluguel, 20_00);

        var excecao = Assert.Throws<ArgumentException>(() =>
            _app.Transacoes.Atualizar(id, TipoTransacao.Saida, Categoria.Aluguel, 120_00, null, "", Hoje));

        Assert.Equal("Saldo insuficiente", excecao.Message);
        var original = _app.TransacaoRepo.BuscarPorId(id)!;
        Assert.Equal(20_00, original.ValorCentavos);
        Assert.Equal(80_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
    }

    [Fact]
    public void Atualizar_DeComumParaMeta_AplicaRegrasDeMeta()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
        var id = NovaTransacao(TipoTransacao.Saida, Categoria.Compras, 100_00);
        var meta = CriarMeta("Notebook", 500_00, atual: 450_00);

        _app.Transacoes.Atualizar(id, TipoTransacao.Saida, Categoria.AdicionarMeta, 100_00, meta, "", Hoje);

        Assert.Equal(500_00, _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos); // SF-004
        var atualizada = _app.TransacaoRepo.BuscarPorId(id)!;
        Assert.Equal(Categoria.AdicionarMeta, atualizada.Categoria);
        Assert.Equal(50_00, atualizada.ValorCentavos); // clamp ao restante
        Assert.Equal(meta.Id, atualizada.MetaId);
    }

    [Fact]
    public void ExcluirTransacao_DeAplicacaoMeta_ReverteSaldoDaMeta()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 500_00);
        var meta = CriarMeta("Viagem", 1000_00, atual: 0);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 200_00, meta, "", Hoje);
        var transacao = _app.TransacaoRepo.ListarTodas().Single();

        _app.Transacoes.ExcluirTransacao(transacao.Id);

        Assert.Empty(_app.TransacaoRepo.ListarTodas());
        Assert.Equal(0, _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos);
        Assert.Equal(500_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
    }

    [Fact]
    public void ExcluirTransacao_DeResgateMeta_DevolveSaldoDaMeta()
    {
        var meta = CriarMeta("Viagem", 1000_00, atual: 300_00);
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.RetirarMeta, 300_00, meta, "", Hoje);
        var transacao = _app.TransacaoRepo.ListarTodas().Single();

        _app.Transacoes.ExcluirTransacao(transacao.Id);

        Assert.Equal(300_00, _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos);
    }

    [Fact]
    public void ReceitasEDespesasDoMes_ConsideramSoOMesAtual()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.Salario, 500_00, null, "", Hoje);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Aluguel, 200_00, null, "", Hoje);
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.Salario, 999_00, null, "", new DateOnly(2026, 10, 5));

        var mes = new DateOnly(2026, 11, 1);
        Assert.Equal(500_00, _app.Transacoes.CalcularReceitasMes(mes));
        Assert.Equal(200_00, _app.Transacoes.CalcularDespesasMes(mes));
    }

    private Guid NovaTransacao(TipoTransacao tipo, Categoria categoria, long valor, Meta? meta = null)
    {
        _app.Transacoes.Registrar(tipo, categoria, valor, meta, "", Hoje);
        return _app.TransacaoRepo.ListarTodas().Single().Id;
    }

    private Meta CriarMeta(string nome, long alvo, long atual = 0)
    {
        var meta = new Meta(Guid.NewGuid(), nome, alvo, atual);
        _app.MetaRepo.Inserir(meta);
        return meta;
    }

    public void Dispose() => _app.Dispose();
}
