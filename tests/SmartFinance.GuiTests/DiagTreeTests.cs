using FlaUI.Core.AutomationElements;
using FlaUI.Core.Definitions;
using FlaUI.Core.Tools;
using Xunit;
using Xunit.Abstractions;

namespace SmartFinance.GuiTests;

[Trait("Category", "GUI")]
public class DiagTreeTests : IClassFixture<GuiTests.AppFixture>, IDisposable
{
    private readonly ITestOutputHelper _saida;
    private readonly GuiTests.AppFixture _fx;

    public DiagTreeTests(GuiTests.AppFixture fx, ITestOutputHelper saida)
    {
        _fx = fx;
        _saida = saida;
    }

    [Fact]
    public void Diag_ArvoreTransacoes()
    {
        Navegar("NavTransacoes");

        var raiz = _fx.Janela.FindFirstDescendant(cf => cf.ByAutomationId("AreaConteudo"));
        _saida.WriteLine("AreaConteudo encontrado: " + (raiz is not null));

        var filhos = _fx.Janela.FindAllDescendants();
        _saida.WriteLine($"total descendentes: {filhos.Length}");
        foreach (var e in filhos.Take(60))
        {
            var id = e.Properties.AutomationId.ValueOrDefault;
            var nome = e.Properties.Name.ValueOrDefault;
            var tipo = e.ControlType;
            if (!string.IsNullOrEmpty(id) || tipo == ControlType.Text)
            {
                _saida.WriteLine($"[{tipo}] id='{id}' nome='{nome}'");
            }
        }
    }

    private void Navegar(string navId)
    {
        var botao = Retry.WhileNull(() => _fx.Janela.FindFirstDescendant(cf => cf.ByAutomationId(navId)),
            TimeSpan.FromSeconds(10), ignoreException: true).Result!;
        botao.Patterns.LegacyIAccessible.Pattern.DoDefaultAction();
        System.Threading.Thread.Sleep(800);
    }

    public void Dispose()
    {
        try
        {
            _fx.Janela.ModalWindows.FirstOrDefault()?.Close();
        }
        catch
        {
        }
    }
}
