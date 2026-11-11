using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Data;
using SmartFinance.Services;

namespace SmartFinance.Views.Dialogs;

public partial class CarteirasDialog : Window
{
    private readonly Servicos _servicos = App.Servicos!;

    public CarteirasDialog()
    {
        InitializeComponent();
        JanelaNativa.AoFonteInicializada(this);
        CarregarValores();
    }

    private void CarregarValores()
    {
        var banco = _servicos.Carteiras.BuscarPorId(Database.CarteiraBancoId);
        var dinheiro = _servicos.Carteiras.BuscarPorId(Database.CarteiraDinheiroFisicoId);
        CampoBanco.Text = ((banco?.SaldoInicialCentavos ?? 0) / 100m).ToString("N2", Formatacao.CulturaBr);
        CampoDinheiro.Text = ((dinheiro?.SaldoInicialCentavos ?? 0) / 100m).ToString("N2", Formatacao.CulturaBr);
    }

    private void ValidarDigitoValor(object sender, TextCompositionEventArgs e) => MascaraValor.ValidarDigitos(sender, e);

    private void FormatarAoSair(object sender, RoutedEventArgs e) => MascaraValor.FormatarAoSair((System.Windows.Controls.TextBox)sender);

    private void Salvar(object sender, RoutedEventArgs e)
    {
        MostrarErro(null);
        try
        {
            // Uma única transação: se a segunda carteira falhar, a primeira não fica aplicada.
            _servicos.Db.EmTransacao(() =>
            {
                _servicos.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, ValorMonetario.ParseCentavos(CampoBanco.Text));
                _servicos.Carteiras.AtualizarSaldoInicial(Database.CarteiraDinheiroFisicoId, ValorMonetario.ParseCentavos(CampoDinheiro.Text));
            });
            DialogResult = true;
        }
        catch (ArgumentException ex)
        {
            MostrarErro(ex.Message);
        }
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
