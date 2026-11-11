using System.Diagnostics;
using System.Drawing.Imaging;
using System.IO;
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
/// Captura telas reais do aplicativo para verificação visual (SF-010/017/025)
/// e para o README. Executa com banco isolado populado com dados de exemplo.
/// </summary>
[Trait("Category", "Captura")]
public class CapturaTests : IDisposable
{
    private readonly ITestOutputHelper _saida;
    private readonly string _dirTemp;
    private readonly string _caminhoBanco;
    private readonly string _pastaCapturas;
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
    public void CapturarTelasDoAplicativo()
    {
        // tema claro
        CapturarTelaAtual("01-dashboard-claro");

        Navegar("NavTransacoes");
        CapturarTelaAtual("02-transacoes-claro");

        Navegar("NavMetas");
        CapturarTelaAtual("03-metas-claro");

        Navegar("NavCartoes");
        CapturarTelaAtual("04-cartao-claro");

        Navegar("NavEmprestimos");
        CapturarTelaAtual("05-emprestimos-claro");

        // diálogos: SF-002 combos legíveis, SF-017 data visível, SF-010 ícones
        Navegar("NavInicio");
        var dialogoTransacao = AbrirDialogo("+ Nova transação");
        CapturarJanela(dialogoTransacao, "06-dialogo-nova-transacao");
        FecharDialogo(dialogoTransacao);

        var dialogoTransferencia = AbrirDialogo("Transferir");
        CapturarJanela(dialogoTransferencia, "07-dialogo-transferencia");
        FecharDialogo(dialogoTransferencia);

        // tema escuro (SF-025 contraste)
        var tema = Buscar("BotaoTema")!;
        tema.Patterns.Invoke.Pattern.Invoke();
        Thread.Sleep(600);
        CapturarTelaAtual("08-dashboard-escuro");
        Navegar("NavTransacoes");
        CapturarTelaAtual("09-transacoes-escuro");
        tema.Patterns.Invoke.Pattern.Invoke();
        Thread.Sleep(400);

        _saida.WriteLine($"Capturas salvas em: {_pastaCapturas}");
    }

    [Fact]
    public void CapturarCenarioDeBancoCorrompido_ESF009_Encerra()
    {
        // SF-009: falha no bootstrap → mensagem + processo encerra
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

        CapturarJanela(caixa, "10-erro-banco-corrompido");

        caixa.FindFirstDescendant(cf => cf.ByName("OK"))!.AsButton().Invoke();
        bool encerrou = processo.WaitForExit(10_000);
        Assert.True(encerrou, "processo deve encerrar após o usuário fechar a mensagem (SF-009)");
        _saida.WriteLine("SF-009 verificado: mensagem exibida e processo encerrou.");
    }

    // ── apoio ─────────────────────────────────────────────────────────────

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

        metaRepo.Inserir(new Meta(Guid.NewGuid(), "Notebook novo", 5000_00, 2100_00));
        metaRepo.Inserir(new Meta(Guid.NewGuid(), "Viagem de férias", 3000_00, 2750_00));

        cartaoRepo.Inserir(new CompraCartaoCredito(Guid.NewGuid(), "Notebook", 4800_00, 12, hoje.AddDays(3)));
        cartaoRepo.Inserir(new CompraCartaoCredito(Guid.NewGuid(), "Curso", 600_00, 6, hoje.AddDays(20)));

        emprestimoRepo.Inserir(new Emprestimo(Guid.NewGuid(), TipoEmprestimo.Emprestei, "Ana", 200_00, hoje.AddDays(10)));
        emprestimoRepo.Inserir(new Emprestimo(Guid.NewGuid(), TipoEmprestimo.PediEmprestado, "Carlos", 150_00, hoje.AddDays(-3)));
    }

    private void Navegar(string navId)
    {
        var botao = Retry.WhileNull(() => _janela.FindFirstDescendant(cf => cf.ByAutomationId(navId)),
            TimeSpan.FromSeconds(10), ignoreException: true).Result!;
        botao.Patterns.LegacyIAccessible.Pattern.DoDefaultAction();
        Thread.Sleep(600);
        _janela.SetForeground();
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
        var retangulo = janela.BoundingRectangle;
        using var bitmap = new System.Drawing.Bitmap((int)retangulo.Width, (int)retangulo.Height);
        using (var g = System.Drawing.Graphics.FromImage(bitmap))
        {
            g.CopyFromScreen((int)retangulo.X, (int)retangulo.Y, 0, 0,
                new System.Drawing.Size((int)retangulo.Width, (int)retangulo.Height));
        }

        bitmap.Save(Path.Combine(_pastaCapturas, nomeArquivo + ".png"), ImageFormat.Png);
        _saida.WriteLine($"captura: {nomeArquivo}.png ({(int)retangulo.Width}x{(int)retangulo.Height})");
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
            Directory.Delete(_dirTemp, recursive: true);
        }
        catch (IOException)
        {
        }
    }
}
