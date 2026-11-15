using System.Diagnostics;
using System.Drawing.Imaging;
using System.IO;
using System.Linq;
using FlaUI.Core;
using FlaUI.Core.AutomationElements;
using FlaUI.Core.Tools;
using FlaUI.UIA3;
using Microsoft.Data.Sqlite;
using SmartFinance.Data;
using SmartFinance.Models;
using Xunit;
using Xunit.Abstractions;

namespace SmartFinance.GuiTests;

/// <summary>
/// Captura TODAS as telas e diálogos nos temas claro e escuro, com banco
/// populado cobrindo os estados visuais (metas em três faixas de progresso,
/// compras vencida/próxima/distante, empréstimos nos dois tipos) — base para
/// verificação de cores, contraste e uniformidade dos cartões.
/// </summary>
[Trait("Category", "Captura")]
public class CapturaTests : IDisposable
{
    private readonly ITestOutputHelper _saida;
    private readonly string _dirTemp;
    private readonly string _caminhoBanco;
    private readonly string _pastaCapturas;
    private readonly string _caminhoConfig;
    private readonly string _configOriginal;
    private readonly Process _processo;
    private readonly UIA3Automation _automation;
    private readonly Window _janela;

    public CapturaTests(ITestOutputHelper saida)
    {
        _saida = saida;
        _dirTemp = Path.Combine(Path.GetTempPath(), "sf-captura-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(_dirTemp);
        _caminhoBanco = Path.Combine(_dirTemp, "captura.db");
        _pastaCapturas = Path.GetFullPath(Path.Combine(
            AppContext.BaseDirectory, "..", "..", "..", "..", "..", "docs", "auditoria", "imagens"));
        Directory.CreateDirectory(_pastaCapturas);
        _caminhoConfig = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "SmartFinance", "settings.json");
        _configOriginal = File.Exists(_caminhoConfig) ? File.ReadAllText(_caminhoConfig) : "";

        PopularBancoDeExemplo(_caminhoBanco);

        var psi = new ProcessStartInfo
        {
            FileName = Path.Combine(AppContext.BaseDirectory, "SmartFinance.exe"),
            UseShellExecute = false
        };
        psi.EnvironmentVariables["SMARTFINANCE_DB_PATH"] = _caminhoBanco;
        _processo = Process.Start(psi)!;
        _automation = new UIA3Automation();

        _janela = Retry.WhileNull(() => Application.Attach(_processo)
                .GetAllTopLevelWindows(_automation)
                .FirstOrDefault(w => w.Properties.Name.ValueOrDefault == "Smart Finance"),
            TimeSpan.FromSeconds(30), ignoreException: true).Result
            ?? throw new InvalidOperationException("janela principal não apareceu");
        _janela.SetForeground();
        Thread.Sleep(800);
    }

    [Fact]
    public void CapturarTodasAsTelasNosDoisTemas()
    {
        AplicarTema("escuro");
        CapturarTodasAsTelas("escuro");

        AplicarTema("claro");
        CapturarTodasAsTelas("claro");

        // restaura a preferência original do usuário
        File.WriteAllText(_caminhoConfig, _configOriginal);
    }

    [Fact]
    public void CapturarDialogosNosDoisTemas()
    {
        AplicarTema("escuro");
        CapturarDialogos("escuro");
        CapturarDialogoEdicao("escuro");

        AplicarTema("claro");
        CapturarDialogos("claro");
        CapturarDialogoEdicao("claro");

        File.WriteAllText(_caminhoConfig, _configOriginal);
    }

    [Fact]
    public void CapturarCalendarioNosDoisTemas()
    {
        AplicarTema("escuro");
        CapturarCalendario("escuro");

        AplicarTema("claro");
        CapturarCalendario("claro");

        // restaura a preferência original do usuário
        File.WriteAllText(_caminhoConfig, _configOriginal);
    }

