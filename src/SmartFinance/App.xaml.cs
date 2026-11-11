using System.Globalization;
using System.IO;
using SmartFinance.Views;
using System.Windows;
using System.Windows.Markup;
using System.Windows.Threading;
using SmartFinance.Data;
using SmartFinance.Services;
using SmartFinance.Themes;

namespace SmartFinance;

public partial class App : Application
{
    public static Servicos? Servicos { get; private set; }

    private const long LimiteLogBytes = 1_000_000;
    private static Mutex? _instanciaUnica;

    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        // F3: uma única instância do aplicativo (evita corrida no SQLite).
        _instanciaUnica = new Mutex(true, @"Local\SmartFinance.SingleInstance", out bool primeiraInstancia);
        if (!primeiraInstancia)
        {
            JanelaNativa.TrazerExistenteParaFrente();
            MessageBox.Show(
                "O Smart Finance já está em execução.",
                "Smart Finance",
                MessageBoxButton.OK,
                MessageBoxImage.Information);
            Shutdown(1);
            return;
        }

        GC.KeepAlive(_instanciaUnica);

        // WPF usa en-US como cultura padrão de elementos (datas do DatePicker, por exemplo);
        // o aplicativo é pt-BR, então fixa a cultura independentemente do Windows (SF-019).
        FrameworkElement.LanguageProperty.OverrideMetadata(
            typeof(FrameworkElement),
            new FrameworkPropertyMetadata(XmlLanguage.GetLanguage(Formatacao.CulturaBr.IetfLanguageTag)));

        DispatcherUnhandledException += (_, args) =>
        {
            RegistrarLog(args.Exception.ToString());

            MessageBox.Show(
                $"Ocorreu um erro inesperado:\n{args.Exception.Message}",
                "Smart Finance",
                MessageBoxButton.OK,
                MessageBoxImage.Error);
            args.Handled = true;
        };

        try
        {
            InicializarAplicativo();
        }
        catch (Exception ex)
        {
            // SF-009: sem isso, exceção no bootstrap deixa o processo vivo e invisível.
            RegistrarLog(ex.ToString());
            MessageBox.Show(
                $"Falha ao iniciar o Smart Finance:\n{ex.Message}\n\nBanco de dados: {Database.CaminhoPadrao()}",
                "Smart Finance",
                MessageBoxButton.OK,
                MessageBoxImage.Error);
            Shutdown(1);
        }
    }

    private void InicializarAplicativo()
    {
        var db = new Database(Database.CaminhoPadrao());
        db.Initialize();
        if (Environment.GetEnvironmentVariable("SMARTFINANCE_DB_PATH") is null)
        {
            // Em desenvolvimento/teste (banco isolado) não gera backup do usuário.
            ExecutarBackupDiario(db);
        }

        var transacaoRepo = new TransacaoRepository(db);
        var metaRepo = new MetaRepository(db);
        var carteiraRepo = new CarteiraRepository(db);
        var carteiras = new CarteiraService(carteiraRepo, transacaoRepo);

        Servicos = new Servicos(
            db,
            new TransacaoService(db, transacaoRepo, metaRepo, carteiras),
            carteiras,
            new MetaService(metaRepo),
            new CartaoCreditoService(db, new CartaoCreditoRepository(db), transacaoRepo),
            new EmprestimoService(db, new EmprestimoRepository(db), transacaoRepo));

        TemaManager.Inicializar();

        var janela = new MainWindow();
        MainWindow = janela;
        janela.Show();

        RegistrarLog("startup ok");
    }

    private static void ExecutarBackupDiario(Database db)
    {
        try
        {
            string pasta = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "SmartFinance", "backups");
            var backup = new BackupService(db.CaminhoBanco, pasta);
            string? criado = backup.ExecutarBackupDiario();
            if (criado is not null)
            {
                RegistrarLog($"backup diário criado: {criado}");
            }
        }
        catch (Exception ex)
        {
            RegistrarLog($"falha no backup diário (não bloqueia inicialização): {ex.Message}");
        }
    }

    internal static void RegistrarLog(string mensagem)
    {
        try
        {
            string pasta = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "SmartFinance");
            string log = Path.Combine(pasta, "erro.log");
            RotacionarSeNecessario(log);
            File.AppendAllText(log, $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] {mensagem}\n");
        }
        catch
        {
            // diagnóstico best-effort
        }
    }

    /// <summary>Mantém o log de erros pequeno: acima de ~1MB arquiva e conserva 2 gerações.</summary>
    internal static void RotacionarSeNecessario(string caminhoLog)
    {
        var info = new FileInfo(caminhoLog);
        if (!info.Exists || info.Length < LimiteLogBytes)
        {
            return;
        }

        string pasta = info.DirectoryName!;
        string baseNome = Path.GetFileNameWithoutExtension(caminhoLog);
        string arquivado = Path.Combine(pasta, $"{baseNome}-{DateTime.Now:yyyyMMdd-HHmmss}.log");
        File.Move(caminhoLog, arquivado);

        foreach (string antigo in Directory.GetFiles(pasta, $"{baseNome}-*.log")
                     .OrderByDescending(File.GetLastWriteTime)
                     .Skip(2))
        {
            File.Delete(antigo);
        }
    }

    protected override void OnExit(ExitEventArgs e)
    {
        Servicos?.Db.Dispose();
        base.OnExit(e);
    }
}
