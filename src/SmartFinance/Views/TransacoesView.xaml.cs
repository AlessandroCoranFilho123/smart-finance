using System.ComponentModel;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using Microsoft.Win32;
using System.IO;
using SmartFinance.Data;
using SmartFinance.Models;
using SmartFinance.Services;
using SmartFinance.Views.Dialogs;

namespace SmartFinance.Views;

public partial class TransacoesView : UserControl, IViewAtualizavel
{
    private readonly Servicos _servicos = App.Servicos!;
    private List<Models.Transacao> _resultado = new();

    public TransacoesView()
    {
        InitializeComponent();

        // "Todas" + categorias; não declarar itens no XAML (coleção populada conflita
        // com ItemsSource — InvalidOperationException).
        var itens = new List<ItemCombo> { new("Todas", null) };
        itens.AddRange(Enum.GetValues<Models.Categoria>()
            .Select(c => new ItemCombo(c.NomeExibicao(), c)));
        ComboCategoria.ItemsSource = itens;
        ComboCategoria.SelectedIndex = 0;

        Atualizar();
    }

    public void Atualizar() => AplicarFiltros();

    private sealed record ItemCombo(string Rotulo, Models.Categoria? Valor)
    {
        public override string ToString() => Rotulo;
    }

    private void AoMudarFiltro(object sender, RoutedEventArgs e) => AplicarFiltros();

    private Models.TipoTransacao? TipoSelecionado => (ComboTipo.SelectedItem as ComboBoxItem)?.Content switch
    {
        "Entrada" => Models.TipoTransacao.Entrada,
        "Saída" => Models.TipoTransacao.Saida,
        _ => null
    };

    private Models.Categoria? CategoriaSelecionada =>
        (ComboCategoria.SelectedItem as ItemCombo)?.Valor;

    private void AplicarFiltros()
    {
        if (ListaTransacoes is null)
        {
            return;
        }

        var filtro = new TransacaoFiltro(
            CampoBusca.Text,
            TipoSelecionado,
            CategoriaSelecionada,
            DataInicio.SelectedDate is null ? null : DateOnly.FromDateTime(DataInicio.SelectedDate.Value),
            DataFim.SelectedDate is null ? null : DateOnly.FromDateTime(DataFim.SelectedDate.Value));

        _resultado = _servicos.Transacoes.ListarPorFiltro(filtro);

        var nomesCarteiras = _servicos.Carteiras.ListarCarteiras()
            .ToDictionary(c => c.Id, c => c.Nome);

        ListaTransacoes.ItemsSource = _resultado
            .Select(t => new LinhaTransacao(t, nomesCarteiras.GetValueOrDefault(t.CarteiraId ?? Database.CarteiraBancoId, "")))
            .ToList();

        long entradas = _resultado.Where(t => t.Tipo == Models.TipoTransacao.Entrada).Sum(t => t.ValorCentavos);
        long saidas = _resultado.Where(t => t.Tipo == Models.TipoTransacao.Saida).Sum(t => t.ValorCentavos);
        TxtEntradas.Text = Formatacao.Moeda(entradas);
        TxtSaidas.Text = Formatacao.Moeda(saidas);
        TxtSaldoFiltro.Text = Formatacao.Moeda(entradas - saidas);
        TxtTotal.Text = $"Total: {_resultado.Count} transação(ões)";
    }

    private void LimparFiltros(object sender, RoutedEventArgs e)
    {
        CampoBusca.Text = "";
        ComboTipo.SelectedIndex = 0;
        ComboCategoria.SelectedIndex = 0;
        DataInicio.SelectedDate = null;
        DataFim.SelectedDate = null;
        AplicarFiltros();
    }

    private void NovaTransacao(object sender, RoutedEventArgs e) => AbrirDialogo(null);

    private void EditarTransacao(object sender, MouseButtonEventArgs e)
    {
        if (ListaTransacoes.SelectedItem is LinhaTransacao linha)
        {
            AbrirDialogo(linha.Transacao);
        }
    }

    private void AbrirDialogo(Models.Transacao? transacao)
    {
        var dialogo = new TransacaoDialog { Owner = Window.GetWindow(this) };
        dialogo.Configurar(transacao);
        if (dialogo.ShowDialog() == true)
        {
            AplicarFiltros();
        }
    }

    private void ExportarCsv(object sender, RoutedEventArgs e)
    {
        var dialogo = new SaveFileDialog
        {
            Title = "Exportar transações (.csv)",
            Filter = "Arquivo CSV (*.csv)|*.csv",
            FileName = $"transacoes-{DateTime.Today:yyyy-MM-dd}.csv"
        };

        if (dialogo.ShowDialog() != true)
        {
            return;
        }

        try
        {
            TransacaoCsvExporter.Exportar(dialogo.FileName, _resultado);
            MessageBox.Show("Transações exportadas com sucesso.", "Exportação",
                MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (IOException ex)
        {
            MessageBox.Show($"Não foi possível salvar o arquivo:\n{ex.Message}", "Exportação",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    private void ExportarTxt(object sender, RoutedEventArgs e)
    {
        var dialogo = new SaveFileDialog
        {
            Title = "Exportar transações (.txt)",
            Filter = "Arquivo de texto (*.txt)|*.txt",
            FileName = $"transacoes-{DateTime.Today:yyyy-MM-dd}.txt"
        };

        if (dialogo.ShowDialog() != true)
        {
            return;
        }

        try
        {
            TransacaoTxtExporter.Exportar(dialogo.FileName, _resultado);
            MessageBox.Show("Transações exportadas com sucesso.", "Exportação",
                MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (IOException ex)
        {
            MessageBox.Show($"Não foi possível salvar o arquivo:\n{ex.Message}", "Exportação",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }
}