    [Fact]
    public void CapturarCenarioDeBancoCorrompido_ESF009_Encerra()
    {
        string corrompido = Path.Combine(_dirTemp, "corrompido.db");
        File.WriteAllBytes(corrompido, new byte[] { 0xDE, 0xAD, 0xBE, 0xEF, 0x00, 0x01 });

        var psi = new ProcessStartInfo
        {
            FileName = Path.Combine(AppContext.BaseDirectory, "SmartFinance.exe"),
            UseShellExecute = false
        };
        psi.EnvironmentVariables["SMARTFINANCE_DB_PATH"] = corrompido;
        var processo = Process.Start(psi)!;
        var app = Application.Attach(processo);
        var automacao = new UIA3Automation();

        var caixa = Retry.WhileNull(() => app.GetAllTopLevelWindows(automacao)
                .FirstOrDefault(w => w.Properties.Name.ValueOrDefault == "Smart Finance"),
            TimeSpan.FromSeconds(20), ignoreException: true).Result
            ?? throw new InvalidOperationException("mensagem de erro não apareceu");

        CapturarJanela(caixa, "20-erro-banco-corrompido");

        caixa.FindFirstDescendant(cf => cf.ByName("OK"))!.AsButton().Invoke();
        bool encerrou = processo.WaitForExit(10_000);
        Assert.True(encerrou, "processo deve encerrar após o usuário fechar a mensagem (SF-009)");
        _saida.WriteLine("SF-009 verificado: mensagem exibida e processo encerrou.");
    }

    // ── apoio ─────────────────────────────────────────────────────────────

    /// <summary>
    /// Regressão de contraste (o calendário chegou a ficar transparente/ilegível
    /// no tema escuro): amostra pixels da captura recém-gerada e exige as cores
    /// da paleta no cartão do popup, na célula do dia de hoje e no campo de data.
    /// </summary>
    private void VerificarCoresDoCalendario(string sufixo, AutomationElement popup, AutomationElement seletor)
    {
        bool escuro = sufixo == "escuro";
        var corCard = escuro ? System.Drawing.Color.FromArgb(0x17, 0x1D, 0x2B) : System.Drawing.Color.FromArgb(0xFF, 0xFF, 0xFF);
        var corHoje = escuro ? System.Drawing.Color.FromArgb(0x24, 0x30, 0x4F) : System.Drawing.Color.FromArgb(0xE6, 0xEB, 0xFF);
        var corCampo = escuro ? System.Drawing.Color.FromArgb(0x1C, 0x24, 0x34) : System.Drawing.Color.FromArgb(0xFF, 0xFF, 0xFF);

        // Capture() do popup devolve a região da janela principal: capturar a
        // própria janela garante uma única referência de coordenadas
        using var captura = _janela.Capture();
        var retJanela = _janela.BoundingRectangle;

        // margem interna do cartão do popup (padding entre a borda e a grade)
        var calendario = popup.FindFirstDescendant(cf => cf.ByControlType(FlaUI.Core.Definitions.ControlType.Calendar))
            ?? throw new InvalidOperationException("calendário não encontrado no popup");
        var retCalendario = calendario.BoundingRectangle;
        Assert.Equal(corCard, PixelDoBitmap(captura, retJanela,
            retCalendario.Left - 4, retCalendario.Top + retCalendario.Height / 2));

        // célula do dia de hoje: fundo BrushAccentSoft (amostra fora do número)
        var hoje = DateTime.Today.Day.ToString();
        var celulaHoje = popup.FindFirstDescendant(cf => cf.ByName(hoje))
            ?? throw new InvalidOperationException($"célula do dia {hoje} não encontrada no calendário");
        var retHoje = celulaHoje.BoundingRectangle;
        Assert.Equal(corHoje, PixelDoBitmap(captura, retJanela,
            retHoje.Left + retHoje.Width * 0.12, retHoje.Top + retHoje.Height / 2));

        // campo de data na janela principal: fundo BrushFieldBg (lado direito, sem texto)
        var botaoCampo = seletor.FindFirstDescendant(cf => cf.ByControlType(FlaUI.Core.Definitions.ControlType.Button))
            ?? throw new InvalidOperationException("botão de data não encontrado");
        var retCampo = botaoCampo.BoundingRectangle;
        Assert.Equal(corCampo, PixelDoBitmap(captura, retJanela,
            retCampo.Right - 12, retCampo.Top + retCampo.Height / 2));
    }

    /// <summary>Amostra um pixel de tela na captura da janela, convertendo
    /// coordenadas (com escala de DPI entre o retângulo UIA e o bitmap).</summary>
    private static System.Drawing.Color PixelDoBitmap(
        System.Drawing.Bitmap bitmap, System.Drawing.Rectangle janela, double xTela, double yTela)
    {
        double escala = bitmap.Width / janela.Width;
        int x = (int)((xTela - janela.Left) * escala);
        int y = (int)((yTela - janela.Top) * escala);
        return bitmap.GetPixel(
            Math.Min(Math.Max(x, 0), bitmap.Width - 1),
            Math.Min(Math.Max(y, 0), bitmap.Height - 1));
    }

