using Microsoft.Data.Sqlite;
using SmartFinance.Data;
using SmartFinance.Models;

namespace SmartFinance.Tests;

/// <summary>
/// Testes de auditoria (fases 3-6): casos-limite de busca, integridade de dados,
/// concorrência, recuperação de falhas e volumes. Bugs confirmados aqui viram
/// regressão após a correção.
/// </summary>
public class AuditoriaTests
{
    private static readonly DateOnly Hoje = new(2026, 11, 10);

    // ── Busca textual (FTS) ───────────────────────────────────────────────

    [Fact]
    public void Busca_SomenteSimbolos_NaoDeveQuebrar()
    {
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 500_00);
        app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Alimentacao, 50_00, null, "mercado", Hoje);

        // "!!!" gera consulta FTS vazia; não pode lançar exceção
        var excecao = Record.Exception(() =>
            app.TransacaoRepo.ListarPorFiltro(new TransacaoFiltro("!!!", null, null, null, null)));

        Assert.Null(excecao);
    }

    [Fact]
    public void Busca_ComAcentosEPrefixo_Encontra()
    {
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 500_00);
        app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Educacao, 100_00, null, "Ação & inglês", Hoje);

        var resultado = app.TransacaoRepo.ListarPorFiltro(new TransacaoFiltro("acao", null, null, null, null));
        Assert.Single(resultado);
    }

    [Fact]
    public void Busca_VaziaOuEspacos_RetornaTudo()
    {
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 500_00);
        app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Alimentacao, 50_00, null, "mercado", Hoje);
        app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Luz, 80_00, null, "luz", Hoje);

        Assert.Equal(2, app.TransacaoRepo.ListarPorFiltro(new TransacaoFiltro("", null, null, null, null)).Count);
        Assert.Equal(2, app.TransacaoRepo.ListarPorFiltro(new TransacaoFiltro("   ", null, null, null, null)).Count);
    }

    // ── Dados e caracteres especiais ──────────────────────────────────────

    [Fact]
    public void Transacao_ComUnicodeEEmoji_SobreviveAoRoundtrip()
    {
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
        app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Compras, 10_00, null, "Café ☕ — 说中文 🎮", Hoje);

        var transacao = app.TransacaoRepo.ListarTodas().Single();
        Assert.Contains("说中文", transacao.Comentario);
        Assert.Contains("☕", transacao.Comentario);

        // FTS5 unicode61: token CJK contíguo ("说中文"); prefixo pela esquerda encontra,
        // infixos ("中文") não — limitação conhecida do tokenizer, idêntica à versão Java.
        var porPrefixo = app.TransacaoRepo.ListarPorFiltro(new TransacaoFiltro("说中", null, null, null, null));
        Assert.Single(porPrefixo);
        var porInfixo = app.TransacaoRepo.ListarPorFiltro(new TransacaoFiltro("中文", null, null, null, null));
        Assert.Empty(porInfixo);
    }

    [Fact]
    public void Transacao_ValoresGrandes_PermanecemExatos()
    {
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 999_999_999_99L);

        app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Compras, 999_999_999_99L, null, "", Hoje);

        Assert.Equal(999_999_999_99L, app.TransacaoRepo.ListarTodas().Single().ValorCentavos);
    }

    // ── Bootstrap e recuperação de falhas ─────────────────────────────────

    [Fact]
    public void Initialize_ChamadoDuasVezes_EhIdempotente()
    {
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 100_00);
        app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.Salario, 10_00, null, "", Hoje);

        var excecao = Record.Exception(() => app.Db.Initialize());

        Assert.Null(excecao);
        Assert.Single(app.TransacaoRepo.ListarTodas());
        Assert.Equal(2, app.Carteiras.ListarCarteiras().Count);
    }

    [Fact]
    public void ArquivoBanco_Vazio_InitializeCriaSchema()
    {
        string dir = NovoDir();
        string caminho = Path.Combine(dir, "vazio.db");
        File.WriteAllBytes(caminho, Array.Empty<byte>());

        using var db = new Database(caminho);
        var excecao = Record.Exception(() => db.Initialize());

        Assert.Null(excecao);
        Assert.Equal(2, new CarteiraRepository(db).ListarTodas().Count);
        TryDelete(dir);
    }

    [Fact]
    public void ArquivoBanco_Corrompido_FalhaComErroClaro()
    {
        string dir = NovoDir();
        string caminho = Path.Combine(dir, "corrompido.db");
        File.WriteAllBytes(caminho, new byte[] { 0xDE, 0xAD, 0xBE, 0xEF, 0x00, 0x01 });

        using var db = new Database(caminho);
        // Deve falhar com SqliteException (erro identificável), não NullReferenceException
        Assert.ThrowsAny<SqliteException>(() => db.Initialize());
        TryDelete(dir);
    }

    [Fact]
    public void DadoCorrompido_TipoInvalido_FalhaAoLer_ComErroIdentificavel()
    {
        using var app = new TestApp();
        ExecutarSql(app.Db, "INSERT INTO transacao (id, descricao, valor_centavos, tipo, data) VALUES ('1', 'x', 100, 'TipoInexistente', '2026-11-10')");

        var excecao = Record.Exception(() => app.TransacaoRepo.ListarTodas());

        Assert.IsNotType<NullReferenceException>(excecao);
        Assert.NotNull(excecao);
    }

    [Fact]
    public void ExcluirTransacao_DuasVezes_EhNoOp()
    {
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 100_00);
        app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Alimentacao, 10_00, null, "", Hoje);
        var id = app.TransacaoRepo.ListarTodas().Single().Id;

        app.Transacoes.ExcluirTransacao(id);
        var segunda = Record.Exception(() => app.Transacoes.ExcluirTransacao(id));

        Assert.Null(segunda);
        Assert.Empty(app.TransacaoRepo.ListarTodas());
    }

    [Fact]
    public void AtualizacaoComposta_FalhaNaSegundaEtapa_RollbackTotal()
    {
        // Padrão usado pelo diálogo de carteiras: duas gravações numa única transação
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 100_00);

        var excecao = Record.Exception(() =>
            app.Db.EmTransacao(() =>
            {
                app.CarteiraRepo.AtualizarSaldoInicial(Database.CarteiraBancoId, 200_00);
                // simula falha na segunda carteira: saldo negativo é recusado
                app.Carteiras.AtualizarSaldoInicial(Database.CarteiraDinheiroFisicoId, -1);
            }));

        Assert.NotNull(excecao);
        Assert.Equal(100_00, app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
    }

    // ── Concorrência ──────────────────────────────────────────────────────

    [Fact]
    public void Concorrencia_EscritorBloqueado_LancaAposBusyTimeout()
    {
        string dir = NovoDir();
        using var db1 = new Database(Path.Combine(dir, "conc.db"));
        using var db2 = new Database(Path.Combine(dir, "conc.db"));
        db1.Initialize();

        db1.EmTransacao(() =>
        {
            // escritor 1 segurando o lock; escritor 2 deve aguardar e falhar após o timeout
            var excecao = Record.Exception(() => db2.Initialize());
            Assert.IsType<SqliteException>(excecao);
        });

        // após o commit do escritor 1, o escritor 2 consegue gravar
        var depois = Record.Exception(() => db2.Initialize());
        Assert.Null(depois);
        TryDelete(dir);
    }

    // ── Volumes ───────────────────────────────────────────────────────────

    [Fact]
    public void Volume_10MilTransacoes_ConsultasRapidas()
    {
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 10_000_000_00L);
        app.Db.EmTransacao(() =>
        {
            for (int i = 0; i < 10_000; i++)
            {
                app.TransacaoRepo.Inserir(new Transacao(
                    Guid.NewGuid(), $"Compra {i}", $"comentário {i}", 10_00 + i,
                    i % 2 == 0 ? TipoTransacao.Saida : TipoTransacao.Entrada,
                    Hoje.AddDays(-(i % 300)), null,
                    (Categoria)(i % 11 + 1), Database.CarteiraBancoId));
            }
        });

        var relogio = System.Diagnostics.Stopwatch.StartNew();
        var todas = app.TransacaoRepo.ListarTodas();
        relogio.Stop();

        var relogioFts = System.Diagnostics.Stopwatch.StartNew();
        var filtradas = app.TransacaoRepo.ListarPorFiltro(new TransacaoFiltro("compra 99", null, null, null, null));
        relogioFts.Stop();

        Assert.Equal(10_000, todas.Count);
        Assert.True(relogio.ElapsedMilliseconds < 2000, $"ListarTodas levou {relogio.ElapsedMilliseconds}ms");
        Assert.True(relogioFts.ElapsedMilliseconds < 1000, $"Busca FTS levou {relogioFts.ElapsedMilliseconds}ms");
        Assert.NotEmpty(filtradas);
    }

    // ── Exportação ────────────────────────────────────────────────────────

    [Fact]
    public void Csv_ComentarioComQuebraDeLinha_Escapado()
    {
        var transacoes = new List<Transacao>
        {
            new(Guid.NewGuid(), "X", "linha1\nlinha2", 100, TipoTransacao.Saida, Hoje, null, Categoria.Outros)
        };

        string conteudo = Services.TransacaoCsvExporter.Formatar(transacoes);

        // comentário com \n deve ficar dentro de aspas, preservando a estrutura do CSV
        Assert.StartsWith("Data;Tipo;Descricao;Categoria;Comentario;Valor", conteudo);
        Assert.Contains("\"linha1\nlinha2\"", conteudo);
        Assert.EndsWith(Environment.NewLine, conteudo);
    }

    private static string NovoDir()
    {
        string dir = Path.Combine(Path.GetTempPath(), "sf-auditoria-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(dir);
        return dir;
    }

    private static void ExecutarSql(Database db, string sql)
    {
        using var comando = db.Connection.CreateCommand();
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
