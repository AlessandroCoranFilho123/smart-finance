using FlaUI.Core.AutomationElements;
using FlaUI.Core.Definitions;
using FlaUI.Core.Tools;
using Xunit;

namespace SmartFinance.GuiTests;

/// <summary>
/// Regressão estrutural de layout (medições de retângulo UIA, independentes de
/// tema): os campos da linha de filtros da tela de transações devem ter a
/// mesma altura e alinhamento — o campo Buscar chegou a ficar ~48px contra
/// ~30px dos demais (padding aplicado duas vezes no template de TextBox) —,
/// e os botões do diálogo de edição (Excluir, Cancelar e Salvar) devem
/// compartilhar a mesma linha.
/// </summary>
[Trait("Category", "GUI")]
public class AlinhamentoTests : IClassFixture<GuiTests.AppFixture>, IDisposable
{
    /// <summary>Tolerância entre controles diferentes: a renderização interna
    /// de TextBox/ComboBox difere em 1-2px; regressões reais são de 10px+.</summary>
    private const double Tolerancia = 2.0;

    private readonly GuiTests.AppFixture _fx;

    public AlinhamentoTests(GuiTests.AppFixture fx) => _fx = fx;

    [Fact]
    public void CamposDoFiltro_MesmaAlturaEAlinhamento()
    {
        Navegar("NavTransacoes");

        var busca = RetanguloDaJanela("CampoBusca");
        var tipo = RetanguloDaJanela("ComboTipo");
        var categoria = RetanguloDaJanela("ComboCategoria");
        var de = RetanguloDaJanela("DataInicio");
        var ate = RetanguloDaJanela("DataFim");

        foreach (var (nome, ret) in new[] { ("Tipo", tipo), ("Categoria", categoria), ("De", de), ("Até", ate) })
        {
            Assert.True(Math.Abs(ret.Height - busca.Height) <= Tolerancia,
                $"{nome} tem {ret.Height}px de altura; Buscar tem {busca.Height}px");
            Assert.True(Math.Abs(ret.Y - busca.Y) <= Tolerancia,
                $"{nome} começa em Y={ret.Y}; Buscar em Y={busca.Y}");
        }
    }

    [Fact]
    public void BotoesDoDialogoEdicao_NaMesmaLinha()
    {
        Navegar("NavTransacoes");
        CriarTransacaoViaUi();
        var dialogo = AbrirDialogoEdicao();

        var excluir = RetanguloDoDialogo(dialogo, "BotaoExcluir");
        var cancelar = RetanguloDoDialogo(dialogo, null, "Cancelar");
        var salvar = RetanguloDoDialogo(dialogo, null, "Atualizar", "Salvar");

        foreach (var (nome, ret) in new[] { ("Cancelar", cancelar), ("Salvar", salvar) })
        {
            Assert.True(Math.Abs(ret.Height - excluir.Height) <= Tolerancia,
                $"{nome} tem {ret.Height}px de altura; Excluir tem {excluir.Height}px");
            Assert.True(Math.Abs(ret.Y - excluir.Y) <= Tolerancia,
                $"{nome} começa em Y={ret.Y}; Excluir em Y={excluir.Y}");
        }
    }

    // ── apoio ─────────────────────────────────────────────────────────────

    private void Navegar(string navId)
    {
        var botao = Retry.WhileNull(() => _fx.Janela.FindFirstDescendant(cf => cf.ByAutomationId(navId)),
            TimeSpan.FromSeconds(10), ignoreException: true).Result!;

        _fx.Janela.SetForeground();
        Thread.Sleep(300);
        botao.Click();
        Thread.Sleep(700);
    }

    /// <summary>Cria uma transação pela própria UI (o banco do fixture nasce
    /// vazio; semear por segunda conexão SQLite não é visível para a conexão
    /// WAL já aberta pelo aplicativo).</summary>
    private void CriarTransacaoViaUi()
    {
        var botaoNova = Retry.WhileNull(() => _fx.Janela.FindFirstDescendant(cf => cf.ByName("+ Nova transação")),
            TimeSpan.FromSeconds(10), ignoreException: true).Result
            ?? throw new InvalidOperationException("botão '+ Nova transação' não encontrado");
        botaoNova.AsButton().Invoke();

        var dialogo = Retry.WhileNull(() => _fx.Janela.ModalWindows.FirstOrDefault(),
            TimeSpan.FromSeconds(5), ignoreException: true).Result
            ?? throw new InvalidOperationException("diálogo de nova transação não abriu");

        Retry.WhileNull(() => dialogo.FindFirstDescendant(cf => cf.ByAutomationId("CampoValor")),
            TimeSpan.FromSeconds(5), ignoreException: true).Result!
            .AsTextBox().Text = "42,50";

        var categoria = dialogo.FindFirstDescendant(cf => cf.ByAutomationId("ComboCategoria"))!.AsComboBox();
        categoria.Expand();
        categoria.Select(0);

        dialogo.FindFirstDescendant(cf => cf.ByAutomationId("BotaoSalvar"))!
            .AsButton().Invoke();

        Retry.WhileTrue(() => _fx.DialogoAtivo() is not null, TimeSpan.FromSeconds(5));
    }

    private FlaUI.Core.AutomationElements.Window AbrirDialogoEdicao()
    {
        var primeiraLinha = Retry.WhileNull(
                () => _fx.Janela.FindFirstDescendant(cf => cf.ByAutomationId("ListaTransacoes"))
                    .FindAllDescendants(cf => cf.ByControlType(ControlType.ListItem))
                    .FirstOrDefault(),
            TimeSpan.FromSeconds(5), ignoreException: true).Result
            ?? throw new InvalidOperationException("nenhuma transação na lista para editar");

        // duplo clique real abre a edição (MouseDoubleClick da lista)
        var retangulo = primeiraLinha.BoundingRectangle;
        FlaUI.Core.Input.Mouse.MoveTo((int)(retangulo.X + retangulo.Width / 2), (int)(retangulo.Y + retangulo.Height / 2));
        FlaUI.Core.Input.Mouse.DoubleClick(FlaUI.Core.Input.MouseButton.Left);

        return Retry.WhileNull(() => _fx.Janela.ModalWindows.FirstOrDefault(),
            TimeSpan.FromSeconds(5), ignoreException: true).Result
            ?? throw new InvalidOperationException("diálogo de edição não abriu");
    }

    private System.Drawing.Rectangle RetanguloDaJanela(string automationId)
    {
        var elemento = Retry.WhileNull(() => _fx.Janela.FindFirstDescendant(cf => cf.ByAutomationId(automationId)),
            TimeSpan.FromSeconds(10), ignoreException: true).Result
            ?? throw new InvalidOperationException($"'{automationId}' não encontrado");
        return elemento.BoundingRectangle;
    }

    private System.Drawing.Rectangle RetanguloDoDialogo(
        FlaUI.Core.AutomationElements.Window dialogo, string? automationId, params string[] nomes)
    {
        var elemento = Retry.WhileNull(() => automationId is not null
                ? dialogo.FindFirstDescendant(cf => cf.ByAutomationId(automationId))
                : nomes.Select(n => dialogo.FindFirstDescendant(cf => cf.ByName(n)))
                    .FirstOrDefault(e => e is not null),
            TimeSpan.FromSeconds(10), ignoreException: true).Result
            ?? throw new InvalidOperationException($"botão não encontrado no diálogo ({automationId ?? string.Join('/', nomes)})");
        return elemento.BoundingRectangle;
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
