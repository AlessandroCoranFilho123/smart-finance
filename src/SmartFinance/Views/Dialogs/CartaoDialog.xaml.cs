using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Data;
using SmartFinance.Models;
using SmartFinance.Services;

namespace SmartFinance.Views.Dialogs;

public partial class CartaoDialog : Window
{
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

        ComboCarteira.ItemsSource = _servicos.Carteiras.ListarCarteiras();

        if (compra is null)
        {
            CampoVencimento.DataSelecionada = DateTime.Today.AddMonths(1);
            CampoPagas.Text = "0";
            ComboCarteira.SelectedItem = ((List<Carteira>)ComboCarteira.ItemsSource).FirstOrDefault();
            return;
        }

        TxtTitulo.Text = "Detalhes da compra";
        Title = "Detalhes da compra";
        CampoNome.Text = compra.Nome;
        CampoValor.Text = (compra.ValorTotalCentavos / 100m).ToString("N2", Formatacao.CulturaBr);
        CampoParcelas.Text = compra.Parcelas.ToString();
        CampoPagas.Text = compra.ParcelasPagas.ToString();
        CampoVencimento.DataSelecionada = compra.Vencimento.ToDateTime(TimeOnly.MinValue);
        ComboCarteira.SelectedItem = ((List<Carteira>)ComboCarteira.ItemsSource)
            .FirstOrDefault(c => c.Id == (compra.CarteiraId ?? Database.CarteiraBancoId));
        BotaoSalvar.Content = "Atualizar";
        BotaoExcluir.Visibility = Visibility.Visible;
    }

    private void ValidarDigitoValor(object sender, TextCompositionEventArgs e) => MascaraValor.ValidarDigitos(sender, e);

    private void FormatarAoSair(object sender, RoutedEventArgs e) => MascaraValor.FormatarAoSair((System.Windows.Controls.TextBox)sender);

    private void Salvar(object sender, RoutedEventArgs e)
    {
        MostrarErro(null);
        try
        {
            if (CampoVencimento.DataSelecionada is null)
            {
                MostrarErro("Selecione a data de vencimento");
                return;
            }

            if (!int.TryParse(CampoParcelas.Text.Trim(), out int parcelas))
            {
                MostrarErro("Informe o número de parcelas");
                return;
            }

            if (!int.TryParse(CampoPagas.Text.Trim(), out int pagas))
            {
                MostrarErro("Informe quantas parcelas já foram pagas (0 se nenhuma)");
                return;
            }

            var carteira = (Carteira?)ComboCarteira.SelectedItem;
            if (carteira is null)
            {
                MostrarErro("Selecione a carteira de onde sai o valor");
                return;
            }

            _servicos.Cartao.Salvar(
                _emEdicao?.Id,
                CampoNome.Text,
                ValorMonetario.ParseCentavos(CampoValor.Text),
                parcelas,
                pagas,
                DateOnly.FromDateTime(CampoVencimento.DataSelecionada.Value),
                carteira.Id);

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