    /// <summary>Abre o calendário do filtro de transações e fotografa o popup
    /// (janela de topo separada do processo, como em DataTransacoesTests).</summary>
    private void CapturarCalendario(string sufixo)
    {
        // recria a view (clicar no próprio RadioButton já marcado não dispara
        // AoNavegar): garante popup recém-instanciado em cada tema
        Navegar("NavInicio");
        Navegar("NavTransacoes");
        var seletor = Buscar("DataInicio")
            ?? throw new InvalidOperationException("seletor de data não encontrado");
        seletor.FindFirstDescendant(cf => cf.ByControlType(FlaUI.Core.Definitions.ControlType.Button))!
            .AsButton().Invoke();

        var app = FlaUI.Core.Application.Attach(_processo);
        var popup = Retry.WhileNull(() => app.GetAllTopLevelWindows(_automation)
                .FirstOrDefault(w => !w.Properties.IsOffscreen.ValueOrDefault
                    && w.FindAllDescendants(
                        cf => cf.ByControlType(FlaUI.Core.Definitions.ControlType.Calendar)).Length > 0),
            TimeSpan.FromSeconds(5), ignoreException: true).Result
            ?? throw new InvalidOperationException("popup do calendário não abriu");
        Thread.Sleep(400);
        CapturarJanela(popup, $"10-calendario-{sufixo}");
        VerificarCoresDoCalendario(sufixo, popup, seletor);

        // visão de meses: clicar no cabeçalho ("novembro de 2026") alterna o modo
        var cabecalho = popup.FindFirstDescendant(cf => cf.ByName("novembro de 2026"));
        if (cabecalho is not null)
        {
            cabecalho.AsButton().Invoke();
            var janeiro = Retry.WhileNull(() => popup.FindFirstDescendant(cf => cf.ByName("jan")),
                TimeSpan.FromSeconds(5), ignoreException: true).Result;
            if (janeiro is null)
            {
                var nomes = popup.FindAllDescendants(cf => cf.ByControlType(FlaUI.Core.Definitions.ControlType.Button))
                    .Select(b => b.Properties.Name.ValueOrDefault)
                    .Where(n => !string.IsNullOrEmpty(n));
                throw new InvalidOperationException(
                    $"visão de meses não abriu ao clicar no cabeçalho; botões visíveis: {string.Join(", ", nomes)}");
            }

            CapturarJanela(popup, $"11-calendario-meses-{sufixo}");
        }

        FlaUI.Core.Input.Keyboard.Press(FlaUI.Core.WindowsAPI.VirtualKeyShort.ESC);
        FlaUI.Core.Input.Keyboard.Release(FlaUI.Core.WindowsAPI.VirtualKeyShort.ESC);
        Thread.Sleep(300);
        FlaUI.Core.Input.Keyboard.Press(FlaUI.Core.WindowsAPI.VirtualKeyShort.ESC);
        FlaUI.Core.Input.Keyboard.Release(FlaUI.Core.WindowsAPI.VirtualKeyShort.ESC);
        Thread.Sleep(300);
    }

    private void CapturarTodasAsTelas(string sufixo)
    {
        CapturarTelaAtual($"01-dashboard-{sufixo}");
        Navegar("NavTransacoes");
        CapturarTelaAtual($"02-transacoes-{sufixo}");
        Navegar("NavMetas");
        CapturarTelaAtual($"03-metas-{sufixo}");
        Navegar("NavCartoes");
        CapturarTelaAtual($"04-cartao-{sufixo}");
        Navegar("NavEmprestimos");
        CapturarTelaAtual($"05-emprestimos-{sufixo}");
        Navegar("NavInicio");
    }

    private void CapturarDialogos(string sufixo)
    {
        Navegar("NavTransacoes");
        var transacao = AbrirDialogo("+ Nova transação");
        CapturarJanela(transacao, $"06-dialogo-nova-transacao-{sufixo}");
        FecharDialogo(transacao);

        Navegar("NavInicio");
        var transferencia = AbrirDialogo("Transferir");
        CapturarJanela(transferencia, $"07-dialogo-transferencia-{sufixo}");
        FecharDialogo(transferencia);

        Navegar("NavMetas");
        var meta = AbrirDialogo("+ Nova meta");
        CapturarJanela(meta, $"08-dialogo-nova-meta-{sufixo}");
        FecharDialogo(meta);

        Navegar("NavInicio");
        var carteiras = AbrirDialogo("Carteiras");
        CapturarJanela(carteiras, $"09-dialogo-carteiras-{sufixo}");
        FecharDialogo(carteiras);
    }

