using FlaUI.Core.AutomationElements;
using FlaUI.Core.Definitions;
using FlaUI.Core.Tools;
using Microsoft.Data.Sqlite;
using Xunit;

namespace SmartFinance.GuiTests;

/// <summary>
/// Verifica os campos de data da tela de transações (pergunta: "faltou campo de
/// data?"): os dois DatePickers existem, aceitam digitação e o filtro aplica de fato.
/// Dados de exemplo são inseridos direto no banco isolado do fixture.
/// </summary>
[Trait("Category", "GUI")]
public class DataTransacoesTests : IClassFixture<GuiTests.AppFixture>, IDisposable
{
    private readonly GuiTests.AppFixture _fx;

    public DataTransacoesTests(GuiTests.AppFixture fx) => _fx = fx;

    [Fact]
    public void CamposDeData_ExistemEFiltram()
    {
        SemearTransacoesDeJaneiro();

        Navegar("NavTransacoes");

        // 1) os dois DatePickers existem na tela
        var de = Buscar("DataInicio") ?? throw new InvalidOperationException("campo 'De' não encontrado");
        var ate = Buscar("DataFim") ?? throw new InvalidOperationException("campo 'Até' não encontrado");

        // 2) sem filtro: as 3 transações semeadas aparecem
        Assert.Equal(3, ContarLinhas());

        // 3) filtro janeiro/2026: só as duas de janeiro
        PreencherData(de, "01/01/2026");
        PreencherData(ate, "31/01/2026");
        Thread.Sleep(600); // SelectedDateChanged aplica

        Assert.Equal(2, ContarLinhas());

        // 4) intervalo sem transações: zero
        PreencherData(de, "01/05/2026");
        PreencherData(ate, "31/05/2026");
        Thread.Sleep(600);
        Assert.Equal(0, ContarLinhas());

        // 5) limpar os campos restaura tudo
        Limpar(de);
        Limpar(ate);
        Thread.Sleep(600);
        Assert.Equal(3, ContarLinhas());
    }

    // ── apoio ─────────────────────────────────────────────────────────────

    private void SemearTransacoesDeJaneiro()
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

    private void PreencherData(AutomationElement datePicker, string data)
    {
        var campo = Retry.WhileNull(
            () => datePicker.FindFirstDescendant(cf => cf.ByControlType(ControlType.Edit)),
            TimeSpan.FromSeconds(5), ignoreException: true).Result!;

        campo.Patterns.Value.Pattern.SetValue(data);
        campo.Focus();
        TecladoEnter();
    }

    private void Limpar(AutomationElement datePicker)
    {
        var campo = Retry.WhileNull(
            () => datePicker.FindFirstDescendant(cf => cf.ByControlType(ControlType.Edit)),
            TimeSpan.FromSeconds(5), ignoreException: true).Result!;

        campo.Patterns.Value.Pattern.SetValue(string.Empty);
        campo.Focus();
        TecladoEnter();
    }

    private static void TecladoEnter()
    {
        FlaUI.Core.Input.Keyboard.Press(FlaUI.Core.WindowsAPI.VirtualKeyShort.ENTER);
        FlaUI.Core.Input.Keyboard.Release(FlaUI.Core.WindowsAPI.VirtualKeyShort.ENTER);
        Thread.Sleep(250);
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
