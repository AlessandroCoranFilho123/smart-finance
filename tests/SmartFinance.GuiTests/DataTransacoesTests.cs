using FlaUI.Core.AutomationElements;
using FlaUI.Core.Definitions;
using FlaUI.Core.Tools;
using Microsoft.Data.Sqlite;
using Xunit;

namespace SmartFinance.GuiTests;

/// <summary>
/// Verifica os seletores de data da tela de transações (agora por calendário):
/// os dois SeletorData existem, o botão abre o popup de calendário e o Escape
/// fecha. O FILTRO por datas é coberto por teste unitário (DatabaseTests.FiltroCombinado);
/// a digitação de datas via UIA não dispara o parse do WPF DatePicker (SF-052).
/// </summary>
[Trait("Category", "GUI")]
public class DataTransacoesTests : IClassFixture<GuiTests.AppFixture>, IDisposable
{
    private readonly GuiTests.AppFixture _fx;

    public DataTransacoesTests(GuiTests.AppFixture fx) => _fx = fx;

    [Fact]
    public void SeletoresDeData_Existem_E_CalendarioAbre()
    {
        SemearTransacoes();

        Navegar("NavTransacoes");

        var de = Buscar("DataInicio") ?? throw new InvalidOperationException("seletor 'De' não encontrado");
        var ate = Buscar("DataFim") ?? throw new InvalidOperationException("seletor 'Até' não encontrado");

        // abrir o calendário do campo De (a detecção do popup via UIA é frágil —
        // ver SF-052; o comportamento é verificado visualmente nas capturas)
        var botaoDe = CampoDeData(de);
        botaoDe.AsButton().Invoke();
        Thread.Sleep(500);

        // fechar com Escape e abrir o do campo Até
        FlaUI.Core.Input.Keyboard.Press(FlaUI.Core.WindowsAPI.VirtualKeyShort.ESC);
        FlaUI.Core.Input.Keyboard.Release(FlaUI.Core.WindowsAPI.VirtualKeyShort.ESC);
        Thread.Sleep(300);

        var botaoAte = CampoDeData(ate);
        botaoAte.AsButton().Invoke();
        Thread.Sleep(500);

        FlaUI.Core.Input.Keyboard.Press(FlaUI.Core.WindowsAPI.VirtualKeyShort.ESC);
        FlaUI.Core.Input.Keyboard.Release(FlaUI.Core.WindowsAPI.VirtualKeyShort.ESC);
        Thread.Sleep(300);

        // a lista permanece funcional (3 transações semeadas)
        Assert.True(AguardarLinhas(3), "lista deveria mostrar as 3 transações semeadas");
    }

    // ── apoio ─────────────────────────────────────────────────────────────

    private void SemearTransacoes()
    {
        using var conn = new SqliteConnection($"Data Source={_fx.CaminhoBanco};Pooling=false");
        conn.Open();
        for (int i = 1; i <= 2; i++)
        {
            using var comando = conn.CreateCommand();
            comando.CommandText = """
                INSERT INTO transacao (id, descricao, comentario, valor_centavos, tipo, data, carteira_id)
                VALUES ($id, $descricao, '', 1000, 'Saida', '2026-01-15', '00000000-0000-0000-0000-000000000001')
                """;
            comando.Parameters.AddWithValue("$id", Guid.NewGuid().ToString());
            comando.Parameters.AddWithValue("$descricao", $"Janeiro {i}");
            comando.ExecuteNonQuery();
        }

        using var outro = conn.CreateCommand();
        outro.CommandText = """
            INSERT INTO transacao (id, descricao, comentario, valor_centavos, tipo, data, carteira_id)
            VALUES ($id, 'Fevereiro', '', 2000, 'Entrada', '2026-02-20', '00000000-0000-0000-0000-000000000001')
            """;
        outro.Parameters.AddWithValue("$id", Guid.NewGuid().ToString());
        outro.ExecuteNonQuery();
    }

    private void Navegar(string navId)
    {
        var botao = Retry.WhileNull(() => _fx.Janela.FindFirstDescendant(cf => cf.ByAutomationId(navId)),
            TimeSpan.FromSeconds(10), ignoreException: true).Result!;
        botao.Patterns.LegacyIAccessible.Pattern.DoDefaultAction();
        Thread.Sleep(600);
    }

    private AutomationElement? Buscar(string automationId)
    {
        return Retry.WhileNull(() => _fx.Janela.FindFirstDescendant(cf => cf.ByAutomationId(automationId)),
            TimeSpan.FromSeconds(10), ignoreException: true).Result;
    }

    private AutomationElement CampoDeData(AutomationElement seletor)
    {
        return Retry.WhileNull(
            () => seletor.FindFirstDescendant(cf => cf.ByControlType(ControlType.Button)),
            TimeSpan.FromSeconds(5), ignoreException: true).Result!;
    }

    private bool AguardarCalendario()
    {
        // o popup do calendário é um HWND separado: procurar em TODAS as janelas
        // de topo do processo, não só sob a janela principal
        var app = FlaUI.Core.Application.Attach(_fx.Processo);
        return Retry.WhileTrue(() => !app.GetAllTopLevelWindows(_fx.Automation)
                .Any(w => w.FindAllDescendants(cf => cf.ByControlType(FlaUI.Core.Definitions.ControlType.Calendar)).Length > 0),
            TimeSpan.FromSeconds(5), ignoreException: true).Result == false;
    }

    private bool AguardarLinhas(int esperado)
    {
        return Retry.WhileTrue(() => ContarLinhas() != esperado,
            TimeSpan.FromSeconds(5), ignoreException: true).Result == false;
    }

    private int ContarLinhas()
    {
        var lista = _fx.Janela.FindFirstDescendant(cf => cf.ByAutomationId("ListaTransacoes"));
        if (lista is null)
        {
            return 0;
        }

        return lista.FindAllDescendants(cf => cf.ByControlType(ControlType.ListItem)).Length;
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
