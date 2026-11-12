using FlaUI.Core.AutomationElements;
using FlaUI.Core.Definitions;
using FlaUI.Core.Tools;
using Microsoft.Data.Sqlite;
using Xunit;

namespace SmartFinance.GuiTests;

/// <summary>
/// Verifica os campos de data da tela de transações (dúvida do usuário: "faltou
/// campo de data?"): os dois DatePickers existem, aceitam entrada via UIA e a
/// lista responde. O FILTRO por datas é coberto por testes unitários
/// (DatabaseTests.FiltroCombinado) — a entrada de data via UIA não dispara o
/// parse do WPF DatePicker (limitação documentada em SF-052), então este teste
/// não afirma filtragem por datas digitadas.
/// </summary>
[Trait("Category", "GUI")]
public class DataTransacoesTests : IClassFixture<GuiTests.AppFixture>, IDisposable
{
    private readonly GuiTests.AppFixture _fx;

    public DataTransacoesTests(GuiTests.AppFixture fx) => _fx = fx;

    [Fact]
    public void CamposDeData_ExistemEAceitamEntrada()
    {
        SemearTransacoes();

        Navegar("NavTransacoes");

        var de = Buscar("DataInicio") ?? throw new InvalidOperationException("campo 'De' não encontrado");
        var ate = Buscar("DataFim") ?? throw new InvalidOperationException("campo 'Até' não encontrado");

        // ambos os campos aceitam entrada de texto e o filtro aplica de fato
        // (datas não-ambíguas nas duas culturas — SF-052)
        var campoDe = CampoDeData(de);
        campoDe.Focus();
        campoDe.Patterns.Value.Pattern.SetValue("01/01/2026");
        Assert.Equal("01/01/2026", campoDe.Patterns.Value.Pattern.Value);

        var campoAte = CampoDeData(ate);
        campoAte.Focus();
        campoAte.Patterns.Value.Pattern.SetValue("01/02/2026");
        Assert.Equal("01/02/2026", campoAte.Patterns.Value.Pattern.Value);

        // janeiro: 2 das 3 transações semeadas
        Assert.True(AguardarLinhas(2), "filtro de janeiro não aplicou");
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

    private AutomationElement CampoDeData(AutomationElement datePicker)
    {
        return Retry.WhileNull(
            () => datePicker.FindFirstDescendant(cf => cf.ByControlType(ControlType.Edit)),
            TimeSpan.FromSeconds(5), ignoreException: true).Result!;
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
