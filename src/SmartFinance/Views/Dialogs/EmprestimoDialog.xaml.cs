using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Data;
using SmartFinance.Models;
using SmartFinance.Services;

namespace SmartFinance.Views.Dialogs;

public partial class EmprestimoDialog : Window
{
    private readonly Servicos _servicos = App.Servicos!;
    private Emprestimo? _emEdicao;

    public EmprestimoDialog()
    {
        InitializeComponent();
        JanelaNativa.AoFonteInicializada(this);
        CampoData.DataSelecionada = DateTime.Today;
    }

    public void Configurar(Emprestimo? emprestimo)
    {
        _emEdicao = emprestimo;

        ComboCarteira.ItemsSource = _servicos.Carteiras.ListarCarteiras();

        if (emprestimo is null)
        {
            BotaoEmprestei.IsChecked = true;
            ComboCarteira.SelectedItem = ((List<Carteira>)ComboCarteira.ItemsSource).FirstOrDefault();
            return;
        }

        TxtTitulo.Text = "Detalhes do empréstimo";
        Title = "Detalhes do empréstimo";
        CampoNome.Text = emprestimo.Nome;
        CampoValor.Text = (emprestimo.ValorCentavos / 100m).ToString("N2", Formatacao.CulturaBr);
        CampoData.DataSelecionada = emprestimo.DataPagamento.ToDateTime(TimeOnly.MinValue);
        ComboCarteira.SelectedItem = ((List<Carteira>)ComboCarteira.ItemsSource)
            .FirstOrDefault(c => c.Id == (emprestimo.CarteiraId ?? Database.CarteiraBancoId));
        BotaoSalvar.Content = "Atualizar";
        BotaoExcluir.Visibility = Visibility.Visible;
        BotaoEmprestei.IsChecked = emprestimo.Tipo == TipoEmprestimo.Emprestei;
        BotaoPedi.IsChecked = emprestimo.Tipo == TipoEmprestimo.PediEmprestado;
    }

    private TipoEmprestimo TipoAtual => BotaoEmprestei.IsChecked == true
        ? TipoEmprestimo.Emprestei
        : TipoEmprestimo.PediEmprestado;

    private void AoMudarTipo(object sender, RoutedEventArgs e)
    {
        // Garante sempre exatamente um tipo marcado (SF-003).
        if (BotaoEmprestei.IsChecked != true && BotaoPedi.IsChecked != true)
        {
            (sender == BotaoEmprestei ? BotaoEmprestei : BotaoPedi).IsChecked = true;
            return;
        }

        if (BotaoEmprestei.IsChecked == true && BotaoPedi.IsChecked == true)
        {
            if (sender == BotaoEmprestei)
            {
                BotaoPedi.IsChecked = false;
            }
            else
            {
                BotaoEmprestei.IsChecked = false;
            }
        }
    }

    private void ValidarDigitoValor(object sender, TextCompositionEventArgs e) => MascaraValor.ValidarDigitos(sender, e);

    private void FormatarAoSair(object sender, RoutedEventArgs e) => MascaraValor.FormatarAoSair((System.Windows.Controls.TextBox)sender);

    private void Salvar(object sender, RoutedEventArgs e)
    {
        MostrarErro(null);
        try
        {
            if (CampoData.DataSelecionada is null)
            {
                MostrarErro("Selecione a data de pagamento");
                return;
            }

            var carteira = (Carteira?)ComboCarteira.SelectedItem;
            if (carteira is null)
            {
                MostrarErro("Selecione a carteira da movimentação");
                return;
            }

            _servicos.Emprestimos.Salvar(
                _emEdicao?.Id,
                TipoAtual,
                CampoNome.Text,
                ValorMonetario.ParseCentavos(CampoValor.Text),
                DateOnly.FromDateTime(CampoData.DataSelecionada.Value),
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

        var resposta = MessageBox.Show(this, "Tem certeza que deseja excluir este empréstimo?",
            "Excluir empréstimo", MessageBoxButton.YesNo, MessageBoxImage.Question);

        if (resposta != MessageBoxResult.Yes)
        {
            return;
        }

        _servicos.Emprestimos.Deletar(_emEdicao.Id);
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
