using FlaUI.Core.AutomationElements;
using FlaUI.Core.Conditions;
using FlaUI.Core.Definitions;
using FlaUI.Core.Tools;
using FlaUI.UIA3;
using Xunit;

namespace SmartFinance.GuiTests;

/// <summary>
/// F4: simulação de leitor de tela — varre a árvore UIA procurando controles
/// interativos sem nome acessível nem AutomationId (o que um leitor de tela
/// não conseguiria anunciar). Partes internas de DatePicker rotulado são
/// excluídas: o anúncio é responsabilidade do DatePicker com AutomationProperties.Name.
/// </summary>
[Trait("Category", "GUI")]
public class AcessibilidadeTests : IClassFixture<GuiTests.AppFixture>, IDisposable
{
    private static readonly ConditionFactory Cf = new(new UIA3PropertyLibrary());
    private readonly GuiTests.AppFixture _fx;

    public AcessibilidadeTests(GuiTests.AppFixture fx) => _fx = fx;

    [Fact]
    public void JanelaPrincipal_ControlesInterativosTemNome()
    {
        var semIdentificacao = ColetarSemNome(_fx.Janela);

        Assert.True(semIdentificacao.Count == 0,
            "controles interativos sem nome acessível: " + string.Join(", ", semIdentificacao));
    }

    [Fact]
    public void DialogoDeTransacao_ControlesInterativosTemNome()
    {
        var botao = Retry.WhileNull(
            () => _fx.Janela.FindFirstDescendant(cf => cf.ByName("+ Nova transação")),
            TimeSpan.FromSeconds(10), ignoreException: true).Result!;
        botao.Patterns.Invoke.Pattern.Invoke();
        var dialogo = Retry.WhileNull(() => _fx.Janela.ModalWindows.FirstOrDefault(),
            TimeSpan.FromSeconds(5), ignoreException: true).Result!;

        var semIdentificacao = ColetarSemNome(dialogo);

        Assert.True(semIdentificacao.Count == 0,
            "controles interativos sem nome acessível: " + string.Join(", ", semIdentificacao));
        dialogo.Close();
    }

    private static List<string> ColetarSemNome(AutomationElement raiz)
    {
        var problemas = new List<string>();
        foreach (var elemento in raiz.FindAllDescendants())
        {
            var tipo = elemento.ControlType;
            bool interativo = tipo is ControlType.Button or ControlType.ComboBox
                or ControlType.Edit or ControlType.RadioButton
                or ControlType.CheckBox or ControlType.ListItem;
            if (!interativo)
            {
                continue;
            }

            var nome = elemento.Properties.Name.ValueOrDefault;
            if (!string.IsNullOrWhiteSpace(nome))
            {
                continue;
            }

            // partes internas de template (PART_*), como o calendário do DatePicker,
            // herdam o contexto do controle rotulado que as contém
            var id = elemento.Properties.AutomationId.ValueOrDefault;
            if (id is not null && id.StartsWith("PART_", StringComparison.Ordinal))
            {
                continue;
            }

            if (string.IsNullOrWhiteSpace(id))
            {
                problemas.Add($"{tipo}:sem-nome");
            }
        }

        return problemas;
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