    /// <summary>Diálogo em modo EDIÇÃO (duplo clique na lista), onde o botão
    /// Excluir fica visível — estado não coberto pelas capturas de criação.</summary>
    private void CapturarDialogoEdicao(string sufixo)
    {
        Navegar("NavTransacoes");
        var primeiraLinha = Retry.WhileNull(
                () => _janela.FindFirstDescendant(cf => cf.ByAutomationId("ListaTransacoes"))
                    .FindAllDescendants(cf => cf.ByControlType(FlaUI.Core.Definitions.ControlType.ListItem))
                    .FirstOrDefault(),
            TimeSpan.FromSeconds(5), ignoreException: true).Result
            ?? throw new InvalidOperationException("nenhuma transação na lista para editar");

        // duplo clique abre a edição (MouseDoubleClick da lista)
        var retangulo = primeiraLinha.BoundingRectangle;
        FlaUI.Core.Input.Mouse.MoveTo((int)(retangulo.X + retangulo.Width / 2), (int)(retangulo.Y + retangulo.Height / 2));
        FlaUI.Core.Input.Mouse.DoubleClick(FlaUI.Core.Input.MouseButton.Left);

        var dialogo = Retry.WhileNull(() => _janela.ModalWindows.FirstOrDefault(),
            TimeSpan.FromSeconds(5), ignoreException: true).Result
            ?? throw new InvalidOperationException("diálogo de edição não abriu");
        Thread.Sleep(400);
        CapturarJanela(dialogo, $"12-dialogo-editar-transacao-{sufixo}");

        dialogo.FindFirstDescendant(cf => cf.ByName("Cancelar"))?.AsButton().Invoke();
        Thread.Sleep(400);
    }

    /// <summary>Alterna o tema pelo próprio app até alcançar o desejado
    /// (lê o settings.json que o aplicativo grava).</summary>
    private void AplicarTema(string desejado)
    {
        if (LerTema() == desejado)
        {
            return;
        }

        Buscar("BotaoTema")!.Patterns.Invoke.Pattern.Invoke();
        Thread.Sleep(600);

        if (LerTema() != desejado)
        {
            Buscar("BotaoTema")!.Patterns.Invoke.Pattern.Invoke();
            Thread.Sleep(600);
        }

        Assert.Equal(desejado, LerTema());
        _janela.SetForeground();
        Thread.Sleep(400);
    }

    private string LerTema()
    {
        if (!File.Exists(_caminhoConfig))
        {
            return "sistema";
        }

        var texto = File.ReadAllText(_caminhoConfig);
        return texto.Contains("\"escuro\"") ? "escuro"
            : texto.Contains("\"claro\"") ? "claro"
            : "sistema";
    }

    private void PopularBancoDeExemplo(string caminho)
    {
        using var db = new Database(caminho);
        db.Initialize();
        var repo = new TransacaoRepository(db);
        var metaRepo = new MetaRepository(db);
        var carteiraRepo = new CarteiraRepository(db);
        var cartaoRepo = new CartaoCreditoRepository(db);
        var emprestimoRepo = new EmprestimoRepository(db);
        var hoje = DateOnly.FromDateTime(DateTime.Today);

        carteiraRepo.AtualizarSaldoInicial(Database.CarteiraBancoId, 3500_00);
        carteiraRepo.AtualizarSaldoInicial(Database.CarteiraDinheiroFisicoId, 180_00);
        repo.Inserir(new Transacao(Guid.NewGuid(), "Salário", "empresa", 4200_00,
            TipoTransacao.Entrada, hoje.AddDays(-5), null, Categoria.Salario, Database.CarteiraBancoId));
        repo.Inserir(new Transacao(Guid.NewGuid(), "Aluguel", "", 1200_00,
            TipoTransacao.Saida, hoje.AddDays(-4), null, Categoria.Aluguel, Database.CarteiraBancoId));
        repo.Inserir(new Transacao(Guid.NewGuid(), "Supermercado", "compras do mês", 450_00,
            TipoTransacao.Saida, hoje.AddDays(-2), null, Categoria.Alimentacao, Database.CarteiraBancoId));
        repo.Inserir(new Transacao(Guid.NewGuid(), "Freela", "", 800_00,
            TipoTransacao.Entrada, hoje.AddDays(-1), null, Categoria.Outros, Database.CarteiraDinheiroFisicoId));

        // metas nas três faixas de cor da barra de progresso
        metaRepo.Inserir(new Meta(Guid.NewGuid(), "Notebook novo", 5000_00, 2100_00));   // 42% — vermelho
        metaRepo.Inserir(new Meta(Guid.NewGuid(), "Viagem de férias", 3000_00, 2750_00)); // 92% — verde
        metaRepo.Inserir(new Meta(Guid.NewGuid(), "Reserva de emergência", 8000_00, 4800_00)); // 60% — laranja

        // compras: vencida (vermelho), próxima (laranja), distante (verde)
        cartaoRepo.Inserir(new CompraCartaoCredito(Guid.NewGuid(), "Notebook", 4800_00, 12, hoje.AddDays(3), 4));
        cartaoRepo.Inserir(new CompraCartaoCredito(Guid.NewGuid(), "Curso", 600_00, 6, hoje.AddDays(20)));
        cartaoRepo.Inserir(new CompraCartaoCredito(Guid.NewGuid(), "Fone", 250_00, 2, hoje.AddDays(-2), 1));

        emprestimoRepo.Inserir(new Emprestimo(Guid.NewGuid(), TipoEmprestimo.Emprestei, "Ana", 200_00, hoje.AddDays(10)));
        emprestimoRepo.Inserir(new Emprestimo(Guid.NewGuid(), TipoEmprestimo.PediEmprestado, "Carlos", 150_00, hoje.AddDays(-3)));
    }

