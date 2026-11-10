using System.Windows;
using System.Windows.Threading;
using SmartFinance.Data;
using SmartFinance.Services;
using SmartFinance.Themes;

namespace SmartFinance;

public partial class App : Application
{
    public static Servicos? Servicos { get; private set; }

    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        DispatcherUnhandledException += (_, args) =>
        {
            MessageBox.Show(
                $"Ocorreu um erro inesperado:\n{args.Exception.Message}",
                "Smart Finance",
                MessageBoxButton.OK,
                MessageBoxImage.Error);
            args.Handled = true;
        };

        var db = new Database(Database.CaminhoPadrao());
        db.Initialize();

        var transacaoRepo = new TransacaoRepository(db);
        var metaRepo = new MetaRepository(db);
        var carteiraRepo = new CarteiraRepository(db);
        var carteiras = new CarteiraService(carteiraRepo, transacaoRepo);

        Servicos = new Servicos(
            db,
            new TransacaoService(db, transacaoRepo, metaRepo, carteiras),
            carteiras,
            new MetaService(metaRepo),
            new CartaoCreditoService(new CartaoCreditoRepository(db)),
            new EmprestimoService(new EmprestimoRepository(db)));

        TemaManager.Inicializar();

        var janela = new MainWindow();
        MainWindow = janela;
        janela.Show();
    }

    protected override void OnExit(ExitEventArgs e)
    {
        Servicos?.Db.Dispose();
        base.OnExit(e);
    }
}
