using System.Diagnostics;
using System.IO;
using FlaUI.Core;
using FlaUI.Core.AutomationElements;
using FlaUI.Core.Conditions;
using FlaUI.Core.Input;
using FlaUI.Core.Tools;
using FlaUI.Core.WindowsAPI;
using FlaUI.UIA3;
using Microsoft.Data.Sqlite;
using Xunit;

namespace SmartFinance.GuiTests;

/// <summary>
/// Testes de interface com automação real (UIA/FlaUI). Lançam o aplicativo
/// com banco isolado em %TEMP% e exercitam navegação, diálogos, teclado,
/// persistência e tema. Executam em uma sessão interativa do Windows
/// (no CI são excluídos com o filtro Category!=GUI).
/// </summary>
[Trait("Category", "GUI")]
public class GuiTests : IClassFixture<GuiTests.AppFixture>, IDisposable
{
    private readonly AppFixture _fx;

    public GuiTests(AppFixture fx) => _fx = fx;

    public class AppFixture : IDisposable
    {
        public AppFixture()
        {
            DiretorioTemp = Path.Combine(Path.GetTempPath(), "sf-guitests-" + Guid.NewGuid().ToString("N"));
            Directory.CreateDirectory(DiretorioTemp);
            CaminhoBanco = Path.Combine(DiretorioTemp, "gui.db");
            CaminhoConfig = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "SmartFinance", "settings.json");

            string exe = Path.Combine(AppContext.BaseDirectory, "SmartFinance.exe");
            Assert.True(File.Exists(exe), $"exe não encontrado: {exe}");

            var psi = new ProcessStartInfo
            {
                FileName = exe,
                UseShellExecute = false
            };
            psi.EnvironmentVariables["SMARTFINANCE_DB_PATH"] = CaminhoBanco;

            Processo = Process.Start(psi)!;
            Automation = new UIA3Automation();

            Janela = Retry.WhileNull(
                    () => Application.Attach(Processo).GetMainWindow(Automation),
                    TimeSpan.FromSeconds(20)).Result
                ?? throw new InvalidOperationException("janela principal não apareceu");
            Janela.WaitUntilClickable(TimeSpan.FromSeconds(10));
        }

        public Process Processo { get; }
        public UIA3Automation Automation { get; }
        public FlaUI.Core.AutomationElements.Window Janela { get; }
        public string DiretorioTemp { get; }
        public string CaminhoBanco { get; }
        public string CaminhoConfig { get; }

        public FlaUI.Core.AutomationElements.Window? DialogoAtivo()
        {
            return Retry.WhileNull(() => Janela.ModalWindows.FirstOrDefault(),
                    TimeSpan.FromSeconds(5)).Result;
        }

        public int ContarTransacoes()
        {
            using var conn = new SqliteConnection($"Data Source={CaminhoBanco};Mode=ReadOnly;Pooling=false");
            conn.Open();
            using var comando = conn.CreateCommand();
            comando.CommandText = "SELECT COUNT(*) FROM transacao";
            return (int)(long)comando.ExecuteScalar()!;
        }

