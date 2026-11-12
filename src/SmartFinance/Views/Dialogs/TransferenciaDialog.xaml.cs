using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Data;
using SmartFinance.Models;
using SmartFinance.Services;

namespace SmartFinance.Views.Dialogs;

public partial class TransferenciaDialog : Window
{
    private readonly Servicos _servicos = App.Servicos!;

    public TransferenciaDialog()
    {
        InitializeComponent();
        JanelaNativa.AoFonteInicializada(this);

        ComboOrigem.ItemsSource = _servicos.Carteiras.ListarCarteiras();
        ComboDestino.ItemsSource = _servicos.Carteiras.ListarCarteiras();
        ComboOrigem.SelectedItem = ((List<Carteira>)ComboOrigem.ItemsSource).FirstOrDefault(c => c.Id == Database.CarteiraBancoId);
        ComboDestino.SelectedItem = ((List<Carteira>)ComboDestino.ItemsSource).FirstOrDefault(c => c.Id == Database.CarteiraDinheiroFisicoId);
        CampoData.DataSelecionada = DateTime.Today;
    }

    private void ValidarDigitoValor(object sender, TextCompositionEventArgs e) => MascaraValor.ValidarDigitos(sender, e);

    private void FormatarAoSair(object sender, RoutedEventArgs e) => MascaraValor.FormatarAoSair((System.Windows.Controls.TextBox)sender);

    private void Salvar(object sender, RoutedEventArgs e)
    {
        MostrarErro(null);
        try
        {
            var origem = (Carteira?)ComboOrigem.SelectedItem;
            var destino = (Carteira?)ComboDestino.SelectedItem;

            if (origem is null || destino is null)
            {
                MostrarErro("Selecione as carteiras de origem e destino");
                return;
            }

            if (CampoData.DataSelecionada is null)
            {
                MostrarErro("Selecione uma data");
                return;
            }

            _servicos.Carteiras.Transferir(
                origem.Id,
                destino.Id,
                ValorMonetario.ParseCentavos(CampoValor.Text),
                DateOnly.FromDateTime(CampoData.DataSelecionada.Value),
                CampoComentario.Text);

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
