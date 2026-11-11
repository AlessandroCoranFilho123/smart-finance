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

            // Não usar GetMainWindow: o host .NET cria uma janela oculta com o nome do
            // exe, que pode ser retornada antes da janela real (falhou no CI). Espera
            // por uma janela de topo com o título do aplicativo; leitura de título é
            // defensiva porque a janela oculta do host não suporta a propriedade Name.
            Janela = Retry.WhileNull(
                    () => Application.Attach(Processo)
                        .GetAllTopLevelWindows(Automation)
                        .FirstOrDefault(w => string.Equals(
                            w.Properties.Name.ValueOrDefault, "Smart Finance", StringComparison.Ordinal)),
                    TimeSpan.FromSeconds(40),
                    ignoreException: true).Result
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

    /// <summary>Busca elemento no diálogo com retry: o conteúdo pode ainda estar
    /// sendo materializado na árvore UIA no instante em que a janela aparece.</summary>
    private FlaUI.Core.AutomationElements.AutomationElement NoDialogo(
        FlaUI.Core.AutomationElements.Window dialogo, string automationId)
    {
        return Retry.WhileNull(
                () => dialogo.FindFirstDescendant(cf => cf.ByAutomationId(automationId)),
                TimeSpan.FromSeconds(10),
                ignoreException: true).Result
            ?? throw new InvalidOperationException($"'{automationId}' não encontrado no diálogo");
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
            ClicarNav(navId);
            Assert.True(BuscarPorNome(marcador, 10) is not null,
                $"marcador '{marcador}' não apareceu após clicar {navId}");
        }

        ClicarNav("NavInicio");
        Assert.NotNull(BuscarPorNome("Saldo disponível"));
    }

    /// <summary>Ativa um item da navegação. Cliques por coordenada falham com escala
    /// de DPI e RadioButtons do WPF não expõem Invoke; o default action do
    /// LegacyIAccessible é o equivalente programático ao clique e funciona sempre.</summary>
    private void ClicarNav(string navId)
    {
        var botao = Buscar(navId) ?? throw new InvalidOperationException($"{navId} não encontrado");

        if (botao.Patterns.LegacyIAccessible.IsSupported)
        {
            botao.Patterns.LegacyIAccessible.Pattern.DoDefaultAction();
        }
        else
        {
            botao.Click();
        }
    }

    [Fact]
    public void NovaTransacao_ViaUi_PersisteNoBanco()
    {
        ClicarNav("NavTransacoes");
        int antes = _fx.ContarTransacoes();

        var dialogo = AbrirDialogo("+ Nova transação");

        NoDialogo(dialogo, "CampoValor").AsTextBox().Text = "42,50";
        NoDialogo(dialogo, "CampoComentario").AsTextBox().Text = "compra gui teste";

        var categoria = NoDialogo(dialogo, "ComboCategoria").AsComboBox();
        categoria.Expand();
        categoria.Select(0); // Salário

        NoDialogo(dialogo, "BotaoSalvar").AsButton().Invoke();

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
        var dialogo = AbrirDialogo("+ Nova transação");
        dialogo.Focus();
        Keyboard.Press(VirtualKeyShort.ESC);
        Keyboard.Release(VirtualKeyShort.ESC);

        Retry.WhileTrue(() => _fx.DialogoAtivo() is not null, TimeSpan.FromSeconds(5));
        Assert.Null(_fx.DialogoAtivo());
    }

    [Fact]
    public void CarteirasDialog_AbreEFechaComEscape()
    {
        Buscar("NavInicio")!.Click();
        var dialogo = AbrirDialogo("Carteiras");

        NoDialogo(dialogo, "CampoBanco").Focus();
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