        public void Dispose()
        {
            try
            {
                Processo.Kill(entireProcessTree: true);
            }
            catch
            {
                // encerramento best-effort
            }

            Automation.Dispose();
            Processo.Dispose();

            try
            {
                Directory.Delete(DiretorioTemp, recursive: true);
            }
            catch (IOException)
            {
            }
        }
    }

    private FlaUI.Core.AutomationElements.AutomationElement? Buscar(string automationId, int segundos = 10)
    {
        return Retry.WhileNull(
                () => _fx.Janela.FindFirstDescendant(cf => cf.ByAutomationId(automationId)),
                TimeSpan.FromSeconds(segundos)).Result;
    }

    private FlaUI.Core.AutomationElements.AutomationElement? BuscarPorNome(string nome, int segundos = 10)
    {
        return Retry.WhileNull(
                () => _fx.Janela.FindFirstDescendant(cf => cf.ByName(nome)),
                TimeSpan.FromSeconds(segundos)).Result;
    }

    private FlaUI.Core.AutomationElements.Window AbrirDialogo(string botao)
    {
        BuscarPorNome(botao)!.AsButton().Invoke();
        var dialogo = _fx.DialogoAtivo()
            ?? throw new InvalidOperationException($"diálogo não abriu ao clicar em '{botao}'");
        return dialogo;
    }

    [Fact]
    public void JanelaPrincipal_Abre_ComDashboard()
    {
        Assert.Equal("Smart Finance", _fx.Janela.Title);
        Assert.NotNull(BuscarPorNome("Saldo disponível"));
        Assert.NotNull(BuscarPorNome("Transações recentes"));
        Assert.NotNull(BuscarPorNome("Metas"));
    }

    [Fact]
    public void Navegacao_AlcancarTodasAsVisoes()
    {
        (string navId, string marcador)[] visoes =
        {
            ("NavTransacoes", "Todas as transações"),
            ("NavMetas", "Metas"),
            ("NavCartoes", "Cartão de crédito"),
            ("NavEmprestimos", "Emprestei pra alguém"),
        };

        foreach (var (navId, marcador) in visoes)
        {
            Buscar(navId)!.Patterns.SelectionItem.Pattern.Select();
            Assert.NotNull(BuscarPorNome(marcador, 10));
        }

        Buscar("NavInicio")!.Patterns.SelectionItem.Pattern.Select();
        Assert.NotNull(BuscarPorNome("Saldo disponível"));
    }

    [Fact]
    public void NovaTransacao_ViaUi_PersisteNoBanco()
    {
        Buscar("NavTransacoes")!.Patterns.SelectionItem.Pattern.Select();
        int antes = _fx.ContarTransacoes();

        var dialogo = AbrirDialogo("Nova transação");

        dialogo.FindFirstDescendant(cf => cf.ByAutomationId("CampoValor"))
            .AsTextBox().Text = "42,50";
        dialogo.FindFirstDescendant(cf => cf.ByAutomationId("CampoComentario"))
            .AsTextBox().Text = "compra gui teste";

        var categoria = dialogo.FindFirstDescendant(cf => cf.ByAutomationId("ComboCategoria")).AsComboBox();
        categoria.Expand();
        categoria.Select(0); // Salário

        dialogo.FindFirstDescendant(cf => cf.ByAutomationId("BotaoSalvar")).AsButton().Invoke();

        Retry.WhileTrue(() => _fx.DialogoAtivo() is not null, TimeSpan.FromSeconds(5));

        Assert.Equal(antes + 1, _fx.ContarTransacoes());

        using var conn = new SqliteConnection($"Data Source={_fx.CaminhoBanco};Mode=ReadOnly;Pooling=false");
        conn.Open();
        using var comando = conn.CreateCommand();
        comando.CommandText = "SELECT COUNT(*) FROM transacao WHERE comentario = 'compra gui teste' AND valor_centavos = 4250";
        Assert.Equal(1L, (long)comando.ExecuteScalar()!);
    }

    [Fact]
    public void Dialogo_FechaComEscape()
    {
        var dialogo = AbrirDialogo("Nova transação");
        dialogo.Focus();
        Keyboard.Press(VirtualKeyShort.ESC);
        Keyboard.Release(VirtualKeyShort.ESC);

        Retry.WhileTrue(() => _fx.DialogoAtivo() is not null, TimeSpan.FromSeconds(5));
        Assert.Null(_fx.DialogoAtivo());
    }

    [Fact]
    public void CarteirasDialog_AbreEFechaComEscape()
    {
        Buscar("NavInicio")!.Patterns.SelectionItem.Pattern.Select();
        var dialogo = AbrirDialogo("Carteiras");

        dialogo.FindFirstDescendant(cf => cf.ByAutomationId("CampoBanco")).Focus();
        Keyboard.Press(VirtualKeyShort.ESC);
        Keyboard.Release(VirtualKeyShort.ESC);

        Retry.WhileTrue(() => _fx.DialogoAtivo() is not null, TimeSpan.FromSeconds(5));
        Assert.Null(_fx.DialogoAtivo());
    }

    [Fact]
    public void AlternarTema_AlteraPersistenciaEVolta()
    {
        string original = File.Exists(_fx.CaminhoConfig) ? File.ReadAllText(_fx.CaminhoConfig) : "";

        try
        {
            var botaoTema = Buscar("BotaoTema")!.AsButton();

            botaoTema.Invoke();
            string depoisPrimeiro = LerTema();
            Assert.NotEqual("sistema", depoisPrimeiro);

            botaoTema.Invoke();
            string depoisSegundo = LerTema();
            Assert.NotEqual(depoisPrimeiro, depoisSegundo);
        }
        finally
        {
            if (File.Exists(_fx.CaminhoConfig))
            {
                File.WriteAllText(_fx.CaminhoConfig, original);
            }
        }
    }

    private string LerTema()
    {
        return File.Exists(_fx.CaminhoConfig)
            ? File.ReadAllText(_fx.CaminhoConfig)
            : "";
    }

    public void Dispose()
    {
        // a fixture cuida do encerramento; garantia extra: fechar diálogos abertos
        try
        {
            _fx.DialogoAtivo()?.Close();
        }
        catch
        {
        }
    }
}