    private void Navegar(string navId)
    {
        var botao = Retry.WhileNull(() => _janela.FindFirstDescendant(cf => cf.ByAutomationId(navId)),
            TimeSpan.FromSeconds(10), ignoreException: true).Result!;

        for (int tentativa = 1; tentativa <= 3; tentativa++)
        {
            // clique real com a janela em primeiro plano (método mais confiável
            // para RadioButton no runner de CI)
            _janela.SetForeground();
            Thread.Sleep(200);
            botao.Click();
            Thread.Sleep(700);

            // navegação confirmada quando a tela alvo aparece
            var alvo = navId switch
            {
                "NavTransacoes" => "Todas as transações",
                "NavMetas" => "Metas",
                "NavCartoes" => "Cartão de crédito",
                "NavEmprestimos" => "Emprestei pra alguém",
                _ => "Transações recentes"
            };

            if (_janela.FindFirstDescendant(cf => cf.ByName(alvo)) is not null)
            {
                return;
            }
        }

        throw new InvalidOperationException("navegação para " + navId + " não aplicou");
    }

    private Window AbrirDialogo(string botao)
    {
        BuscarPorNome(botao)!.AsButton().Invoke();
        var dialogo = Retry.WhileNull(() => _janela.ModalWindows.FirstOrDefault(),
            TimeSpan.FromSeconds(5), ignoreException: true).Result!;
        Thread.Sleep(400);
        return dialogo;
    }

    private void FecharDialogo(Window dialogo)
    {
        dialogo.FindFirstDescendant(cf => cf.ByName("Cancelar"))?.AsButton().Invoke();
        Thread.Sleep(400);
    }

    private AutomationElement? Buscar(string automationId)
    {
        return Retry.WhileNull(() => _janela.FindFirstDescendant(cf => cf.ByAutomationId(automationId)),
            TimeSpan.FromSeconds(10), ignoreException: true).Result;
    }

    private AutomationElement? BuscarPorNome(string nome)
    {
        return Retry.WhileNull(() => _janela.FindFirstDescendant(cf => cf.ByName(nome)),
            TimeSpan.FromSeconds(10), ignoreException: true).Result;
    }

    private void CapturarTelaAtual(string nomeArquivo) => CapturarJanela(_janela, nomeArquivo);

    private void CapturarJanela(Window janela, string nomeArquivo)
    {
        janela.SetForeground();
        Thread.Sleep(350);
        // PrintWindow (via Capture): fotografa o conteúdo da janela mesmo com
        // outras janelas sobrepostas — imune ao uso simultâneo da máquina.
        using var bitmap = janela.Capture();
        bitmap.Save(Path.Combine(_pastaCapturas, nomeArquivo + ".png"), ImageFormat.Png);
        _saida.WriteLine($"captura: {nomeArquivo}.png ({bitmap.Width}x{bitmap.Height})");
    }

    public void Dispose()
    {
        try
        {
            _processo.Kill(entireProcessTree: true);
        }
        catch
        {
        }

        _automation.Dispose();
        _processo.Dispose();
        try
        {
            File.WriteAllText(_caminhoConfig, _configOriginal);
            Directory.Delete(_dirTemp, recursive: true);
        }
        catch (IOException)
        {
        }
    }
}
