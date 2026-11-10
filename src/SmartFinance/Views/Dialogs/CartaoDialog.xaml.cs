using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Models;
using SmartFinance.Services;

namespace SmartFinance.Views.Dialogs;

public partial class CartaoDialog : Window
{
    private static readonly Regex PadraoValor = new("^[0-9.,]*$", RegexOptions.Compiled);

    private readonly Servicos _servicos = App.Servicos!;
    private CompraCartaoCredito? _emEdicao;

    public CartaoDialog()
    {
        InitializeComponent();
        JanelaNativa.AoFonteInicializada(this);
    }

    public void Configurar(CompraCartaoCredito? compra)
    {
        _emEdicao = compra;

        if (compra is null)
        {
            CampoVencimento.SelectedDate = DateTime.Today.AddMonths(1);
            return;
        }

        TxtTitulo.Text = "Detalhes da compra";
        Title = "Detalhes da compra";
        CampoNome.Text = compra.Nome;
        CampoValor.Text = (compra.ValorTotalCentavos / 100m).ToString("N2", Formatacao.CulturaBr);
        CampoParcelas.Text = compra.Parcelas.ToString();
        CampoVencimento.SelectedDate = compra.Vencimento.ToDateTime(TimeOnly.MinValue);
        BotaoSalvar.Content = "Atualizar";
        BotaoExcluir.Visibility = Visibility.Visible;
    }

    private void ValidarDigitoValor(object sender, TextCompositionEventArgs e)
    {
        e.Handled = !PadraoValor.IsMatch(e.Text);
    }

    private void Salvar(object sender, RoutedEventArgs e)
    {
        MostrarErro(null);
        try
        {
            if (CampoVencimento.SelectedDate is null)
            {
                MostrarErro("Selecione a data de vencimento");
                return;
            }

            if (!int.TryParse(CampoParcelas.Text.Trim(), out int parcelas))
            {
                MostrarErro("Informe o número de parcelas");
                return;
            }

            _servicos.Cartao.Salvar(
                _emEdicao?.Id,
                CampoNome.Text,
                ValorMonetario.ParseCentavos(CampoValor.Text),
                parcelas,
                DateOnly.FromDateTime(CampoVencimento.SelectedDate.Value));

            DialogResult = true;
        }
        catch (ArgumentException ex)
        {
            MostrarErro(ex.Message);
        }
    }

    private void Excluir(object sender, RoutedEventArgs e)
    {
        if (_emEdicao is null)
        {
            return;
        }

        var resposta = MessageBox.Show(this, "Tem certeza que deseja excluir esta compra?",
            "Excluir compra", MessageBoxButton.YesNo, MessageBoxImage.Question);

        if (resposta != MessageBoxResult.Yes)
        {
            return;
        }

        _servicos.Cartao.Deletar(_emEdicao.Id);
        DialogResult = true;
    }

    private void Cancelar(object sender, RoutedEventArgs e) => DialogResult = false;

    private void MostrarErro(string? mensagem)
    {
        if (mensagem is null)
        {
            TxtErro.Visibility = Visibility.Collapsed;
            TxtErro.Text = "";
        }
        else
        {
            TxtErro.Text = mensagem;
            TxtErro.Visibility = Visibility.Visible;
        }
    }
}
