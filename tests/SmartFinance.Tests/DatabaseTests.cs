using Microsoft.Data.Sqlite;
using SmartFinance.Data;
using SmartFinance.Models;

namespace SmartFinance.Tests;

public class DatabaseTests
{
    private static readonly DateOnly Hoje = new(2026, 11, 10);

    [Fact]
    public void Initialize_CriaTabelasECarteirasPadrao()
    {
        using var app = new TestApp();

        var carteiras = app.Carteiras.ListarCarteiras();
        Assert.Equal(2, carteiras.Count);

        using var comando = app.Db.Connection.CreateCommand();
        comando.CommandText = "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name IN ('meta', 'carteira', 'transacao', 'compra_cartao_credito', 'emprestimo', 'transferencia_carteira', 'transacao_fts')";
        Assert.Equal(7L, (long)comando.ExecuteScalar()!);
    }

    [Fact]
    public void Initialize_MigraBancoLegadoSemColunasNovas()
    {
        string dir = Path.Combine(Path.GetTempPath(), "sf-tests-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(dir);
        string caminho = Path.Combine(dir, "legado.db");

        try
        {
            // Simula um banco da versão Java 2.0: transacao sem comentario/categoria/carteira_id
            var builder = new SqliteConnectionStringBuilder { DataSource = caminho, Pooling = false };
            using (var conn = new SqliteConnection(builder.ToString()))
            {
                conn.Open();
                Executar(conn, "CREATE TABLE meta (id TEXT PRIMARY KEY, nome TEXT NOT NULL, alvo_centavos INTEGER, atual_centavos INTEGER NOT NULL DEFAULT 0)");
                Executar(conn, "CREATE TABLE carteira (id TEXT PRIMARY KEY, nome TEXT NOT NULL UNIQUE, saldo_inicial_centavos INTEGER NOT NULL DEFAULT 0)");
                Executar(conn, "INSERT INTO carteira (id, nome, saldo_inicial_centavos) VALUES ('00000000-0000-0000-0000-000000000001', 'Banco', 0), ('00000000-0000-0000-0000-000000000002', 'Dinheiro físico', 0)");
                Executar(conn, "CREATE TABLE transacao (id TEXT PRIMARY KEY, descricao TEXT NOT NULL, valor_centavos INTEGER NOT NULL, tipo TEXT NOT NULL, data TEXT NOT NULL, meta_id TEXT)");
                Executar(conn, "INSERT INTO transacao (id, descricao, valor_centavos, tipo, data) VALUES ('11111111-1111-1111-1111-111111111111', 'Salário', 100000, 'Entrada', '2026-01-15')");
            }

            using var db = new Database(caminho);
            db.Initialize();
            var repo = new TransacaoRepository(db);
            var transacao = repo.BuscarPorId(new Guid("11111111-1111-1111-1111-111111111111"))!;

            Assert.Equal("Salário", transacao.Descricao);
            Assert.Equal(Database.CarteiraBancoId, transacao.CarteiraId);
            Assert.Equal(string.Empty, transacao.Comentario);
            Assert.Null(transacao.Categoria);
        }
        finally
        {
            TryDelete(dir);
        }
    }

    [Fact]
    public void BuscaTextual_EncontraPorPrefixoSemAcento()
    {
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 500_00);
        app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Alimentacao, 50_00, null, "mercado do João", Hoje);
        app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Educacao, 100_00, null, "curso inglês", Hoje);

        var porPrefixo = app.TransacaoRepo.ListarPorFiltro(new TransacaoFiltro("merca", null, null, null, null));
        var semAcento = app.TransacaoRepo.ListarPorFiltro(new TransacaoFiltro("ingles", null, null, null, null));
        var variosTermos = app.TransacaoRepo.ListarPorFiltro(new TransacaoFiltro("mercado joao", null, null, null, null));
        var semResultado = app.TransacaoRepo.ListarPorFiltro(new TransacaoFiltro("viagem", null, null, null, null));

        Assert.Single(porPrefixo);
        Assert.Single(semAcento);
        Assert.Single(variosTermos);
        Assert.Empty(semResultado);
    }

    [Fact]
    public void BuscaTextual_ExpurgoAutomaticoAoExcluir()
    {
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 500_00);
        app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Compras, 50_00, null, "playstation", Hoje);
        var transacao = app.TransacaoRepo.ListarTodas().Single();

        app.Transacoes.ExcluirTransacao(transacao.Id);

        Assert.Empty(app.TransacaoRepo.ListarPorFiltro(new TransacaoFiltro("playstation", null, null, null, null)));
    }

    [Fact]
    public void FiltroCombinado_AplicaTipoCategoriaEPeriodo()
    {
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 2000_00);
        app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.Salario, 1000_00, null, "", new DateOnly(2026, 11, 1));
        app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Alimentacao, 10_00, null, "", new DateOnly(2026, 11, 2));
        app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Alimentacao, 20_00, null, "", new DateOnly(2026, 10, 2));

        var filtro = new TransacaoFiltro("", TipoTransacao.Saida, Categoria.Alimentacao, new DateOnly(2026, 11, 1), new DateOnly(2026, 11, 30));
        var resultado = app.TransacaoRepo.ListarPorFiltro(filtro);

        Assert.Single(resultado);
        Assert.Equal(10_00, resultado[0].ValorCentavos);
    }

    [Fact]
    public void EmTransacao_Nested_ChamaApenasUmaTransacaoReal()
    {
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 100_00);

        app.Db.EmTransacao(() => app.Db.EmTransacao(() =>
        {
            app.TransacaoRepo.Inserir(new Transacao(
                Guid.NewGuid(), "Teste", "", 10_00, TipoTransacao.Entrada, Hoje, null, Categoria.Salario));
        }));

        Assert.Single(app.TransacaoRepo.ListarTodas());
    }

    private static void Executar(SqliteConnection conn, string sql)
    {
        using var comando = conn.CreateCommand();
        comando.CommandText = sql;
        comando.ExecuteNonQuery();
    }

    private static void TryDelete(string dir)
    {
        try
        {
            Directory.Delete(dir, recursive: true);
        }
        catch (IOException)
        {
        }
    }
}
