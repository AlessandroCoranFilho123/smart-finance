using System.Text.RegularExpressions;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Services;

namespace SmartFinance.Views;

/// <summary>Máscara e formatação compartilhadas dos campos de valor (SF-013/D-03).</summary>
public static partial class MascaraValor
{
    [GeneratedRegex("^[0-9.,]*$")]
    private static partial Regex PadraoDigitos();

    public static void ValidarDigitos(object sender, TextCompositionEventArgs e)
    {
        e.Handled = !PadraoDigitos().IsMatch(e.Text);
    }

    /// <summary>Reformata o campo para o padrão pt-BR ao perder o foco; texto
    /// inválido é mantido para o usuário corrigir (o salvar recusa).</summary>
    public static void FormatarAoSair(TextBox campo)
    {
        if (campo.Text.Length == 0)
        {
            return;
        }

        try
        {
            long centavos = ValorMonetario.ParseCentavos(campo.Text);
            campo.Text = (centavos / 100m).ToString("N2", Formatacao.CulturaBr);
        }
        catch (ArgumentException)
        {
            // deixa o usuário corrigir o texto inválido
        }
    }
}
