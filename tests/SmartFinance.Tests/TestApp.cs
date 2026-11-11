using SmartFinance.Data;
using SmartFinance.Services;

namespace SmartFinance.Tests;

/// <summary>Aplicação de teste com banco SQLite isolado em pasta temporária.</summary>
internal sealed class TestApp : IDisposable
{
    private readonly string _dir;

    public Database Db { get; }
    public TransacaoRepository TransacaoRepo { get; }
    public MetaRepository MetaRepo { get; }
    public CarteiraRepository CarteiraRepo { get; }
    public CartaoCreditoRepository CartaoRepo { get; }
    public EmprestimoRepository EmprestimoRepo { get; }
    public CarteiraService Carteiras { get; }
    public TransacaoService Transacoes { get; }
    public MetaService Metas { get; }
    public CartaoCreditoService Cartao { get; }
    public EmprestimoService Emprestimos { get; }

    public TestApp()
    {
        _dir = Path.Combine(Path.GetTempPath(), "sf-tests-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(_dir);

        Db = new Database(Path.Combine(_dir, "test.db"));
        Db.Initialize();

        TransacaoRepo = new TransacaoRepository(Db);
        MetaRepo = new MetaRepository(Db);
        CarteiraRepo = new CarteiraRepository(Db);
        CartaoRepo = new CartaoCreditoRepository(Db);
        EmprestimoRepo = new EmprestimoRepository(Db);

        Carteiras = new CarteiraService(CarteiraRepo, TransacaoRepo);
        Transacoes = new TransacaoService(Db, TransacaoRepo, MetaRepo, Carteiras);
        Metas = new MetaService(MetaRepo);
        Cartao = new CartaoCreditoService(Db, CartaoRepo, TransacaoRepo);
        Emprestimos = new EmprestimoService(Db, EmprestimoRepo, TransacaoRepo);
    }

    public void Dispose()
    {
        Db.Dispose();
        try
        {
            Directory.Delete(_dir, recursive: true);
        }
        catch (IOException)
        {
            // limpeza de teste é best-effort
        }
    }
}
