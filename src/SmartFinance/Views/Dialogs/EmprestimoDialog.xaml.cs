using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Models;
using SmartFinance.Services;

namespace SmartFinance.Views.Dialogs;

public partial class EmprestimoDialog : Window
{
    private static readonly Regex PadraoValor = new("^[0-9.,]*$", RegexOptions.Compiled);

    private readonly Servicos _servicos = App.Servicos!;
    private Emprestimo? _emEdicao;

    public EmprestimoDialog()
    {
        InitializeComponent();
        JanelaNativa.AoFonteInicializada(this);
        CampoData.SelectedDate = DateTime.Today;
    }

    public void Configurar(Emprestimo? emprestimo)
    {
        _emEdicao = emprestimo;

        if (emprestimo is null)
        {
            BotaoEmprestei.IsChecked = true;
            return;
        }

        TxtTitulo.Text = "Detalhes do empréstimo";
        Title = "Detalhes do empréstimo";
        CampoNome.Text = emprestimo.Nome;
        CampoValor.Text = (emprestimo.ValorCentavos / 100m).ToString("N2", Formatacao.CulturaBr);
        CampoData.SelectedDate = emprestimo.DataPagamento.ToDateTime(TimeOnly.MinValue);
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
        if (sender == BotaoEmprestei && BotaoEmprestei.IsChecked == true)
        {
            BotaoPedi.IsChecked = false;
        }
        else if (sender == BotaoPedi && BotaoPedi.IsChecked == true)
        {
            BotaoEmprestei.IsChecked = false;
        }
        else if (BotaoEmprestei.IsChecked != true && BotaoPedi.IsChecked != true)
        {
            (sender == BotaoEmprestei ? BotaoEmprestei : BotaoPedi).IsChecked = true;
        }
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
            if (CampoData.SelectedDate is null)
            {
                MostrarErro("Selecione a data de pagamento");
                return;
            }

            _servicos.Emprestimos.Salvar(
                _emEdicao?.Id,
                TipoAtual,
                CampoNome.Text,
                ValorMonetario.ParseCentavos(CampoValor.Text),
                DateOnly.FromDateTime(CampoData.SelectedDate.Value));

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
